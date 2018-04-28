import spark.Filter;

import static spark.Spark.*;

public class main {
    public static void main(String[] argv){
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
            response.body(search.normal_search(suggestion));
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
