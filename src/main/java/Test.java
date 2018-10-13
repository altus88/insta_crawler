import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Test
{
    static Pattern instaHashTagPattern = Pattern.compile("#[^# ]+");

    public static void main(String[] args)
    {
        System.out.println("222k".replace("k", "000"));
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
