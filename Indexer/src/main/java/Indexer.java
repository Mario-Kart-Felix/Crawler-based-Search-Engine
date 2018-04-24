import com.mongodb.*;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class Indexer implements Runnable {

    private static AtomicInteger i = new AtomicInteger();
    private static Map<String, Integer> tags_rank;
    private static MongoClient mongo;
    private static MongoCredential credential;
    private static MongoDatabase database;
    private static Set<String> stopWordSet;
    HashMap<String, Page> terms_map = new HashMap<String, Page>();
    private Pattern pattern;
    private Document documents_to_process;
    private MongoCollection<Document> collection;

    // Stemmer.
    SnowballStemmer snowballStemmer;

    public Indexer(Document _document)
    {

        snowballStemmer = new englishStemmer();

        pattern = Pattern.compile("[^a-z A-Z]");

        documents_to_process = _document;

    }

    private String stem(String s)
    {

        snowballStemmer.setCurrent(s);
        snowballStemmer.stem();
        return snowballStemmer.getCurrent();

    }


    public void run()
    {

        terms_map.clear();
        collection = database.getCollection("terms");
        updateDocument(documents_to_process.getString("url"));
        addDocument();
        //addDocument(documents_to_process.getString("body"), documents_to_process.getString("url"));
        System.out.println("documenets indexed " + i.getAndIncrement());

    }

    private static void init_tags_rank()
    {

        tags_rank = new HashMap<String, Integer>();
        tags_rank.put("h1", 10);
        tags_rank.put("h2", 9);
        tags_rank.put("h3", 7);
        tags_rank.put("h4", 6);
        tags_rank.put("h5", 5);
        tags_rank.put("h6", 4);
        tags_rank.put("b", 8);
        tags_rank.put("title", 11);

    }



    private void addDocument()
    {

        AtomicInteger current_word_pos = new AtomicInteger(0);

        List<String> title = documents_to_process.get("title",List.class);
        List<String> full_text = documents_to_process.get("body",List.class);
        String url = documents_to_process.getString("url");

        // Process title, we don't change position here.
        processText(title, "title", terms_map, current_word_pos, url);

        // Process whole body.
        processText(full_text, "p", terms_map, current_word_pos, url);

        List<WriteModel<Document>> updates = new ArrayList<WriteModel<Document>>();

        try {
            for (Map.Entry<String, Page> entry : terms_map.entrySet()) {
                Document temp_doc = new Document("positions", entry.getValue().positions)
                        .append("url", entry.getValue().url)
                        .append("tag", entry.getValue().tag)
                        .append("unstemmed", entry.getValue().unstemmed);

                updates.add(new UpdateOneModel<Document>(eq("term", entry.getKey()),
                        Updates.addToSet("documents", temp_doc),
                        new UpdateOptions().upsert(true)));

            }

        } catch (Exception e)
        {

            System.out.println("update error -> " + e.getMessage());

        }

        BulkWriteResult bulkWriteResult = null;

        try
        {

            // Bulk write options.
            BulkWriteOptions bulkWriteOptions = new BulkWriteOptions();
            bulkWriteOptions.ordered(false); //False to allow parallel execution
            bulkWriteOptions.bypassDocumentValidation(true);


            // Perform bulk update.
            bulkWriteResult = collection.bulkWrite(updates,
                    bulkWriteOptions);
        } catch (BulkWriteException e)
        {

            // Handle bulkwrite exception.
            List<BulkWriteError> bulkWriteErrors = e.getWriteErrors();

            for (BulkWriteError bulkWriteError : bulkWriteErrors)
            {

                int failedIndex = bulkWriteError.getIndex();
                System.out.println("Failed record: " + failedIndex);

            }
        }
    }

    private void processText(List<String> splitArray, String tag, Map<String, Page> terms_map, AtomicInteger current_word_pos, String url)
    {

        Page page;

        try {
           // String[] splitArray = text.split("\\s+");
            String unstemmed;
            for (int i = 0; i < splitArray.size(); i++) {

                // Store unstemmed.
                unstemmed = splitArray.get(i);

                // Remove unwanted symbols.
                Matcher matcher = pattern.matcher(splitArray.get(i));
                splitArray.set(i,matcher.replaceAll("").toLowerCase());

                // Stem the word.
                splitArray.set(i, stem(splitArray.get(i)));

                // Check if it is a stop word.
                if (stopWordSet.contains(splitArray.get(i)) || splitArray.get(i).length() == 0)
                {

                    current_word_pos.getAndIncrement();

                    continue;

                }

                // Check if word already exists.
                if (!terms_map.containsKey(splitArray.get(i)))
                {

                    terms_map.put(splitArray.get(i), new Page());

                }

                page = terms_map.get(splitArray.get(i));

                // If it is title don't add position it will be added later.
                if (!tag.equals("title"))
                    page.positions.add(current_word_pos.getAndIncrement());

                if (page.tag != null && tags_rank.containsKey(page.tag) && tags_rank.containsKey(tag))
                {

                    if ((tags_rank.get(tag) > tags_rank.get(page.tag)))
                        page.tag = tag;

                } else
                    {

                        page.tag = tag;

                    }

                page.url = url;
                page.unstemmed.add(unstemmed);
                terms_map.put(splitArray.get(i), page);

            }
        } catch (Exception ex)
        {

            System.err.println(" error " + ex.getMessage());

        }

    }

    private void updateDocument(String url)
    {

        MongoCollection<Document> collection;
        collection = database.getCollection("terms");
        // collection.deleteMany(Filters.eq("documents.$.url",url), );
        //collection.updateMany(Filters.eq("documents.url", url), Updates.unset("documents.$" ));
        Bson delete = Updates.pull("documents", new Document("url", url));
        Bson filter = Filters.eq("documents.url", url);

        collection.updateMany(filter, delete);

    }

    public static void setInitialParameters(MongoClient _mongo, MongoCredential _credential, MongoDatabase _database, Set<String> _stopWordSet) {

        mongo = _mongo;
        credential = _credential;
        database = _database;
        stopWordSet = _stopWordSet;

        init_tags_rank();

    }

    public void normal_search(String term) {
        MongoCollection<Document> collection;
        collection = database.getCollection("terms");
        Document result = collection.find(Filters.eq("term", term)).first();
        FindIterable<Document> docs = (FindIterable<Document>) result.get("documents");
        for (Document doc : docs)
            System.out.println(doc.getString("url") + "\n ---------------------- \n ");
    }
}
