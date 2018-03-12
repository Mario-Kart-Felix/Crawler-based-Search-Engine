import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class main {

    public static void main (String[] argv){
        try {
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
          /* FindIterable<Document> iterDoc = collection.find();
           int i = 1;

           // Getting the iterator
           Iterator it = iterDoc.iterator();

           while (it.hasNext()) {
               System.out.println(it.next());
               i++;
           }*/
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

}
