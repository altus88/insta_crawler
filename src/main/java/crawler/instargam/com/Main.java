package crawler.instargam.com;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

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
import org.openqa.selenium.interactions.SourceType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.bonigarcia.wdm.WebDriverManager;

import static io.github.bonigarcia.wdm.DriverManagerType.CHROME;

public class Main
{
    static Pattern instaHashTagPattern = Pattern.compile("#[^# ]+");

    static String awsAccessKey = "AKIAJESO4JSOQ34R46GA";

    static String awsSecretKey = "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";

    static String awsS3Bucket = "altus88";


    public static void main(String[] args) throws InterruptedException, IOException
    {
        String tagName = args[0];
        Boolean isProduction = args.length > 1 && args[1].equals("prod");

        WebDriverManager.getInstance(CHROME).setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--incognito");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        driver.get("https://www.instagram.com/explore/tags/" + tagName + "/");
        Thread.sleep(1000);
        Actions builder = new Actions(driver);

        Set<String> previousItemsUrls = new HashSet<>();
        Map<String, Integer> hashTagPopularity = new HashMap<>();
        int loop = 0;
        long begin = System.currentTimeMillis();
        String postsTagsFileName = "#" + tagName + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(postsTagsFileName)))
        {
            for (int i = 1; i <= 1000; i++)
            {
                long start = System.currentTimeMillis();
                 //jse.executeScript("scroll(0," + scrollDistance + ");");
                List<WebElement> postElements = driver.findElements(By.cssSelector("div.v1Nh3.kIKUG._bz0w"));
                System.out.print(++loop + " ");
                for (WebElement postElement : postElements)
                {
                    try
                    {
                        System.out.print("*");
                        WebElement linkToPostElement = postElement.findElement(By.tagName("a"));
                        String linkToPost = linkToPostElement.getAttribute("href");
                        if (!previousItemsUrls.contains(linkToPost)) // check if we parsed already this post
                        {
                            previousItemsUrls.add(linkToPost);
                            builder.moveToElement(postElement).perform(); // mouse hover on the element to see number of likes
//                        String javaScript = "var evObj = document.createEvent('MouseEvents');" +
//                                "evObj.initMouseEvent(\"mouseover\",true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);" +
//                                "arguments[0].dispatchEvent(evObj);";

//                        Integer numberOfLikes = getNumberOfLikes(postElement);
//                        if (numberOfLikes >= 500)
//                        {
                            WebElement postDescriptionElement = linkToPostElement.findElement(By.cssSelector("div.KL4Bh img"));
                            String postDescription = postDescriptionElement.getAttribute("alt");
                            List<String> hashTagsFromPost = extractHashTags(postDescription);
                            for (String hashTag : hashTagsFromPost)
                            {
                                hashTagPopularity.merge(hashTag, 1, (v1, v2) -> v1 + v2);
                            }
                            hashTagsFromPost.add(linkToPost);
                            //hashTagsFromPost.add(numberOfLikes.toString()); // add count likes at the end
                            writer.write(String.join(",", hashTagsFromPost));
                            writer.write("\n");
//                        }
                        }
                    }
                    catch (Exception ex)
                    {
                        System.out.println("Sleep in case of exception : " + ex.getMessage());
                        Thread.sleep(5000);
                    }
                }

//                if (i % 1000 == 0)
//                {
//                    writeInSortedOrderHashTags("#"+ tagName + "_stat.txt", hashTagPopularity);
//                }

                Thread.sleep(2000);
                long elapsed = System.currentTimeMillis() - start;
                System.out.println(" Unique posts: "  + previousItemsUrls.size() + ". Time elapsed: " + elapsed);
  //              scrollDistance += 2000;
            }
        }

        System.out.println("Overall downloading time elapsed: " + (System.currentTimeMillis() - begin));
        // put hash tags in sorted order by likes
        String statFileName = "#"+ tagName + "_stat.txt";
        writeInSortedOrderHashTags(statFileName, hashTagPopularity);

        if (isProduction)
        {
            System.out.println("Put data in s3...");
            putFileInS3Bucket(new File(statFileName), statFileName);
            putFileInS3Bucket(new File(postsTagsFileName), postsTagsFileName);
            System.out.println("Finished");
        }
        driver.close();
    }

    private static AmazonS3 getS3Client()
    {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        return AmazonS3ClientBuilder.standard().withRegion("eu-central-1").withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
    }

    public static void putFileInS3Bucket(File file, String key)
    {
        PutObjectRequest por = new PutObjectRequest(awsS3Bucket, key, file);
        por.setCannedAcl(CannedAccessControlList.PublicRead);
        getS3Client().putObject(por);
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
