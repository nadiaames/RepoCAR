package tpcar;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.net.*;




class Serveur{
 public static void main(String[] args) {
 
 ServerSocket serverSocket;
 try {
 serverSocket = new ServerSocket(2121);
 System.out.println("connected with success to localhost");

 while (true) {

 Socket sockt = serverSocket.accept();
 OutputStream out =sockt.getOutputStream();
 String str="200 Service ready\r\n";
 out.write(str.getBytes());

 InputStream in = sockt.getInputStream();
  Scanner scanner = new Scanner(in);

 String username= scanner.nextLine();
 System.out.println(username);
 out.write("331 User name ok \r\n".getBytes());

 String motdepasse= scanner.nextLine();
 out.write("230 User logged in \r\n".getBytes());
 System.out.println(motdepasse);
 scanner.close();

 }
 } catch (IOException e) {
 e.printStackTrace();
 }

 }
