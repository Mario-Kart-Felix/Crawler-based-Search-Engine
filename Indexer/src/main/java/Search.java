import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Search {

    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;

    SnowballStemmer snowballStemmer;


    private Pattern pattern;

    private static Set<String> stopWordSet;


    public Search() {
        snowballStemmer = new englishStemmer();
        pattern = Pattern.compile("[^a-z A-Z]");

    }

    public String phrase_search(String search_text) {

        // Json array to pass results.
       StringBuilder json_array = new StringBuilder("[");

        JSONArray jsonarray = new JSONArray();

        // Split words to search for each word independently.
        String[] words = search_text.split("\\s+");

        // First word to start with the phrase search.
        int phrase_start_index = 0;

        // Clean String of text.
        for ( int i = 0; i<words.length; i++)
        {

            // pattern = all ascii codes that are not alphabet.
            Matcher matcher = pattern.matcher(words[i]);
            words[i] = matcher.replaceAll("").toLowerCase();

            // If word in stopping words, advance the pointer.
            if(stopWordSet.contains(words[i]))
                phrase_start_index++;

            else
                break;

        }

        // If all stop words then return.
        if(phrase_start_index>= words.length)
            return jsonarray.toJSONString();

        // First word to search with.
        String word = words[phrase_start_index];

        // Table used in search.
        MongoCollection<Document> collection;
        collection = database.getCollection("terms");

        // Pages_collection.
        MongoCollection<Document> page_collection = database.getCollection("pages");


        List<String> urls = new ArrayList<String>();
            AggregateIterable<Document> docs = collection.aggregate(
                    Arrays.asList(
                            new Document("$unwind", "$documents"),
                            new Document("$match", new Document("documents.unstemmed", word)),
                                    new Document("$project", new Document("document","$documents"))
                    )
            );
            for (Document doc : docs) {

                Document document = doc.get("document", Document.class);
                String url = document.getString("url");

                if(url.equals("http://blog.portswigger.net/2015/02"))

                {
                    int x = 3;
                }
                Document page = page_collection.find(Filters.eq("url",  url)).first();
                List<String> body =  page.get("body", List.class);

                List<Integer> positions = document.get("positions",List.class);

                for( Integer indx : positions)
                {

                    String word_in_doc = body.get(indx);

                    // Clean the word in doc.
                    Matcher matcher = pattern.matcher(word_in_doc);
                    word_in_doc = matcher.replaceAll("").toLowerCase();

                    // Check if word != word in doc.
                    if(!word.equals(word_in_doc))
                        continue;

                    // Walk through the document to get the exact phrase.

                    int start_indx = indx;
                    int lenght_of_remaining_phrase = words.length - phrase_start_index;

                    // If remaining words are less than the phrase length.
                    if (indx + lenght_of_remaining_phrase > body.size())
                        return jsonarray.toJSONString();

                    Boolean matched = false;
                    int words_start = phrase_start_index+1;
                    int matched_words_count = phrase_start_index+1;
                    for(int i = indx + 1; i< indx + lenght_of_remaining_phrase; i++)
                    {

                        // pattern = all ascii codes that are not alphabet.
                        Matcher doc_word_matcher = pattern.matcher(body.get(i));
                        String word_to_compare = doc_word_matcher.replaceAll("").toLowerCase();

                        if(!words[words_start].equals(word_to_compare) &&
                                !stopWordSet.contains(words[words_start]) &&
                                !stopWordSet.contains(body.get(i)))
                        {


                            break;
                        }

                        if( words[words_start].equals(word_to_compare) || stopWordSet.contains(words[words_start]))
                        {

                            matched_words_count++;

                        }
                        words_start++;

                    }

                    if(matched_words_count == words.length)
                    {

                        // Title string.
                        StringBuilder title = new StringBuilder();
                        List<String> title_arr = page.get("title", List.class);
                        for( String s : title_arr)
                        {
                            title.append(s).append(" ");
                        }
                        StringBuilder snippet = new StringBuilder();
                        for(int i = Math.max(indx-50,0); i < Math.min(indx+50,body.size()); i++)
                        {
                            snippet.append(body.get(i)).append(" ");
                        }
                        JSONObject object = new JSONObject();
                        object.put("url", url);
                        object.put("snippet", snippet);
                        object.put("title", title);
                        jsonarray.add(object);
                        json_array.append("{" + "\"url\":\"")
                                .append(url).append("\",")
                                .append("\"snippet\":")
                                .append("\""+snippet+"")
                                .append("\",")
                                .append("\"title\":\"")
                                .append(title)
                                .append("\"},");
//                        System.out.println(" matched url : " + url );
//                        System.out.println(" snippet : " + snippet );
//                        System.out.println(" title : " + title );
//                        System.out.println(" json : " + json_array );

                        break;
                    }

                }

            }

            json_array.deleteCharAt(json_array.length()-1);
            json_array.append("]");

            return jsonarray.toJSONString();
            // Get term with  this word in the unstemmed array.
           /*  Bson filter = Filters.in("documents.unstemmed",word);

            // Get term with this word.
            Document term_document = collection.find(filter).first();

            List<Document> documents = term_document.get("documents",List.class);

            for( Document doc : documents)
            {
                String url = doc.getString("url");

                // If url is in the map already.
                if(!urls.contains(url))
                {
                    continue;
                }

                List<String> unstemmed = doc.get("unstemmed", List.class);

                if(!unstemmed.contains(url))
                {
                    continue;
                }

                List<Integer> positions = doc.get("positions", List.class);

                Document page = page_collection.find(Filters.eq("url", url)).first();

                List<String> body = page.get("body", List.class);
                int x = 3;

            }

            // Get pages has this word.
*/

        }



    // TODO:: Remove stop words.
    public void normal_search(String search_text) {

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


        for (String word : words) {

            // pattern = all ascii codes that are not alphabet.
            Matcher matcher = pattern.matcher(word);
            word = matcher.replaceAll("").toLowerCase();
            stemmed_word = word;
            // Stem.
            stemmed_word = stem(stemmed_word);

            // Check if string is a stop word or empty after stemming.
            if (stemmed_word.length() == 0 || stopWordSet.contains(stemmed_word) || word.length() == 0) {

                continue;

            }

            words_count++;

            Document result = collection.find(Filters.eq("term", stemmed_word)).first();

            if (result == null)
                continue;

            List<Document> documents = result.get("documents", List.class);

            // Temp page score object to hold page ranks.
            PageScore temp_page_score;

            for (Document doc : documents) {

                String url = doc.getString("url");
                String tag = doc.getString("tag");

                // Get list of unstemmed words to rank page by the number of unstemmed words found.
                List<String> unstemmed_words_in_document = doc.get("unstemmed", List.class);

                if (!pages.containsKey(url)) {

                    pages.put(url, new PageScore());

                }


                temp_page_score = pages.get(url);


                int index_of_unstemmed = unstemmed_words_in_document.indexOf(word);

                if (index_of_unstemmed != -1)
                    temp_page_score.unstemmed_score = temp_page_score.unstemmed_score + 1;

                // Give score to title.
                if (tag.equals("title"))
                    temp_page_score.title_score += 1;

                temp_page_score.words = temp_page_score.words + 1;

                // Return back the page.
                pages.put(url, temp_page_score);

            }
        }

        for (Map.Entry<String, PageScore> entry : pages.entrySet()) {

            pages.get(entry.getKey()).unstemmed_score /= words_count;
            pages.get(entry.getKey()).words /= words_count;
            pages.get(entry.getKey()).title_score /= words_count;

            // Logging.
            System.out.println(entry.getKey());
            System.out.println(" unstemmed_score " +
                    entry.getValue().unstemmed_score + " count score : " + entry.getValue().words
                    + " title score : " + entry.getValue().title_score);

        }

    }

    private String stem(String s) {

        snowballStemmer.setCurrent(s);
        snowballStemmer.stem();
        return snowballStemmer.getCurrent();

    }

    public static void setInitialParameters(MongoClient _mongo, MongoCredential _credential, MongoDatabase _database, Set<String> _stopWordSet) {

        mongo = _mongo;
        credential = _credential;
        database = _database;
        stopWordSet = _stopWordSet;

    }

}
