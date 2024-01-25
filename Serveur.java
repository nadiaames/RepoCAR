package tpcar;

import java.io.*;
import java.util.Scanner;
import java.net.*;

class Serveur {

    private static ServerSocket serverSocket, dataServer;
    private static Socket dataSocket; 
    public static void main(String[] args) {

        try {
            serverSocket = new ServerSocket(2121);

            System.out.println("connected with success to localhost");
            Socket sockt = serverSocket.accept();

            OutputStream out = sockt.getOutputStream();
            out.write("220 Service ready \r\n".getBytes());

            InputStream in = sockt.getInputStream();
            try (Scanner scanner = new Scanner(in)) {
                while (true) {

                    String command = scanner.nextLine();               
                    System.out.println("Client Response: " + command);
                   
                    String[] commandParts = command.split(" ");
                   
                    if ("USER nadia".equals(command)) {
                        out.write("331 User name ok \r\n".getBytes());
                        System.out.println(command);
                        String password = scanner.nextLine();
                        if ("PASS nad".equals(password)) {
                            out.write("230 User logged in \r\n".getBytes());
                            System.out.println(password);
                        } else {
                            out.write("530 Not logged in \r\n".getBytes());
                        }
                    } else if (command.equals("SYST")) {
                        out.write("215 UNIX Type: L8\r\n".getBytes());
                    } else if (command.equals("FEAT")) {
                        out.write("211-Features:\r\n PASV\r\n SIZE\r\n UTF8\r\n211 End\r\n".getBytes());
                    } else if (command.equals("TYPE I")) {
                        out.write("200 Binary mode set\r\n".getBytes());
                    } else if (command.startsWith("SIZE")) {
                        // gerer la commande SIZE
                        handleSizeCommand(command, out);
                    } else if (command.equals("EPSV") || command.equals("PASV")) {
                        // gerer la commande EPSV/PASV
                        handlePasvCommand(out);
                    } else if (command.startsWith("RETR")) {
                        // gerer la commande RETR (telechargement de fichier)
                        handleRetrCommand(commandParts, out);
                    } else if (command.toUpperCase().equals("QUIT")) {
                        out.write("221 User logged out\r\n".getBytes());
                		System.out.println("exit");
                        break;
                    } else {
                        out.write("502 Command not implemented\r\n".getBytes());
                    }

                }
            }
        } catch (IOException e) {
            System.out.println("! Something wrong happened !");
            e.printStackTrace();
        }
    }

    private static void handleSizeCommand(String command, OutputStream out) throws IOException {
        String[] parts = command.split("\\s+");
        if (parts.length == 2) {
            String filename = parts[1];
            File file = new File(filename);
            if (file.exists() && file.isFile()) {
                long fileSize = file.length();
                out.write(("213 " + fileSize + "\r\n").getBytes());
            } else {
                out.write("550 Requested action not taken. File not found\r\n".getBytes());
            }
        } else {
            out.write("501 Syntax error in parameters or arguments\r\n".getBytes());
        }
    }

    
    private static void handlePasvCommand(OutputStream out) throws IOException {
        // creer un nouveau ServerSocket pour ecouter la connexion de donnees
        try {            
			dataServer = new ServerSocket(0);
            int hostPort= dataServer.getLocalPort();
            out.write(("229 Entering Extended Passive Mode (" + hostPort + ")\r\n").getBytes());
        } catch (IOException e) {
            out.write("500 Internal Server Error\r\n".getBytes());
        }
    }
    
    
    private static void handleRetrCommand(String[] commandParts, OutputStream out) throws IOException {
        // verifier si la commande a suffisamment d'arguments et si la connexion de donnee est etablie
        if (commandParts.length > 1 && dataSocket != null && dataSocket.isConnected()) {
        String fileName = commandParts[1];
        File file = new File("/home/m1ipint/nadia.amesrouy.etu/eclipse-workspace/tpcar/src/tpcar/files" + fileName);

        if (!file.exists()) {
            out.write("550 File not found\r\n".getBytes());
            return;
        }

        out.write("150 Opening binary mode data connection\r\n".getBytes());

        try (FileInputStream fis = new FileInputStream(file);
            OutputStream dataOut = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int count;

            while ((count = fis.read(buffer)) > 0) {
                dataOut.write(buffer, 0, count);
            }

            out.write("226 Transfered with success\r\n".getBytes());
        } finally {
            // Ferme la connexion de donnee et reutilise la socket
            dataSocket.close();
            dataSocket = null; 
        }
        } else {
            out.write("425 Can't open data connection\r\n".getBytes());
        }
    }

  
    
}
