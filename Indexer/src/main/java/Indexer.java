import com.mongodb.*;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.internal.connection.ConcurrentLinkedDeque;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;

public class Indexer implements Runnable{
    static AtomicInteger i = new AtomicInteger();
    Map<String,Integer> tags_rank;
    MongoClient mongo;
    MongoCredential credential;
    MongoDatabase database;
    public  Set<String> stopWordSet ;
    Map<String,Page> terms_map=new HashMap<String, Page>();
    Pattern pattern;
    List<Document> documents_to_process;
    MongoCollection <Document> collection;
    ConcurrentLinkedDeque<Document> deque;
    //stemmer
    SnowballStemmer snowballStemmer ;
    public   Indexer(MongoClient _mongo, MongoCredential _credential, MongoDatabase _database, Set<String> _stopWordSet , ConcurrentLinkedDeque<Document> _deque){
        deque=_deque;
        snowballStemmer = new englishStemmer();
        mongo = _mongo;
        credential = _credential;
        database = _database;
        pattern = Pattern.compile("[^a-z A-Z]");
        stopWordSet = _stopWordSet;

        init_tags_rank();
    }
    public String stem(String s){
        snowballStemmer.setCurrent(s);
        snowballStemmer.stem();
        return snowballStemmer.getCurrent();
    }

    public void setDocuments(List<Document> docs){
        documents_to_process = docs;
    }

    public void run(){
        collection = database.getCollection("terms");
        while(true) {
            while (deque.isEmpty()) {
                try {
                    sleep(300);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            terms_map.clear();
            Document doc = deque.poll();
            updateDocument(doc.getString("url"));
            addDocument(doc.getString("body"), doc.getString("url"));
            System.out.println("documenets indexed " +i.getAndIncrement());
        }
    }

    private void init_tags_rank() {
        tags_rank= new HashMap<String,Integer>();
        tags_rank.put("h1",10);
        tags_rank.put("h2",9);
        tags_rank.put("h3",7);
        tags_rank.put("h4",6);
        tags_rank.put("h5",5);
        tags_rank.put("h6",4);
        tags_rank.put("b",8);
        tags_rank.put("title",11);
    }


    public void DFS(Elements elements, AtomicInteger current_word_pos, String url){


           for(Element element :elements ){
               {
                   if(element.ownText().length() != 0)
                   processText(element.ownText(),element.tagName(),terms_map,  current_word_pos,url);

                   if(element.children().size()!=0)
                    DFS(element.children(),current_word_pos, url);
               }
           }




    }
    public void addDocument(String _document, String url){
        org.jsoup.nodes.Document document = Jsoup.parse(_document);


        AtomicInteger current_word_pos = new AtomicInteger(0);
        //Elements e = document.children().first().children().first().children();
        //DFS(document.children(),current_word_pos,url);


      //TODO: stem and remove stop words
       Page page;
        //processText(element.ownText(),element.tagName(),terms_map,  current_word_pos,url);
        //Integer current_word_pos = 0;
        Elements title = document.getElementsByTag("title");

        processText(title.text().toString(),"title",terms_map,  current_word_pos,url);
        Elements h;
        for(Integer i =1; i<=6; i++){
                h = document.getElementsByTag("h"+ i.toString());
            processText(h.text(),"h"+ i.toString(),terms_map,  current_word_pos,url);
        }
        Elements p = document.getAllElements();

        processText(p.text(),"p",terms_map,  current_word_pos,url);

        List<WriteModel<Document>> updates = new ArrayList<WriteModel<Document>>();
        try {
            for (Map.Entry<String, Page> entry : terms_map.entrySet()) {
                Document temp_doc = new Document("positions", entry.getValue().positions)
                        .append("url", entry.getValue().url)
                        .append("tag", entry.getValue().tag)
                        .append("unstemmed", entry.getValue().unstemmed);
                //  System.out.println(entry.getKey() + "/" + entry.getValue().positions.get(0));
            /*collection.updateOne(eq("term", entry.getKey()),
                    Updates.addToSet("documents",temp_doc),
                    new UpdateOptions().upsert(true));*/
                updates.add(new UpdateOneModel<Document>(eq("term", entry.getKey()),
                        Updates.addToSet("documents", temp_doc),
                        new UpdateOptions().upsert(true)));
               // System.out.println("updates " + updates.size());
            }

        }catch (Exception e){
            System.out.println("update error -> " + e.getMessage());
        }

        BulkWriteResult bulkWriteResult = null;
        try {
            //Bulk write options
            BulkWriteOptions bulkWriteOptions = new BulkWriteOptions();
            bulkWriteOptions.ordered(false); //False to allow parallel execution
            bulkWriteOptions.bypassDocumentValidation(true);


            //Perform bulk update
            bulkWriteResult = collection.bulkWrite(updates,
                    bulkWriteOptions);
        }catch (BulkWriteException e) {
            //Handle bulkwrite exception
            List<BulkWriteError> bulkWriteErrors = e.getWriteErrors();
            for (BulkWriteError bulkWriteError : bulkWriteErrors) {
                int failedIndex = bulkWriteError.getIndex();
                System.out.println("Failed record: " +failedIndex);
            }

        }
    }
    public void processText(String text, String tag, Map<String, Page> terms_map, AtomicInteger current_word_pos, String url){

        Matcher matcher = pattern.matcher(text);
        text = matcher.replaceAll("").toLowerCase();

        Page page;

        try {
            String[] splitArray = text.split("\\s+");
            String unstemmed;
            for(int i = 0 ; i<splitArray.length; i++){

                //store unstemmed
                unstemmed = splitArray[i];
                //stem the word
                splitArray[i] = stem(splitArray[i]);

                // check if it is a stop word
                if(stopWordSet.contains(splitArray[i]) || splitArray[i].length() == 0)
                      continue;
                //check if word already exists
                if(!terms_map.containsKey(splitArray[i])){
                    // System.out.println("no term : "+term);
                    terms_map.put(splitArray[i],new Page());
                }
                // TODO: if the page is new , insert the page with positions and tags
                page = terms_map.get(splitArray[i]);
                //System.out.println(current_word_pos);
                page.positions.add(current_word_pos.getAndIncrement());


                if(page.tag != null && tags_rank.containsKey(page.tag) && tags_rank.containsKey(tag) ){
                    if((tags_rank.get(tag)>tags_rank.get(page.tag)))
                        page.tag=tag;
                }else{
                    page.tag=tag;
                }

                page.url= url;
                page.unstemmed.add(unstemmed);
                terms_map.put(splitArray[i],page);

            }
        } catch (Exception ex) {
            System.err.println(" error " + ex.getMessage());
        }




    }

    public void updateDocument(String url){
        MongoCollection <Document> collection;
        collection = database.getCollection("terms");
       // collection.deleteMany(Filters.eq("documents.$.url",url), );
        //collection.updateMany(Filters.eq("documents.url", url), Updates.unset("documents.$" ));
      Bson delete = Updates.pull("documents", new Document("url",url));
        Bson filter = Filters.eq("documents.url", url);
        collection.updateMany(filter,delete);
    }
    public void deleteDocument(String url){

    }

    public void normal_search(String term) {
        MongoCollection <Document> collection;
        collection = database.getCollection("terms");
        FindIterable<Document> result = collection.find(Filters.eq("term",term));
        for(Document doc :result)
        System.out.println(doc.toJson() + "\n ---------------------- \n ");
    }
}
