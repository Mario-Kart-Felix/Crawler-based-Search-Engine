import sun.jvm.hotspot.runtime.Threads;

import java.net.ServerSocket;
import java.net.Socket;

public class HandleClient extends Thread {
    ServerSocket s ;
    Socket socket;
    int op;

   public HandleClient(ServerSocket ss,int oper){
       this.op = oper;
       while(true){
           try {
               socket = ss.accept();
               BufferedReader in = new BufferedReader( new InputStreamReader(s.getInputStream()));
           }catch(Exception e ){
               System.out.println(e);
       }
   }

   }



}
