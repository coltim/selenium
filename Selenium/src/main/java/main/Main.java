package main;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {

        final String SELENIUM_DISABLE_FULL_PAGE_TYPE = "application/pdf;application/msword;application/vnd.openxmlformats-officedocument.wordprocessingml.document;application/download;binary/octet-stream;application/binary;application/octet-stream;\n" +
            "        application/force-download;application/x-unknown;application/x-download;application/sdk";
        final String SELENIUIM_DOWNLOAD_TYPES = "application/pdf;application/msword;application/vnd.openxmlformats-officedocument.wordprocessingml.document;application/download;binary/octet-stream;application/binary;application/octet-stream;\n" +
            "        application/force-download;application/x-unknown;application/x-download;application/sdk";
        String downloadDir = "C:\\Users\\cseke.tamas\\Downloads";
        File firefoxPath = new File("C:\\Users\\cseke.tamas\\Downloads\\FirefoxPortable_27.0.1_English.paf.exe");
        FirefoxBinary firefoxBinary = new FirefoxBinary(firefoxPath);
        firefoxBinary.setEnvironmentProperty("DISPLAY", System.getProperty("lmportal.xvfb.id", ":1"));

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("plugin.disable_full_page_plugin_for_types", SELENIUM_DISABLE_FULL_PAGE_TYPE);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk", SELENIUIM_DOWNLOAD_TYPES);
        profile.setPreference("browser.helperApps.neverAsk.openFile", "");
        profile.setPreference("pdfjs.disabled", true);
        profile.setPreference("pdfjs.firstRun", true);
        profile.setPreference("pdfjs.previousHandler.alwaysAskBeforeHandling", true);
        profile.setPreference("plugin.state.nppdf", 0);
        if (downloadDir == null) {
            profile.setPreference("browser.download.dir", downloadDir);
        }
        profile.setPreference("browser.download.folderList", 2);

        WebDriver driver = new FirefoxDriver(firefoxBinary, profile);
        driver.manage().timeouts().setScriptTimeout(-1, TimeUnit.SECONDS);
        driver.manage().timeouts().pageLoadTimeout(-1, TimeUnit.SECONDS);
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /*driver.get("https://www.google.com/");
        driver.findElement(By.name("q")).sendKeys("javatpoint tutorials");
        driver.findElement(By.name("btnK")).click();*/

        driver.get("https://index.hu/techtud");
        List<WebElement> elementName = driver.findElements(By.xpath("(//*[@id='ajanlok']/li)"));
        for (WebElement webElement : elementName) {
            System.out.println(webElement.findElement(By.xpath(".//h1/a")).getText());
        }

    }
}
