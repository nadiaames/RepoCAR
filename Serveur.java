//package tpcar;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Serveur {

    private static ServerSocket serverSocket, dataServer;
    private static Socket clientSocket, dataSocket;
    private static boolean auth = true;
    private static String currentDirectory = System.getProperty("user.dir");
    private static Map<String, String> users = new HashMap<>();

    public static void main(String[] args) {
        users.put("nadia", "nad");
        users.put("miage", "car");

        try {
            serverSocket = new ServerSocket(2121);
            System.out.println("\nServer started on port 2121\n");

            while (true) {
                try {
                    clientSocket = serverSocket.accept();
                    processClientConnection(clientSocket);
                } catch (IOException e) {
                    System.out.println("Error handling client: " + e.getMessage());
                } finally {
                    if (clientSocket != null && !clientSocket.isClosed()) {
                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port 2121: " + e.getMessage());
        }
    }

    private static void processClientConnection(Socket clientSocket) {
        try {
            OutputStream out = clientSocket.getOutputStream();
            InputStream in = clientSocket.getInputStream();
            Scanner scanner = new Scanner(in);

            out.write("220 Service ready \r\n".getBytes());

            while (true) {
                String command = scanner.nextLine();
                System.out.println("Recieved : " + command);

                String[] commandParts = command.split(" ");
                String msgLog = "421 Service not available, login first\r\n";

                if (command.startsWith("USER")) {
                    handleUserAuthentication(commandParts, out, scanner);
                } else if (command.equals("EPSV")) {
                    if (auth == true) {
                        handleEpsvCommand(out);
                    } else {
                        out.write(msgLog.getBytes());
                    }
                } else if (command.startsWith("RETR")) {
                    if (auth == true) {
                        handleRetrCommand(commandParts, out);
                    } else {
                        out.write(msgLog.getBytes());
                    }
                } else if (command.equals("LIST")) {
                    if (auth == true) {
                        handleListCommand(out);
                    } else {
                        out.write(msgLog.getBytes());
                    }
                } else if (command.startsWith("CWD")) {
                    if (auth == true) {
                        handleCwdCommand(commandParts, out);
                    } else {
                        out.write(msgLog.getBytes());
                    }
                } else if (command.toUpperCase().equals("QUIT")) {
                    out.write("221 User logged out\r\n".getBytes());
                    if (clientSocket != null) clientSocket.close();
                } else {
                    out.write("502 Command not implemented\r\n".getBytes());
                }
            }
        } catch (IOException e) {
            System.out.println("! Something wrong happened !");
            e.printStackTrace();
        }
    }

    private static void handleUserAuthentication(String[] commandParts, OutputStream out, Scanner scanner) throws IOException {
        String username = commandParts[1];
        if (users.containsKey(username)) {
            out.write("331 User name okay, need password\r\n".getBytes());
            String passCommand = scanner.nextLine();
            String[] passParts = passCommand.split(" ");
            if (passParts[0].equalsIgnoreCase("PASS") && users.get(username).equals(passParts[1])) {
                out.write("230 User logged in\r\n".getBytes());
            } else {
                out.write("530 Not logged in, incorrect password\r\n".getBytes());
                auth = false;
            }
        } else {
            out.write("530 Not logged in, user not found\r\n".getBytes());
            auth = false;
        }
    }
    

    private static void handleEpsvCommand(OutputStream out) throws IOException {
        dataServer = new ServerSocket(0); 
        int hostPort = dataServer.getLocalPort();
        out.write(("229 Entering Extended Passive Mode (|||" + hostPort + "|)\r\n").getBytes());
    }

    private static void handleRetrCommand(String[] commandParts, OutputStream out) throws IOException {
        String fileName = commandParts[1];
        File file = new File(fileName);

        if (!file.exists()) {
            out.write("550 File not found\r\n".getBytes());
            return;
        }

        out.write("150 Opening BINARY mode data connection for file transfer\r\n".getBytes());

        try (Socket dataSocket = dataServer.accept();
        FileInputStream fis = new FileInputStream(file);
        OutputStream dataOut = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int count;
            while ((count = fis.read(buffer)) > 0) {
                dataOut.write(buffer, 0, count);
            }
            out.write("226 Transfered with success\r\n".getBytes());
        } 
    }
    
    private static void handleListCommand(OutputStream out) throws IOException {
        try (Socket dataSocket = dataServer.accept();
        OutputStream dataOut = dataSocket.getOutputStream()) {

            File directoryCurrent = new File(currentDirectory);
            File[] files = directoryCurrent.listFiles();
            
            if (files != null) {
                out.write("150 Displaying the directory contents\r\n".getBytes());

                for (File file : files) {
                    if (file.isDirectory()) {
                        dataOut.write(("d " + file.getName() + "\r\n").getBytes());
                    } else {
                        dataOut.write(("- " + file.getName() + "\r\n").getBytes());
                    }
                }
                out.write("226 Directory listing sent successfully.\r\n".getBytes());
            } else {
                out.write("550 Directory not found\r\n".getBytes());
                return;
            }
        } 
    }
    
    private static void handleCwdCommand(String[] commandParts, OutputStream out) throws IOException {
        String newDirectory = commandParts[1];
        File newDir = new File(currentDirectory, newDirectory);
        
        if ("..".equals(newDirectory)) {
            if (currentDirectory.equals(System.getProperty("user.dir"))) {
                out.write(("550 Requested action not taken. Cannot move beyond root directory.\r\n").getBytes());
                return;
            } else {
                File parentDir = new File(currentDirectory).getParentFile();
                currentDirectory = parentDir.getAbsolutePath();
                out.write(("250 CWD successful. Current directory is " + currentDirectory + "\r\n").getBytes());
                return;
            }
        }
        
        if (newDir.exists() && newDir.isDirectory()) {
            currentDirectory = newDir.getAbsolutePath();
            out.write(("250 CWD successful. Current directory is " + currentDirectory + "\r\n").getBytes());
        } else {
            out.write("550 Requested action not taken. Directory does not exist\r\n".getBytes());
        }
    }
    

}