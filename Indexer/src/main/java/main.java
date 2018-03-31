import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoDatabase;
import org.jsoup.Jsoup;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Time;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {
    static MongoClient mongo;
    static MongoCredential credential;
    static MongoDatabase database;
    public static String[] stop_words = {"a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves", "zero"};
    public static Set<String> stopWordSet ;
    public static SnowballStemmer snowballStemmer;

    static List<Thread>indexers ;
    public static String stem(String s){
        snowballStemmer.setCurrent(s);
        snowballStemmer.stem();
        return snowballStemmer.getCurrent();
    }

    public static void initializeStopWords(String[] stop_words){
        snowballStemmer = new englishStemmer();

        for(int i = 0; i < stop_words.length; i++)
            stop_words[i] = stem(stop_words[i]);
        stopWordSet = new HashSet<String>(Arrays.asList(stop_words));
    }
    public static void main (String[] argv){
        //establish database connection once
        connectDB();
        initializeStopWords(stop_words);
     //   long startTime = System.nanoTime();
//
//
     //   String test = "_Al * i";
//
     //   Pattern pattern = Pattern.compile("[^a-z A-Z]");
     //   Matcher matcher = pattern.matcher(test);
     //   String text = matcher.replaceAll("");
//
     //   System.out.println(text);
//
     //   long endTime   = System.nanoTime();
     //   long totalTime = endTime - startTime;
     //   System.out.println(totalTime/100000);

     //   Indexer indexer = new Indexer(mongo, credential, database, stopWordSet);
       // indexer.run();

        indexers = new ArrayList<Thread>();
        for(int i =0 ; i < 20; i++)
        {
            Thread temp = new Thread(new Indexer(mongo, credential, database, stopWordSet));
            temp.start();
            indexers.add(temp);
        }
       // Indexer indexer = new Indexer(mongo, credential, database, stopWordSet);
       // indexer.updateDocument("index.html");/

        StringBuilder html6= new StringBuilder();
/*

        SnowballStemmer snowballStemmer = new englishStemmer();
        String  test = "playing";
       // test = test.toLowerCase().replaceAll("[-+.^:,_]","");
        snowballStemmer.setCurrent(test);
       snowballStemmer.stem();
        String result = snowballStemmer.getCurrent();
        System.out.println(result);
*/
       // System.out.println(mp.containsKey("ali"));
        try {
            File file = new File("src/main/in.html");
            FileReader fr = new FileReader(file);
            BufferedReader bfr = new BufferedReader(fr);
            String Line;
            while((Line=bfr.readLine() )!= null){
                html6.append(Line);

            }
            String html = "<html><head><title>First parse</title></head>"
                    + "<body><p>Parsed HTML into a doc.</p><p><div><h1>heading  <h2>heading </h2></h1></div></p></body></html>";
            //org.jsoup.nodes.Document document = Jsoup.parse(html6.toString());
            //System.out.println(html6);


            //indexer.addDocument(document,"short_doc.html");
            try {
               //indexer.updateDocument("short_doc.html");
        //        indexer.normal_search("Add");
            }catch (Exception e ) {
            System.err.println(e.getMessage());
            }

            }catch(Exception e){

        }


      /*  Elements elements = document.select("*");

        for (Element element : elements) {
            System.out.println(element.tagName()+"   "+element.ownText());
        }*/
       /* for(Element e : elements)
            System.out.println( e.tagName() + ": " + e.text());
*/
        /*for(int i = 0; i<all_tags.size(); i++){
            Element element = all_tags.first();
            System.out.println(element.);
            System.out.println(element.text());
            all_tags.next();
        }*/
        //System.out.println(all_tags);
        /*try {
            System.out.println("start");

            MongoClient mongo = new MongoClient("localhost", 27017);
            MongoCredential credential;
            credential = MongoCredential.createCredential("", "test", "".toCharArray());
            System.out.println("Connected to the database successfully");
            MongoDatabase database = mongo.getDatabase("test");
            System.out.println("Credentials ::" + credential);
            //database.createCollection("sampleCollection");(table)
            MongoCollection <Document> collection = database.getCollection("terms");

            System.out.println("Collection myCollection selected successfully");
            List<Integer> arr = Arrays.asList(1,2,3);
            Document page = new Document("positions", arr)
                    .append("url", "world.com")
                    .append("freq", 10)
                    .append("tag", 1);
            List<Document> documents = new ArrayList<Document>();
            documents.add(page);
            Document document = new Document("term", "dodo")
                    .append("documents", documents);
            //collection.insertOne(document);
            //update documents
            //update,delete -> set,unset
            collection.updateOne(Filters.eq("documents.url", "g.com"), Updates.unset("documents.$"));
            //collection.deleteOne(Filters.eq("x.url", "world.com"));
            System.out.println("Document update successfully..."+ collection.count());
            //retrieve documents.
           FindIterable<Document> iterDoc = collection.find();
           int i = 1;

           // Getting the iterator
           Iterator it = iterDoc.iterator();

           while (it.hasNext()) {
               System.out.println(it.next());
               i++;
           }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        */
    }
    private static void connectDB() {
        try {
            mongo = new MongoClient("localhost", 27017);
            credential = MongoCredential.createCredential("", "test", "".toCharArray());
            database = mongo.getDatabase("test");
        }catch(Exception e){
            System.out.println("error connecting to database "+e.getMessage());

        }
    }

}
