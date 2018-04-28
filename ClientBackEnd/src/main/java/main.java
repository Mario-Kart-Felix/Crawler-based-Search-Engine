import spark.Filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.*;

public class main {


    public static void main(String[] argv){


        // Load ranks
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("ranks.txt")));
            String url,score;
            while ((url = bufferedReader.readLine()) != null) {
                score  = bufferedReader.readLine();
                Search.ranks.put(url,Float.parseFloat(score));
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("ERROR READING RANKS.TXT0");
        }


        before((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET,POST");
            response.header("Content-Type", "text/html");
            response.type("text/html");
        });
        get("/search/:name", (request, response) -> {
            String suggestion=request.params("name");
            Search search= new Search();
            //TODO:insert search for the database
            search.add_suggestion(suggestion);
            response.status(200);
            String val = search.normal_search(suggestion);
            response.body(val);
            System.out.println(val);
            if(response.body()==null||response.body().equals(""))
                response.body("not empty");
            return response.body();
            //return "Hello: " + request.params("name");
        });
        get("/suggest/:name", (request, response) -> {
            //TODO:search for the database for similar suggestions
            String suggestion=request.params("name");
            Search search = new Search();
            response.body(search.get_suggestions(suggestion));
            response.status(200);
            if(response.body()==null||response.body().equals(""))
                response.body("not empty");
            //return search.normal_search(request.params("name"));
            return response.body();
        });
    }

}
