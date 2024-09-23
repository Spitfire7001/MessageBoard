import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class Server {

    // Create an array list of chat boards for users to connect to
    private static ArrayList<messageBoard> messageBoards = new ArrayList<>();

    // A String of commands offered by a server
    private static String listOfCommands = "\n/help --> display command options\n/MSGBoards --> displays current message boards\n/join --> connect to a message board\n/leave --> leaves current message board\n/create --> creates a message board with the name specified\n/exit --> terminates the app";

    // An array list of files that hold all message boards
    private static ArrayList<File> messageBoardFiles = new ArrayList<>();

    public static void main(String[] args) {

        // Initializes server data on startup
        serverStartUp();
        try {

            // Initializa server socket and client socket
            @SuppressWarnings("resource")
            ServerSocket serverSocket = new ServerSocket(1234);

            // Listen for client connections
            while (true) {
                // Accept new client
                Socket clientSocket = serverSocket.accept();
                System.out.println("New Chatter Connected");

                // Run new client on a thread
                clientThread clientThread = new clientThread(clientSocket);
                Thread thread = new Thread(clientThread);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Error occured");
            System.exit(1);
        }
    }

    // Initializes all message boards saved in folder
    public static void serverStartUp() {
        // Path to message boards folder
        File folder = new File("messageBoards");

        // Need to check if folder is real and a folder
        if (folder.exists() && folder.isDirectory()) {
            // Obtain list of .txt files and add to arrayList
            File[] files = folder.listFiles();
            for (File file : files) {
                messageBoardFiles.add(file);
            }

            // After obtaining each file need to create the message boards
            // and add stored meesage boards to message board array
            for (File file : messageBoardFiles) {
                messageBoard storedBoard = new messageBoard(removeFileExtenion(file.getName()));
                messageBoards.add(storedBoard);
                // Reading from a file
                try (BufferedReader reader = new BufferedReader(new FileReader("messageBoards/" + file.getName()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        storedBoard.addMessage(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Successfully Initialized Message Boards");
        } else {
            System.out.println("Folder does not excist or cannot be found");
        }
    }

    public static String removeFileExtenion(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public static synchronized void sendMessageToAll(String message, messageBoard board) {
        // Iterate through the clientThreds arraylist and send message to each client

        for (clientThread clientThread : board.getClients()) {
            clientThread.sendMessage(message);
        }
    }

    // Returns current server time for user messages
    public static String getCurrentServerTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return (currentDateTime.format(dateFormat));
    }

    // Returns a list of commands the server offers
    public static String getServerCommands() {
        return (listOfCommands);
    }

    // Return a list of Message Boards
    public static ArrayList<messageBoard> getMessageBoards() {
        return messageBoards;
    }

}

class clientThread implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username = null;
    private messageBoard currentBoard = null;

    public clientThread(Socket socket) {
        this.clientSocket = socket;

        try {
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {

        }
    }

    // Sends message to a client
    public void sendMessage(String message) {
        out.println(message);
    }

    public void setUsername() throws IOException {
        // Send a message to user asking for a username
        out.println("Please input your username: ");

        String userInput;
        // Sets username to be what the user sends back to server
        while ((userInput = in.readLine()) != null) {
            this.username = userInput;
            out.println("Username set.\n");
            return;
        }

    }

    // Creates a message board
    public void createMessageBoard() throws IOException {
        // Send a message to user asking for message board name
        out.println("Imput name of message board:");

        String userInput;
        // Gets user input and then sends back to the server to create message board
        while ((userInput = in.readLine()) != null) {

            // Check each board to see if that name is already taken
            // If name is taken exit function
            for (messageBoard board : Server.getMessageBoards()) {
                if (board.getName().equals(userInput)) {
                    out.println("Name is already taken");
                    return;
                }
            }
            messageBoard messageBoard = new messageBoard(userInput);
            ArrayList<messageBoard> messageBoards = Server.getMessageBoards();
            messageBoard.addMessage("This is the begining of the Message Board!");
            messageBoards.add(messageBoard);

            // Write begining message to message board .txt file
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter("messageBoards/" + messageBoard.getName() + ".txt", true))) {
                writer.write("This is the begining of the Message Board!");
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;
        }
        out.println("Message Board Created.\n");
    }

    public void joinMessageBoard() throws IOException {

        if (Server.getMessageBoards().size() > 0 && this.currentBoard == null) {
            // Sends a message to user asking for which message board to join
            out.println("Input name of message board");

            // Init variables
            messageBoard messageBoard = null;
            String userInput;

            while ((userInput = in.readLine()) != null) {
                ArrayList<messageBoard> messageBoards = Server.getMessageBoards();

                // Finds matching name,
                for (messageBoard board : messageBoards) {
                    if (board.getName().equals(userInput)) {
                        messageBoard = board;
                    }
                }

                // If no message board is found tell user to retry, else client joins message
                // board
                if (messageBoard == null) {
                    out.println("Message board " + userInput + " does not exist, enter a new name");
                } else {
                    this.currentBoard = messageBoard;
                    messageBoard.addClient(this);

                    // Displays the messages posted on the board to the client
                    ArrayList<String> boardMessages = messageBoard.getMessages();
                    for (String message : boardMessages) {
                        out.println(message);
                    }
                    return;
                }
            }

            // Handling if the user is already in a message board or there are no message
            // boards created
        } else if (Server.getMessageBoards().size() > 0 && this.currentBoard != null) {
            out.println("You cannot join two message boards at once, please leave the current message board");
        } else {
            out.println("There are no message boards to join.");
        }
        return;

    }

    public void leaveMessageBoard() throws IOException {
        if (this.currentBoard != null) {
            currentBoard.removeClient(this);
            String boardName = currentBoard.getName();
            this.currentBoard = null;
            out.println("You have left the message board: " + boardName);
        } else {
            out.println("You are not currently in a message board");
        }
    }

    @Override
    public void run() {
        try {
            // Establish the in / out streams
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Let user set their name
            if (this.username == null) {
                setUsername();
            }

            out.println("Welcome to the Message Boards!");
            out.println("Here is a list of commands offered." + Server.getServerCommands());

            // Reads the user input
            String userInput;
            while ((userInput = in.readLine()) != null) {

                // Displays a list of commands to the user
                if (userInput.equals("/help")) {
                    out.println(Server.getServerCommands());

                    // Displays each message board name for the user
                } else if (userInput.equals("/MSGBoards")) {
                    ArrayList<messageBoard> messageBoards = Server.getMessageBoards();
                    if (messageBoards.size() > 0) {
                        for (messageBoard messageBoard : Server.getMessageBoards()) {
                            out.println(messageBoard.getName());
                        }
                    } else {
                        out.println("No message boards currently, please create one :)");
                    }

                    // Creates a message board with the user
                } else if (userInput.equals("/create")) {
                    createMessageBoard();

                    // Joins a message board
                } else if (userInput.equals("/join")) {
                    if (Server.getMessageBoards().size() > 0) {
                        joinMessageBoard();
                    } else {
                        out.println("No message boards available");
                    }

                    // Leaves a message board
                } else if (userInput.equals("/leave")) {
                    leaveMessageBoard();

                    // A message is posted to a board
                } else {
                    // Check if user is in a message board
                    if (this.currentBoard == null) {
                        out.println("Please join a message board");

                        // If user is in a message board post the message
                    } else {
                        userInput = "[" + Server.getCurrentServerTime() + "] " + this.username + ": " + userInput;
                        this.currentBoard.addMessage(userInput);

                        // Write message to message board .txt files
                        try (BufferedWriter writer = new BufferedWriter(
                                new FileWriter("messageBoards/" + this.currentBoard.getName() + ".txt", true))) {
                            writer.write(userInput);
                            writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            // When a user terminates it will dissconnect them from connected users
        } catch (IOException e) {
            System.out.println("User " + this.username + " has disconnected from server");

        }
    }
}

class messageBoard {
    // Create array list to hold messages on board and clients connected to message
    // boards
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<clientThread> connectedClientThreads = new ArrayList<>();
    private String name;

    public messageBoard(String name) {
        this.name = name;
    }

    // Return chat board name
    public String getName() {
        return this.name;
    }

    // Adds message to chatboard array list
    public void addMessage(String message) {
        messages.add(message);
        Server.sendMessageToAll(message, this);
    }

    // Returns all messages in chat board
    public ArrayList<String> getMessages() {
        return this.messages;
    }

    // Returns arrayList of connected clients
    public ArrayList<clientThread> getClients() {
        return this.connectedClientThreads;
    }

    // Adds a client to the message board
    public void addClient(clientThread client) {
        connectedClientThreads.add(client);
    }

    // Removes a client from the message board
    public void removeClient(clientThread client) {
        connectedClientThreads.remove(client);
    }
}
