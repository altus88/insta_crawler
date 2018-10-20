package crawler.instargam.com;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.proxy.CaptureType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.bonigarcia.wdm.WebDriverManager;

import static io.github.bonigarcia.wdm.DriverManagerType.CHROME;

public class Test
{
    static Pattern instaHashTagPattern = Pattern.compile("#[^# ]+");

    public static void main(String[] args) throws IOException, InterruptedException
    {
        testTrafficCatch();
//        Map<String, String> parameters = new HashMap<>();
//        parameters.put("query_hash", "f92f56d47dc7a55b606908374b43a314");
//        parameters.put("variables", "{\"tag_name\":\"love\",\"first\":10,\"after\":\"QVFEcVJBeGtHa3B5QmF1QUphaWdVSmMwS28yZTZ3cGV2Y3J0b2tfTFJvM2FHR21GeXFHSzd3amFhZlpsZWd5cmFOX25GNXVXcTZ5em1uQkVHVFVuSlpjdw==\"}");
//
//        Document doc = Jsoup.connect("https://www.instagram.com/graphql/query/").data(parameters).get();
//        System.out.println();

    }

    public static void testTrafficCatch() throws InterruptedException
    {
        WebDriverManager.getInstance(CHROME).setup();
        ChromeOptions options = new ChromeOptions();
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.start(0);
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

        // get the Selenium proxy object
        options.setCapability(CapabilityType.PROXY, seleniumProxy);
        WebDriver driver = new ChromeDriver(options);

        proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);

        driver.get("https://www.instagram.com/explore/tags/cars/");

        Actions builder = new Actions(driver);
        for (int i = 1; i <= 100; i++)
        {
            proxy.newHar("insta");
            List<WebElement> postElements = driver.findElements(By.cssSelector("div.v1Nh3.kIKUG._bz0w"));
            WebElement latestElement = null;
            for (WebElement webElement : postElements)
            {
                latestElement = webElement;
            }
            builder.moveToElement(latestElement).perform();
            Har har = proxy.getHar();
            List<String> requests = new ArrayList<>();

            for (HarEntry harEntry : har.getLog().getEntries())
            {
                if (harEntry.getRequest().getUrl().startsWith("https://www.instagram.com/graphql/query/"))
                {
                    System.out.println(harEntry);
                    driver.get(harEntry.getRequest().getUrl());
                    Thread.sleep(5000);
                    System.out.println();
                }
            }
//


        }
    }

    private static List<String> extractHashTags(String str)
    {
        List<String> hashTags = new ArrayList<>();
        Matcher hashTagMather = instaHashTagPattern.matcher(str);

        while (hashTagMather.find())
        {
            hashTags.add(hashTagMather.group());
        }
        return hashTags;
    }

    private static class HashTagCount implements Comparable<HashTagCount>
    {
        String hashTag;
        Integer count;

        public HashTagCount(String hashTag, Integer count)
        {
            this.hashTag = hashTag;
            this.count = count;
        }

        @Override
        public int compareTo(HashTagCount o)
        {
            return o.count.compareTo(this.count);
        }

        @Override
        public String toString()
        {
            return "HashTagCount{" +
                    "hashTag='" + hashTag + '\'' +
                    ", count=" + count +
                    '}';
        }
    }
}
