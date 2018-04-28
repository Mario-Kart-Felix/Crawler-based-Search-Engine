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
            Search search= new Search();
            return search.normal_search(request.params("name"));
            //return "Hello: " + request.params("name");
        });
        get("/suggest/:name", (request, response) -> {

            //return search.normal_search(request.params("name"));
            return "Hello: " + request.params("name");
        });
    }

}
