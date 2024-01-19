package tpcar;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.net.*;
import java.nio.file.*;

class Serveur {
    public static void main(String[] args) {
    	
        try (ServerSocket serverSocket = new ServerSocket(2121)) {
           
        	System.out.println("connected with success to localhost");
            Socket sockt = serverSocket.accept();
            
            OutputStream out = sockt.getOutputStream();
            out.write("220 Service ready \r\n".getBytes());
           
            InputStream in = sockt.getInputStream();
            Scanner scanner = new Scanner(in);
           
            while (true) {
            	
                String username = scanner.nextLine();
                
                if ("USER nadia".equals(username)) {
                    out.write("331 User name ok \r\n".getBytes());
                    System.out.println(username);
                    String password = scanner.nextLine();
                    if ("PASS nad1".equals(password)) {
                        out.write("230 User logged in \r\n".getBytes());
                        System.out.println(password);
                    } else {
                        out.write("530 Not logged in \r\n".getBytes());
                    }
                } else {
                    out.write("User Not exists \r\n".getBytes());
                }
                
                
                while (true) {
                    String str = scanner.nextLine();
                    if (str.equalsIgnoreCase("quit")) {
                        out.write("210 Exit \r\n".getBytes());
                        System.out.println("exit");
                        break;
                    } else {
                        out.write("Invalid command \r\n".getBytes());
                    }
                }
                
                String line = scanner.nextLine();
                
                while (line != null) {
                    System.out.println("File name received: " + line);
                    if (line.startsWith("get ")) {
                        String fileName = line.substring(4).trim();
                        openFile(fileName, out);
                    } else {
                        out.write("200 you typed something else \r\n".getBytes());
                    }
                    line = scanner.nextLine();
                }
                
                scanner.close();
                sockt.close();
            }
        } catch (IOException e) {
            System.out.println("! Something wrong happened !");
            e.printStackTrace();
        }
    }

    private static void openFile(String fileName, OutputStream out) throws IOException {
        String projectDirectory = "/home/m1ipint/nadia.amesrouy.etu/eclipse-workspace/tpcar/src/tpcar";
        Path filePath = Paths.get(projectDirectory, fileName);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            
        	/****/
        	out.write(" File is about to open \r\n".getBytes());
        } else {
            out.write(" File not found \r\n".getBytes());
        }
    }
}
