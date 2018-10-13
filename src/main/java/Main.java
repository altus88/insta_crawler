import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.bonigarcia.wdm.WebDriverManager;

import static io.github.bonigarcia.wdm.DriverManagerType.CHROME;

public class Main
{
    static Pattern instaHashTagPattern = Pattern.compile("#[^# ]+");

    public static void main(String[] args) throws InterruptedException, IOException
    {
        String tagName = args[0];
        WebDriverManager.getInstance(CHROME).setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options);
        driver.get("https://www.instagram.com/explore/tags/" + tagName + "/");
        Actions builder = new Actions(driver);

        Set<String> previousItemsUrls = new HashSet<>();
        Map<String, Integer> hashTagPopularity = new HashMap<>();
        int loop = 0;
        long begin = System.currentTimeMillis();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("#" + tagName + ".txt")))
        {
            for (int i = 0; i < 10; i++)
            {
                long start = System.currentTimeMillis();
                 //jse.executeScript("scroll(0," + scrollDistance + ");");
                List<WebElement> postElements = driver.findElements(By.cssSelector("div.v1Nh3.kIKUG._bz0w"));
                System.out.print(++loop + " ");
                for (WebElement postElement : postElements)
                {
                    System.out.print("*");
                    WebElement linkToPostElement = postElement.findElement(By.tagName("a"));
                    String linkToPost = linkToPostElement.getAttribute("href");
                    if (!previousItemsUrls.contains(linkToPost)) // check if we parsed already this post
                    {
                        previousItemsUrls.add(linkToPost);
                        builder.moveToElement(postElement).perform(); // mouse hover on the element to see number of likes
                       // ((JavascriptExecutor)driver).executeScript("arguments[0].click();", postElement);
                        //Thread.sleep(1000);
                        Integer numberOfLikes = getNumberOfLikes(postElement);
                        if (numberOfLikes >= 500)
                        {
                            WebElement postDescriptionElement = linkToPostElement.findElement(By.cssSelector("div.KL4Bh img"));
                            String postDescription = postDescriptionElement.getAttribute("alt");
                            List<String> hashTagsFromPost = extractHashTags(postDescription);
                            for (String hashTag : hashTagsFromPost)
                            {
                                hashTagPopularity.merge(hashTag, 1, (v1, v2) -> v1 + v2);
                            }
                            hashTagsFromPost.add(linkToPost);
                            hashTagsFromPost.add(numberOfLikes.toString()); // add count likes at the end
                            writer.write(String.join(",", hashTagsFromPost));
                            writer.write("\n");
                        }
                    }
                }
                long elapsed = System.currentTimeMillis() - start;
                System.out.println(" Unique posts: "  + previousItemsUrls.size() + ". Time elapsed: " + elapsed);
  //              scrollDistance += 2000;
            }
        }

        System.out.println("Overall time elapsed: " + (System.currentTimeMillis() - begin));
        // put hash tags in sorted order by likes
        writeInSortedOrderHashTags("#"+ tagName + "_stat.txt", hashTagPopularity);
        driver.close();
    }

    private static Integer getNumberOfLikes(WebElement webElement) throws InterruptedException
    {
        String numberOfLikesStr = null;
        try
        {
            numberOfLikesStr = webElement.findElement(By.cssSelector("div._6S0lP ul.Ln-UN li.-V_eO span")).getText();
        } catch (Exception e)
        {
            Thread.sleep(500);
            numberOfLikesStr = webElement.findElement(By.cssSelector("div._6S0lP ul.Ln-UN li.-V_eO span")).getText();
        }
        Integer numberOfLikes = numberOfLikesToString(numberOfLikesStr);
        return numberOfLikes;
    }

    private static void writeInSortedOrderHashTags(String fileName, Map<String, Integer> hashTagPopularity) throws IOException
    {
        List<HashTagCount> hashTagCounts = hashTagPopularity.entrySet().stream().map(entry -> new HashTagCount(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        Collections.sort(hashTagCounts);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName)))
        {
            for (HashTagCount hashTagCount : hashTagCounts)
            {
                writer.write(hashTagCount.hashTag + " " + hashTagCount.count);
                writer.newLine();
            }
        }
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
    }

    private static Integer numberOfLikesToString(String numberOfLikesStr)
    {
        try
        {   numberOfLikesStr = numberOfLikesStr.replace(",", "");
            numberOfLikesStr = numberOfLikesStr.replace(".", "");
            numberOfLikesStr = numberOfLikesStr.replace("k", "000");
            return Integer.parseInt(numberOfLikesStr);
        } catch (Exception e)
        {
            System.out.println("Could not parse: '" + numberOfLikesStr + "'");
            return 0;
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
}
