package tpcar;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Serveur {

    private static ServerSocket serverSocket, dataServer;
    private static Socket clientSocket, dataSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(2121);
            System.out.println("Server started on port 2121");

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
                if (!scanner.hasNextLine())
                    break;
                String command = scanner.nextLine();
                System.out.println("Client Response: " + command);

                String[] commandParts = command.split(" ");

                if (command.startsWith("USER")) {
                    handleUserAuthentication(commandParts, out, scanner);
                } else if (command.equals("SYST")) {
                    out.write("215 UNIX Type: L8\r\n".getBytes());
                } else if (command.equals("FEAT")) {
                    out.write("211-Features:\r\n PASV\r\n SIZE\r\n UTF8\r\n211 End\r\n".getBytes());
                } else if (command.equals("TYPE I")) {
                    out.write("200 Binary mode set\r\n".getBytes());
                } else if (command.startsWith("SIZE")) {
                    handleSizeCommand(commandParts, out);
                } else if (command.equals("EPSV") || command.equals("PASV")) {
                    handlePasvCommand(out);
                } else if (command.startsWith("RETR")) {
                    handleRetrCommand(commandParts, out);
                } else if (command.toUpperCase().equals("QUIT")) {
                    out.write("221 User logged out\r\n".getBytes());
                    break;
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
        if (commandParts.length > 1) {
            String username = commandParts[1];
    
            if ("nadia".equalsIgnoreCase(username)) {
                out.write("331 User name okay, need password\r\n".getBytes());
                
                if (scanner.hasNextLine()) {
                    String passCommand = scanner.nextLine();
                    String[] passParts = passCommand.split(" ");
                    if (passParts[0].equalsIgnoreCase("PASS") && passParts.length > 1 && "nad".equals(passParts[1])) {
                        out.write("230 User logged in\r\n".getBytes());
                    } else {
                        out.write("530 Not logged in, incorrect password\r\n".getBytes());
                    }
                } else {
                    out.write("501 Syntax error in parameters or arguments\r\n".getBytes());
                }
            } else {
                out.write("530 Not logged in, user not found\r\n".getBytes());
            }
        } else {
            out.write("501 Syntax error in parameters or arguments\r\n".getBytes());
        }
    }

    private static void handleSizeCommand(String[] commandParts, OutputStream out) throws IOException {
        if (commandParts.length > 1) {
            String fileName = commandParts[1];
            File file = new File(fileName);
            if (file.exists()) {
                out.write(("213 " + file.length() + "\r\n").getBytes());
            } else {
                out.write("550 File not found\r\n".getBytes());
            }
        } else {
            out.write("501 Syntax error in parameters or arguments\r\n".getBytes());
        }
    }

    private static void handlePasvCommand(OutputStream out) throws IOException {
        dataServer = new ServerSocket(0); 
        int hostPort = dataServer.getLocalPort();
        out.write(("229 Entering Extended Passive Mode (|||" + hostPort + "|)\r\n").getBytes());
        dataSocket = dataServer.accept();
    }

    private static void handleRetrCommand(String[] commandParts, OutputStream out) throws IOException {
        // verifier si la commande a suffisamment d'arguments et si la connexion de donnee est etablie
        if (commandParts.length > 1 && dataSocket != null) {
            String fileName = commandParts[1];
            File file = new File(fileName);

            if (!file.exists()) {
                out.write("550 File not found\r\n".getBytes());
                return;
            }

            out.write("150 Opening BINARY mode data connection for file transfer\r\n".getBytes());

            try (FileInputStream fis = new FileInputStream(file);
                    OutputStream dataOut = dataSocket.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    dataOut.write(buffer, 0, count);
                }
                out.write("226 Transfered with success\r\n".getBytes());
            } finally {
                if (dataSocket != null && !dataSocket.isClosed()) {
                    dataSocket.close();
                }
                if (dataServer != null && !dataServer.isClosed()) {
                    dataServer.close();
                }
            }
        } else {
            out.write("425 Can't open data connection\r\n".getBytes());
        }
    }
}