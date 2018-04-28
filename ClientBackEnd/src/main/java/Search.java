import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import org.apache.commons.text.similarity;
public class Search {

    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;
    static ConcurrentHashMap<String,Float> ranks = new ConcurrentHashMap<>();

    private SnowballStemmer snowballStemmer;


    private Pattern pattern;

    private static Set<String> stopWordSet;
    public static String[] stop_words = {"a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves", "zero"};


    public Search() {
        connectDB();
        initializeStopWords();
        snowballStemmer = new englishStemmer();
        pattern = Pattern.compile("[^a-z A-Z]");

    }

    public void add_suggestion(String sugg){
        MongoCollection<Document> collection;
        collection = database.getCollection("suggestions");
        collection.insertOne(new Document("suggestion",sugg));
    }

    public String get_suggestions(String sugg){
        MongoCollection<Document> collection;
        collection = database.getCollection("suggestions");
        FindIterable<Document>documents= collection.find();
        ArrayList<String>words=new ArrayList<>();
        for(Document doc : documents){
            String suggestion=(String)doc.get("suggestion");
            words.add(suggestion);
            //int val = FuzzySearch.weightedRatio(sugg,suggestion);
        }

        words.sort(Comparator.comparingInt((String z) -> FuzzySearch.weightedRatio(sugg, z)));

        JSONObject data = new JSONObject();

        if(words.size()>10)
        data.put("Suggestions", words.subList(0,10));
        else
            data.put("Suggestions",words);
        return data.toString();
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
                    for(int i = Math.max(indx-10,0); i < Math.min(indx+10,body.size()); i++)
                    {
                        snippet.append(body.get(i)).append(" ");
                    }
                    JSONObject object = new JSONObject();

                    object.put("url", ""+url+"");
                    object.put("title",""+title+"");
                    object.put("snippet",""+snippet+"");
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
//
//        json_array.deleteCharAt(json_array.length()-1);
//        json_array.append("]");

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
    public String normal_search(String search_text) {

        // Table used in search.
        MongoCollection<Document> collection;
        collection = database.getCollection("terms");
        MongoCollection<Document> page_collection = database.getCollection("pages");

        // Pages to be ranked.
        HashMap<String, PageScore> pages = new HashMap<String, PageScore>();
        HashMap<String, List<Integer>> page_positions = new HashMap<String, List<Integer>>();

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
                List<Integer> positions = doc.get("positions",List.class);


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

                // Add page positions.
                if(!page_positions.containsKey(url))
                {
                    page_positions.put(url, new ArrayList<Integer>());
                }
                List<Integer> temp_pos = page_positions.get(url);
                temp_pos.add(positions.get(0));

                page_positions.put(url,temp_pos);

            }
        }

        TreeMap<String,Double> mp = new TreeMap<String, Double>();

        ArrayList<String> sorted_urls  = new ArrayList<String>();

        double weight_unstemmed = 1;
        double weight_words = 0.5;
        double weight_title = 6;

        for (Map.Entry<String, PageScore> entry : pages.entrySet()) {

            pages.get(entry.getKey()).unstemmed_score /= words_count;
            pages.get(entry.getKey()).words /= words_count;
            pages.get(entry.getKey()).title_score /= words_count;

            // Score.
            double score =  (entry.getValue().unstemmed_score * weight_unstemmed+
                    entry.getValue().words * weight_words+
                    entry.getValue().title_score * weight_title);

            mp.put(entry.getKey(), score+ ranks.get(entry.getKey()));

            sorted_urls.add(entry.getKey());
            // Logging.
//            System.out.println(entry.getKey());
//            System.out.println(" unstemmed_score " +
//                    entry.getValue().unstemmed_score + " count score : " + entry.getValue().words
//                    + " title score : " + entry.getValue().title_score);

        }

        sorted_urls.sort(Comparator.comparing(mp::get).reversed());

//        for(String key : mp.descendingKeySet()){
//            System.out.println("value of " + key + " is " + mp.get(key));
//        }


        FindIterable<Document> top_pages = page_collection.find(Filters.in("url",sorted_urls));


        JSONArray jsonarray = new JSONArray();

        for(Document top_page : top_pages)
        {

            JSONObject object = new JSONObject();
            String url = top_page.getString("url");
            StringBuilder snippet = new StringBuilder();

            StringBuilder title = new StringBuilder();
            List<String> title_arr = top_page.get("title", List.class);
            for( String s : title_arr)
                title.append(s);

            List<Integer> curr_page_pos = page_positions.get(url);

            List<String> body = top_page.get("body", List.class);

            for(int j = 0; j< Math.min(curr_page_pos.size(), 50); j++)
            {
                int pos = curr_page_pos.get(j);
                for(int i = Math.max(0, pos-5); i<Math.min(pos+5, body.size()); i++)
                {

                    snippet.append(body.get(i)).append(" ");

                }
                snippet.append("....");
            }
            object.put("url", ""+url+"");
            object.put("title",""+title+"");
            object.put("snippet",""+snippet+"");
            jsonarray.add(object);

        }

        return jsonarray.toJSONString();
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
    private static void connectDB() {
        try {

            MongoClientOptions.Builder clientOptions = new MongoClientOptions.Builder();
            clientOptions.connectionsPerHost(120);

            mongo = new MongoClient(new ServerAddress("localhost",27017),clientOptions.build());
            //mongo = new MongoClient("localhost:27017?replicaSet=rs0&maxPoolSize=200", 27017);
            credential = MongoCredential.createCredential("", "test", "".toCharArray());
            database = mongo.getDatabase("test");

        }catch(Exception e){

            System.out.println("error connecting to database "+e.getMessage());

        }
    }
    public  void initializeStopWords(){
        snowballStemmer = new englishStemmer();

        for(int i = 0; i < stop_words.length; i++)
            stop_words[i] = stem(stop_words[i]);
        stopWordSet = new HashSet<String>(Arrays.asList(stop_words));
    }
}