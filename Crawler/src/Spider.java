import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Spider {

    private static Thread[] crawlers;
    private static final int CRAWLERS_NUM = 100;

    public static void main(String[] args) throws InterruptedException {


        /*String str = "http://www.tutorialspoint.com/pathtofile/";
        String[] out=(str.split("/"));
        for(int i=0;i<out.length;i++) {
            System.out.println(out[i]);
        }
        URL aURL = new URL(str);
        System.out.println(aURL.getHost());
        System.out.println(aURL.getPath());*/
        /*try (InputStream robotsTxtStream = new URL("https://github.com/robots.txt").openStream()) {
            RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
            boolean hasAccess = robotsTxt.query("*","/humans.txt");
            System.out.println(hasAccess);
        }
        exit(1);*/
        /*Document doc = Jsoup.connect("https://stackoverflow.com").followRedirects(true).get();
        Elements links = doc.select("a[href]");
        for (Element link : links)
        {   String abs="";
        StringBuilder rel=new StringBuilder();
            String url=link.attr("href");
            try {
                //if the url is in perfect format
                URL aURL = new URL(url);
                abs = aURL.getHost();
                rel = new StringBuilder(aURL.getPath());
            } catch (MalformedURLException e) {
                //if the url doesn't begin with http or https then the previous block will throw an exception
                //and splitting on the / means that we get the domain name
                String[] tokens = url.split("/");
                abs = tokens[0];
                for (int i = 1; i < tokens.length; i++)
                    rel.append(tokens[i]);
            }
            System.out.println("relative: "+rel+" abs: "+abs);
            System.out.println();
        }
        //while (true)
        //{

        //}
*/
//        ConcurrentLinkedQueue<String> q = new ConcurrentLinkedQueue<>();
//        try {
//            PrintWriter out_file = new PrintWriter("test.txt");
//            for (Integer i = 0; i < 10; i++)
//            {
//                q.add(i.toString());
//            }
//            for(String sq : q)
//            {
//                System.out.println(sq);
//            }
//            for(String sq : q)
//            {
//                System.out.println(sq);
//            }
//            for (String aQ : q) {
//                out_file.println(aQ);
//            }
//            for (String aQ : q) {
//                out_file.println(aQ);
//            }
//            out_file.close();
//
//        }catch(Exception e)
//        {
//
//        }



        // Create array with number of crawlers needed
        crawlers = new Thread[CRAWLERS_NUM];

        // Initialize seed list
        Crawler.init("seed.txt");

        // Create & Start crawler threads
        crawlers[0] = new Thread(new Crawler(0,true));
        crawlers[0].start();
        for (int i = 1; i < CRAWLERS_NUM; ++i) {
            crawlers[i] = new Thread(new Crawler(i,false));
            crawlers[i].start();
        }

        // Wait for threads to finish
        for (int i = 0; i < CRAWLERS_NUM; ++i)
            crawlers[i].join();

        System.out.println("All crawlers finished!");

        // Save states of queues
        //Crawler.saveQueues("queues.txt", "gathered_links.txt");


    }
}
