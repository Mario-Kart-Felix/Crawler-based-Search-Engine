import java.util.*;
public class Page {
   public Set<String> unstemmed;
   public List<Integer> positions;
   public String url;
   public String tag;
   public Page(){
       unstemmed = new HashSet<String>();
       positions = new ArrayList<Integer>();
   }

}
