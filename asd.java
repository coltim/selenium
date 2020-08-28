package hu.dolphio.jcmf.modules.procurement.downloader.gr.ippokratio;

import hu.dolphio.jcmf.modules.procurement.common.jpa.SrcGrIppokratioProcurement;
import hu.dolphio.jcmf.modules.procurement.common.type.ProcurementProcessedType;
import hu.dolphio.jcmf.modules.tasks.ModuleTaskException;
import hu.dolphio.utils.DownloaderUtils;
import hu.dolphio.utils.PdfContentReader;
import hu.dolphio.utils.WebDriverType;
import hu.dolphio.utils.WordContentReader;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Log4j
public class DigestProcessor {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);

    private final String article_xpath;
    private final String PROCUREMENTS_URL;
    private final String xpath_to_get_links_on_page;
    private final String next_button_linktext;
    private final String constans_string_in_attachments;
    private final String announcement_type;
    private final String announcement_category;
    private final String proposer;
    private final String nuts_codes;

    private final WebDriver driver;
    private final PdfContentReader pdfContentReader;
    private final WordContentReader wordContentReader;
    private SrcGrIppokratioProcurement newProcurement;

    public DigestProcessor(PropertiesConfiguration config) {
        PROCUREMENTS_URL = config.getString("ippokratio.procurement.main.url");
        xpath_to_get_links_on_page = config.getString("ippokratio.xpath.to.get.links.on.page");
        next_button_linktext = config.getString("ippokratio.next.button.linktext");
        constans_string_in_attachments = config.getString("ippokratio.contans.string.in.attachments");
        article_xpath = config.getString("ippokratio.xpath.article");

        announcement_type = config.getString("ippokratio.announcement.type");
        announcement_category = config.getString("ippokratio.announcement.category");
        proposer = config.getString("ippokratio.proposer");
        nuts_codes = config.getString("ippokratio.nuts.codes");
        pdfContentReader = new PdfContentReader();
        wordContentReader = new WordContentReader();
        //        driver = DownloaderUtils.getWebdriver(config, "ippokratio", true);
        driver = WebDriverType.PHANTOMJS.create();
    }

    public Map<String, String> getLinks() throws ModuleTaskException {
        log.debug("load page : " + PROCUREMENTS_URL);
        driver.get(PROCUREMENTS_URL);
        Map<String, String> urls = new HashMap<>();
        do {
            List<WebElement> anchorList = driver.findElements(By.xpath(xpath_to_get_links_on_page));
            if (CollectionUtils.isEmpty(anchorList)) {
                throw new ModuleTaskException("Found 0 procurement, check the site: " + PROCUREMENTS_URL);
            }
            for (WebElement anchor : anchorList) {
                try {
                    String href = StringUtils.trim(anchor.getAttribute("href"));
                    if (StringUtils.isNotEmpty(href)) {
                        urls.put(href, driver.getCurrentUrl());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get href from" + driver.getCurrentUrl(), e);
                }
            }
            if (MapUtils.isEmpty(urls)) {
                throw new ModuleTaskException("Found 0 procurement, check the site: " + PROCUREMENTS_URL);
            }
        } while (clickNext());

        return urls;
    }

    /**
     * @return true if success or false if not
     */
    private boolean clickNext() {
        boolean nextSuccess = false;
        List<WebElement> nextButton = driver.findElements(By.partialLinkText(next_button_linktext));
        if (CollectionUtils.isNotEmpty(nextButton)) {
            log.debug("Click next : " + nextButton.size());
            nextButton.get(0).click();
            nextSuccess = true;
        }
        return nextSuccess;
    }

    private String getInnerID(String url) {
        String result = null;
        String[] urlPieces = url.split("/?p=");
        if (urlPieces.length > 1) {
            result = urlPieces[1];
        }
        return result;
    }

    public SrcGrIppokratioProcurement processProcurements(String url, String pageURL) {
        driver.get(pageURL);
        newProcurement = new SrcGrIppokratioProcurement();

        String innerID = getInnerID(url);
        log.debug(String.format("procurement innerID: %s URL: %s", innerID, pageURL));

        newProcurement.setProcessed(ProcurementProcessedType.UNPROCESSED.getType());
        newProcurement.setCountry("GR");
        newProcurement.setDownloadDate(new Date());
        newProcurement.setUrl(url);
        newProcurement.setAnnouncementType(announcement_type);
        newProcurement.setAnnouncementCategory(announcement_category);
        newProcurement.setProposer(proposer);
        newProcurement.setNuts(nuts_codes);
        newProcurement.setPublishedDate(getPublishDate(innerID));
        newProcurement.setProcurementSubject(getProcurementSubject(innerID));
        newProcurement.setBriefContent(getBriefContent(innerID));
        newProcurement.setContent(getContent(innerID));
        newProcurement.setInnerId(innerID);

        return newProcurement;
    }

    private Date getPublishDate(String innerID) {
        Date result = null;
        boolean trueorfalse = isAble(buildXPathForPublishDate(innerID));
        if (Objects.nonNull(innerID) && trueorfalse) {
            try {
                String dateText = getTextFromDriver(buildXPathForPublishDate(innerID));
                if (Objects.nonNull(dateText)) {
                    result = convertToDate(dateText);
                }
            } catch (Exception ex) {
                log.error("The publish date cant be found! ", ex);
            }
        }
        return result;
    }

    private Date convertToDate(String dateString) {
        Date date = null;
        if (StringUtils.isNotEmpty(dateString)) {
            try {
                date = simpleDateFormat.parse(dateString);
            } catch (ParseException ex) {
                log.error(String.format("Date parse error: %s, for url: %s - message: %s", dateString, driver.getCurrentUrl(), ex
                    .getMessage()));
            }
        }
        return date;
    }

    private String getProcurementSubject(String innerID) {
        String result = null;
        boolean trueorfalse = isAble(buildXPathForPublishDate(innerID));
        if (Objects.nonNull(innerID) && trueorfalse) {
            try {
                result = getTextFromDriver(buildXPathForProcSubject(innerID));
            } catch (Exception ex) {
                log.error("The procurement subject text can't found!", ex);
            }
        }
        return result;
    }

    private String getBriefContent(String innerID) {
        String text = null;
        boolean trueorfalse = isAble(buildXPathForPublishDate(innerID));
        if (Objects.nonNull(innerID) && trueorfalse) {
            try {
                text = getTextFromDriver(buildXPathForBriefContent(innerID));
            } catch (Exception ex) {
                log.error("The brief content text can't found!", ex);
            }
        }
        return text;
    }

    private boolean isAble(String xpath){
        boolean isAblevar = false;
        try {
            WebElement procurmentSubject = driver.findElement(By.xpath(xpath));
            if (Objects.nonNull(procurmentSubject)) {
                isAblevar = true;
            }
        }catch (Exception e){
            log.warn(e);
            isAblevar = false;
        }
        return isAblevar;
    }

    private String getTextFromDriver(String xpath) {
        String result = null;
        try {
            WebElement procurmentSubject = driver.findElement(By.xpath(xpath));
            if (Objects.nonNull(procurmentSubject)) {
                result = procurmentSubject.getText();
            }
        }catch (Exception e){
            log.warn(e);
        }

        return result;
    }

    private String buildXPath(String innerID, String closeTagOfXpath) {
        String xpath = String.format(article_xpath, innerID) + closeTagOfXpath;
        log.debug("generated xpath: " + xpath);
        return xpath;
    }

    private String buildXPathForPublishDate(String innerID) {
        return buildXPath(innerID, "//span[@class=\"entry-date\"]");
    }

    private String buildXPathForProcSubject(String innerID) {
        return buildXPath(innerID, "//h2[@class=\"hippo-postheader\"]/span/a");
    }

    private String buildXPathForContent(String innerID) {
        return buildXPath(innerID, "//div[@class='hippo-postcontent']/p//a");
    }

    private String buildXPathForBriefContent(String innerID) {
        return buildXPath(innerID, "//div[@class='hippo-postcontent']");
    }

    private String getContent(String innerID) {
        StringBuilder content = new StringBuilder();
        List<WebElement> attachmentList = new ArrayList<>();
        if (Objects.nonNull(innerID)) {
            String xpath = buildXPathForContent(innerID);
            log.debug("xpath for attachments : " + xpath);
            try {
                attachmentList = driver.findElements(By.xpath(xpath));
            } catch (Exception e){
                log.warn(e);
            }
            //List<WebElement> attachmentList = driver.findElements(By.xpath(xpath));
            log.debug("Number of found attachments: " + attachmentList.size());
            for (WebElement attachment : attachmentList) {
                String link = attachment.getAttribute("href");

                if (StringUtils.containsIgnoreCase(link, constans_string_in_attachments) && !StringUtils.endsWithIgnoreCase(link, ".rar")) {
                    content.append(getContentFromLink(attachment.getAttribute("href")));
                    newProcurement.addURLToFileContentURLs(attachment.getAttribute("href"));
                }
            }
        }
        return content.toString();
    }

    private String getContentFromLink(String urlText) {
        String content = "";
        try {
            URL url = new URL(urlText);
            if (StringUtils.endsWithIgnoreCase(urlText, ".pdf")) {
                content = pdfContentReader.getContentWithExceptions(url, true);
            } else if (StringUtils.endsWithIgnoreCase(urlText, ".doc")) {
                content = wordContentReader.getContent(url);
            } else if (StringUtils.endsWithIgnoreCase(urlText, ".docx")) {
                content = wordContentReader.getContentFromDocx(url);
            } else {
                log.warn(String.format("Unsupported file type found: %s", urlText));
            }
        } catch (Exception ex) {
            log.warn(String.format("Couldn't download the pdf. link: %s", urlText), ex);
        }

        return StringUtils.trim(content);
    }

    public void closeSelenium() {
        DownloaderUtils.closeSelenium(driver);
    }
}
