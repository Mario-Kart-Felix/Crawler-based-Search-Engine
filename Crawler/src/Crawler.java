import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static java.lang.Thread.sleep;


public class Crawler implements Runnable {

    private static MongoCollection<org.bson.Document> collection;
    private int ID;
    private static Integer processed_links = 0;     // Total number of processed links by crawler threads
    private boolean save_flag;      // Flag of thread that save queues

    //public static Queue<String> q = new LinkedList<String>();
    public static Deque<String> q = new ConcurrentLinkedDeque<>();
    public static Set visited_links = new HashSet();
    public static ConcurrentHashMap robots = new ConcurrentHashMap<String, RobotsTxt>();
    public static ConcurrentHashMap<String, ArrayList<String>> webGraph = new ConcurrentHashMap<>();
    private static MongoClient mongoClient;
    private static MongoCredential credintials;
    private static MongoDatabase database;

    // Constructor
    public Crawler(int i, boolean flag) {
        ID = i;
        save_flag = flag;

    }

    static public void init(String fileName) {
        try {
             mongoClient = new MongoClient("localhost", 27017);
             credintials = MongoCredential.createCredential("", "test", "".toCharArray());
             database = mongoClient.getDatabase("test");
             collection = database.getCollection("pages");
            System.out.println(collection.find().first());
        } catch (Exception e) {
        }
        if (new File("queues.txt").length() == 0) {
            getSeedList(fileName);
        } else {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("queues.txt")));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    q.add(line);
                }
                bufferedReader = new BufferedReader(new FileReader(new File("gathered_links.txt")));
                while ((line = bufferedReader.readLine()) != null) {
                    visited_links.add(line);
                }
                bufferedReader.close();

                bufferedReader = new BufferedReader(new FileReader(new File("web_graph.txt")));

                line = bufferedReader.readLine();
                String value;
                while ((line = bufferedReader.readLine()) != null) {
                    String key = line;
                    while ((value=bufferedReader.readLine())!=null&&!value.equals("z")) {
                        if (webGraph.containsKey(key)) {
                            webGraph.get(key).add(value);
                        } else {
                            webGraph.put(key, new ArrayList<>(Arrays.asList(value)));
                        }
                    }
                }
                bufferedReader.close();
            } catch (IOException e) {
            }
        }
    }

    // Read seed list from seed file and push it in the queue
    static public void getSeedList(String fileName) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(fileName)));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                q.add(line);
                visited_links.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
        }
    }

    public void run() {
        try {
            if (save_flag) {
                saveDaemon();
            } else crawl();
        } catch (Exception e) {
        }
    }

    private void saveDaemon() {
        long start = 0, end = 0;
        while (true) {
            try {

                // Save queues states every 1 minute
                if (start - end < 20000) {
                    sleep(20000 - (start - end));
                }

                start = System.currentTimeMillis();

                PrintWriter out_file = new PrintWriter("queues.txt");

                Iterator it = q.descendingIterator();
                while (it.hasNext()) {
                    out_file.println(it.next());
                }

                out_file.close();

                out_file = new PrintWriter("gathered_links.txt");

                int i = 0;
                long size = visited_links.size();

                for (Object visited_link : visited_links) {
                    out_file.println(visited_link);
                    if (i++ == size)
                        break;
                }

                out_file = new PrintWriter("web_graph.txt");

                for (Map.Entry<String, ArrayList<String>> entry : webGraph.entrySet()) {
                    String key = entry.getKey();
                    ArrayList<String> value = entry.getValue();
                    out_file.println("z\n" + key);
                    for (String link : value)
                        out_file.println(link);
                }

                out_file.close();

            } catch (Exception e) {

            }

            end = System.currentTimeMillis();

            // Crawling iteration ends
            if (processed_links >= 5000) {
                try {
                    PrintWriter out_file = new PrintWriter("queues.txt");
                    out_file.print("");
                    out_file.close();
                    out_file = new PrintWriter("gathered_links.txt");
                    out_file.print("");
                    out_file.close();
                    out_file = new PrintWriter("web_graph.txt");
                    out_file.print("");
                    out_file.close();
                } catch (Exception e) {

                }
                getNewSeedList();
                break;
            }
        }
    }

    private void getNewSeedList() {

        /*
            Call Ranker function to rank pages and get the top X links
            to make the new seed list.
         */

    }

    public void crawl() throws IOException {
        while (true) {

            // Crawling iteration ends
            if (processed_links >= 5000)
                break;

            try {
                System.out.println("Queue: " + q.size());
                System.out.println("Gathered websites: " + visited_links.size());
                System.out.println("Processed links: " + processed_links);

                String url = "";
                //TODO: Block instead of busy waiting
                while (true) {
                    try {
                        // Get a new url from the queue
                        url = q.poll();
                        url = url.toLowerCase();
                        break;
                    } catch (Exception e) {

                    }
                }


                // Prepend https or http if not found in url
                if (!(url.startsWith("http://") || (url.startsWith("https://")))) {
                    url = "https://" + url;
                }

                // Found in robots file of the domain
                if (!robot(url)) {
                    visited_links.add(url);  // Mark it as visited
                    System.out.println("URL in Robots.txt: " + url);
                    continue;
                }

                //TODO: Check for modified time span
                String date = getServerTime();

                // Send HTTP/HTTPS request
                Connection.Response response = Jsoup.connect(url).header("If-Modified-Since", date).execute();

                System.out.println("Response status code: " + response.statusCode());

                // Response returned with 304 unmodified header
                if (response.statusCode() == 304) {
                    System.out.println("URL not modified. Skipping...");
                    continue;
                }

                synchronized (processed_links) {
                    processed_links++;
                }

                System.out.println("Success: " + url);

                // Parse the HTML page
                Document html_page = response.parse();
                Document doc = Jsoup.parse(html_page.toString());
                //TODO: Extract relative links and resolve dns individually (remove abs:)

                //TODO: Save in a database

                // Split document text into array of strings( both title and body).
                ArrayList<String> splitted_all_text = new ArrayList<String>(Arrays.asList(doc.getAllElements()
                        .text().split("\\s+")));

                ArrayList<String> splitted_title = new ArrayList<String>(Arrays.asList(doc.getElementsByTag("title")
                        .text().split("\\s+")));

                org.bson.Document document = new org.bson.Document("body", splitted_all_text)
                        .append("status", 0)
                        .append("title",splitted_title)
                        .append("url", url);

                try{

                    collection.insertOne(document);


                }catch (Exception e)
                {
                    System.out.println(e.getMessage());
                    e.printStackTrace();

                }

                // Get <a> tags (hyperlinks) in the document
                Elements links = doc.select("a[href]");

                for (Element link : links) {

                    synchronized (visited_links) {
                        //InetAddress giriAddress = java.net.InetAddress.getByName("www.girionjava.com");
                        //String address = giriAddress.getHostAddress();

                        //TODO: Sanitize the url
                        String sanitizedURL = (link.attr("abs:href").split("#"))[0];
                        sanitizedURL = normalizeUrl(sanitizedURL);

                        if (sanitizedURL == null)
                            continue;

                        if (webGraph.containsKey(url)) {
                            webGraph.get(url).add(sanitizedURL);
                        } else {
                            webGraph.put(url, new ArrayList<>(Arrays.asList(sanitizedURL)));
                        }

                        String rel = getRel(link.attr("rel:href"));
                        if (rel.equals("/robots.txt") || rel.equals("robots.txt") || rel.equals("#")) {
                            System.out.println("found robots.txt file! " + rel);
                            continue;
                        }
                        //sanitizedURL=address+"/"+sanitizedURL;
                        //System.out.println(sanitizedURL);
                        //System.exit(1);
                        int prevSize = visited_links.size();
                        visited_links.add(sanitizedURL);
                        if (prevSize < visited_links.size())//this is a new link
                        {
                            //TODO: Sanitize the url and remove last # to eliminate duplicates

                            //TODO: Save current q in case of stopping the process to continue crawling

                            //System.out.println("id:" + id + ", " +sanitizedURL);
                            //System.out.println(Robot(sanitizedURL));
                            q.add(sanitizedURL);
                        }
                    }
                    //System.out.printf(" * a: <%s>  (%s)\n", link.attr("abs:href"), trim(link.text(), 35));
                }
                //System.out.println(visited_links.size());
            } catch (Exception e) {
                //TODO: Do something useful here Exception type:IOException

            }
        }
    }

    private boolean robot(String url) {
        String abs = ""; //absolute and relative urls
        StringBuilder rel = new StringBuilder();
        try {
            try {
                //if the url is in perfect format
                URL aURL = new URL(url);
                abs = aURL.getHost();
                rel = new StringBuilder(aURL.getPath());
            } catch (Exception e) {
                //if the url doesn't begin with http or https then the previous block will throw an exception
                //and splitting on the / means that we get the domain name
                String[] tokens = url.split("/");
                abs = tokens[0];
                for (int i = 1; i < tokens.length; i++)
                    rel.append(tokens[i]);
            }

            if (!robots.containsKey(abs)) {
                StringBuilder robotURL = new StringBuilder("https://" + abs + "/robots.txt");

                try (InputStream robotsTxtStream = new URL(robotURL.toString()).openStream()) {

                    RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
                    synchronized (robots) {
                        robots.put(abs, robotsTxt);
                    }
                    return robotsTxt.query("*", rel.toString());
                } catch (Exception e) {
                    return true;
                }
            } else {
                RobotsTxt robotsTxt = (RobotsTxt) robots.get(abs);
                return robotsTxt.query("*", rel.toString());
            }
        } catch (Exception e) {
            return true;
        }
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }

    public static String getAbs(String url) {
        String abs = "";
        StringBuilder rel = new StringBuilder();

        try {
            //if the url is in perfect format
            URL aURL = new URL(url);
            abs = aURL.getHost();
        } catch (Exception e) {
            //if the url doesn't begin with http or https then the previous block will throw an exception
            //and splitting on the / means that we get the domain name
            String[] tokens = url.split("/");
            abs = tokens[0];
        }
        return abs;
    }

    private static String getRel(String url) {
        StringBuilder rel = new StringBuilder();
        try {
            //if the url is in perfect format
            URL aURL = new URL(url);
            rel = new StringBuilder(aURL.getPath());
        } catch (MalformedURLException e) {
            //if the url doesn't begin with http or https then the previous block will throw an exception
            //and splitting on the / means that we get the domain name
            String[] tokens = url.split("/");
            for (int i = 1; i < tokens.length; i++)
                rel.append(tokens[i]);
        }
        return rel.toString();
    }

    private String getServerTime() {

        Calendar calendar = Calendar.getInstance();

        //TODO: Change re-crawling unmodified time span according to page rank
        calendar.add(Calendar.DATE, -10);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());

    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private String normalizeUrl(String urlString) throws URISyntaxException {
        if (urlString == null || urlString.equals("") || urlString.equals(" ")) {
            return null;
        }

        urlString = urlString.replaceAll(" ", "%20");
        urlString = urlString.replaceAll("\\[", "%5B");
        urlString = urlString.replaceAll("]", "%5D");

        URI uri = new URI(urlString);
        uri = new URI(uri.toASCIIString());
        if (!uri.isAbsolute()) {
            throw new URISyntaxException(urlString, "Must provide an absolute URI for repositories");
        }

        uri = uri.normalize();
        String path = uri.getPath();
        if (path != null) {
            path = path.replaceAll("//*/", "/"); // Collapse multiple forward slashes into 1.
            if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
                path = path.substring(0, path.length() - 1);
            }
        }

        return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
                path, uri.getQuery(), uri.getFragment()).toString();
    }

    public void finalize() throws FileNotFoundException {
        System.out.println("Thread destroyed!!");
    }
}

//no saving documents in database
//no graph