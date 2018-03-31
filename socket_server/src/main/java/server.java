import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.Socket;

public class server {

    public static void main(String[] args) {
        int[] ports = new int[]{6667,6666};

        try{

            ServerSocket ss_deter = new ServerSocket(ports[0]);  //create a server socket with port 6666
            ServerSocket ss_transpose = new ServerSocket(ports[1]);  //create a server socket with port 6666

            new Thread(new HandleClient(ss_deter,1)).start();
            new Thread(new HandleClient(ss_transpose,2)).start();

            /*Socket s=ss.accept();//server waits a client to send a request. when the connection request reaches, it establishes connection and returns the socket object that will be used for communication with the client
            BufferedReader in = new BufferedReader( new InputStreamReader(s.getInputStream())); //what does this line do? by now you should already know.
            String  str = in.readLine();  // The server reads a message from the client
            System.out.println("message= "+str);
            ss.close();  //close the server socket*/
        }catch(Exception e){
            System.out.println(e);
        }





    }

    }

