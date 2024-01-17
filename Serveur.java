package tpcar;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.net.*;

class Serveur {
	public static void main(String[] args) {

		ServerSocket serverSocket;

		try {

			serverSocket = new ServerSocket(2121);
			System.out.println("connected with success to localhost");

			Socket sockt = serverSocket.accept();
			OutputStream out = sockt.getOutputStream();

			InputStream in = sockt.getInputStream();
			Scanner scanner = new Scanner(in);

			out.write("200 Service ready \r\n".getBytes());

			while (true) {

				String username = scanner.nextLine();
				if ("USER nadia".equals(username)) {
					out.write("331 User name ok \r\n".getBytes());
					System.out.println(username);

					String password = scanner.nextLine();

					if ("PASS nad1".equals(password)) {
						out.write("230 User logged in \r\n".getBytes());
					} else {
						out.write("530 Not logged in \r\n".getBytes());
						break;
					}
				} else {
					out.write("User Not exists \r\n".getBytes());
					break;
				}

				scanner.close();

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}