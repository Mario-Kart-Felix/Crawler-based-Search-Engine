import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {

    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;

    SnowballStemmer snowballStemmer;


    private Pattern pattern;

    private static Set<String> stopWordSet;


    public  Search ()
    {
        snowballStemmer = new englishStemmer();
        pattern = Pattern.compile("[^a-z A-Z]");

    }


    public void normal_search(String search_text)
    {

        // Table used in search.
        MongoCollection<Document> collection;
        collection = database.getCollection("terms");

        // Pages to be ranked.
        HashMap<String, PageScore> pages = new HashMap<String, PageScore>();

        // Init words count = 0 , used to normalize ranks.
        int words_count = 0;

        // Stemmed version of the searched word.
        String stemmed_word;

        // Split words to search for each word independently.
        String[] words = search_text.split("\\s+");


        for( String word : words)
        {

            // pattern = all ascii codes that are not alphabet.
            Matcher matcher = pattern.matcher(word);
            stemmed_word = matcher.replaceAll("").toLowerCase();

            // Stem.
            stemmed_word = stem(stemmed_word);

            // Check if string is a stop word or empty after stemming.
            if(stemmed_word.length() == 0 || stopWordSet.contains(stemmed_word) || word.length() == 0)
            {

                continue;

            }

            words_count ++;

            Document result = collection.find(Filters.eq("term", stemmed_word)).first();

            if (result == null)
                continue;

            List<Document> documents =  result.get("documents", List.class);

            // Temp page score object to hold page ranks.
            PageScore temp_page_score ;

            for (Document doc : documents)
            {

                String url = doc.getString("url");
                String tag = doc.getString("tag");

                // Get list of unstemmed words to rank page by the number of unstemmed words found.
                List<String> unstemmed_words_in_document = doc.get("unstemmed", List.class);

                if(!pages.containsKey(url))
                {

                    pages.put(url, new PageScore());

                }


                temp_page_score = pages.get(url);


                int index_of_unstemmed = unstemmed_words_in_document.indexOf(word);

                if (index_of_unstemmed != -1)
                    temp_page_score.unstemmed_score = temp_page_score.unstemmed_score + 1;

                // Give score to title.
                if(tag.equals("title"))
                    temp_page_score.title_score+=1;

                temp_page_score.words = temp_page_score.words + 1;

                // Return back the page.
                pages.put(url, temp_page_score);

        }
        }

        for (Map.Entry<String, PageScore> entry : pages.entrySet())
        {

            pages.get(entry.getKey()).unstemmed_score /=words_count;
            pages.get(entry.getKey()).words /=words_count;
            pages.get(entry.getKey()).title_score /=words_count;

            // Logging.
            System.out.println(entry.getKey());
            System.out.println(" unstemmed_score " +
                    entry.getValue().unstemmed_score + " count score : " + entry.getValue().words
                   + " title score : " + entry.getValue().title_score);

        }
    }

    private String stem(String s)
    {

        snowballStemmer.setCurrent(s);
        snowballStemmer.stem();
        return snowballStemmer.getCurrent();

    }

    public static void setInitialParameters(MongoClient _mongo, MongoCredential _credential, MongoDatabase _database, Set<String> _stopWordSet)
    {

        mongo = _mongo;
        credential = _credential;
        database = _database;
        stopWordSet = _stopWordSet;

    }

}
