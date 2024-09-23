import java.io.*;
import java.net.*;

public class Client {

    public static void main(String[] args) {

        try {
            // Create a new socket that will connect to "localhost" at port 1234
            Socket Socket = new Socket("localhost", 1234);

            // Create only output stream since input stream will be created and used in a
            // thread
            PrintWriter out = new PrintWriter(Socket.getOutputStream(), true);

            // Create a input stream that takes input from user terminal
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

            // Create a thread to read incoming messages from server
            messageReader msgReader = new messageReader(Socket);
            Thread msgThread = new Thread(msgReader);
            msgThread.start();

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {

                if (userInput.equals("/exit")) {
                    System.exit(1);
                } else {
                    // Send the users input to the server
                    out.println(userInput);
                }
            }
            // Checks to see if the client can connect to the server, if not, exit
        } catch (IOException e) {
            System.out.println("Could not connect to server, check internet connection");
            System.exit(1);
        }
    }
}

// Thread that will read in messages from the server constantly
class messageReader implements Runnable {

    private Socket clientSocket;
    private BufferedReader in;

    public messageReader(Socket socket) {
        this.clientSocket = socket;

        // Create the input stream to read data in from server
        try {
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Could not create an \"in\" stream");
        }
    }

    @Override
    public void run() {
        // Constantly read input stream for messages from server and display to the
        // client where the're messages
        String serverMessage;
        try {
            while ((serverMessage = in.readLine()) != null) {
                System.out.println(serverMessage);
            }
        } catch (IOException e) {
            System.out.println("Could not connect to server, check internet connection");
        }
    }
}
