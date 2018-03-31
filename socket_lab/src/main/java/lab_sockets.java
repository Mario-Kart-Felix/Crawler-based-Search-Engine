import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class lab_sockets {


    public static void main(String[] args) {
        int[][] matrix = new int[3][3];

        int operation;
        int[] ports = new int[]{6667,6666};
        System.out.println(" enter matrix 3  x 3 ");

        Scanner sc=new Scanner(System.in);
        for(int i =0 ; i<3; i++)
            for(int j = 0 ; j<3; j++){
            matrix[i][j] = sc.nextInt();

        }
        System.out.println(" enter operation ");
        operation = sc.nextInt();
        Socket s;

        try{
            if(operation == 1)
                s=new Socket("localhost",ports[0]);  // establish a connection with the server with IP address "localhost" or "127.0.0.1" and port 6666, it is the same port used in the server
            else {

                s=new Socket("localhost",ports[1]);
            }
            //ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream out=new ObjectOutputStream(s.getOutputStream());
            out.writeObject(matrix);

            Thread.sleep((new Random()).nextInt(10000));

            ObjectInputStream in = new ObjectInputStream(s.getInputStream());
            int result;
            if(operation ==1)
            {

                result = in.readInt();
                System.out.println(" determinant is " + result);
            }
            else{
                matrix = (int[][]) in.readObject();

            }

            s.close();  //close the socket
        }catch(Exception e){
            System.out.println(e);
        }




    }
}
