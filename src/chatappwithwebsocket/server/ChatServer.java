/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatappwithwebsocket.server;

/*
  @author danie
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer implements Runnable {

    private final ArrayList<Connector> usersConnected;
    private ServerSocket serverSocket;

    private boolean finished;
    private boolean isLocked = false;
    private ExecutorService threadpool;

    public ChatServer() {
        usersConnected = new ArrayList<>();
        finished = false;
    }

    @Override
    public void run() {

        try {
            serverSocket = new ServerSocket(9000);
            threadpool = Executors.newCachedThreadPool();
            System.out.println("Server is ON and waiting for connections");
            while (!finished) {
                Socket client = serverSocket.accept();
                if (isLocked) {
                    PrintWriter output = new PrintWriter(client.getOutputStream(), true);
                    output.println("Chatroom has been locked by a user, try again later");
                    client.close();
                } else {
                    Connector connector = new Connector(client);
                    usersConnected.add(connector);
                    threadpool.execute(connector);

                }
            }
        } catch (IOException e) {
            System.out.println("Server is turning OFF");
            finishServer();
        }


    }

    public void broadcastMessage(String message) {
        synchronized (usersConnected) {
        for (Connector connector : usersConnected) {
            if (connector != null) {
                connector.send(message);
            }
        }
        }
    }


    //here we close the connector that we had with the client
    public void finishServer() {
        finished = true;
        threadpool.shutdown();
        synchronized (usersConnected) {
            try {
                if (!serverSocket.isClosed()) {
                    serverSocket.close();
                }
                for (Connector connector : usersConnected) {
                    connector.finish();
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }


    class Connector implements Runnable {
        private final Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;

        public Connector(Socket clientSocket) {

            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {

            try {

                output = new PrintWriter(clientSocket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output.println("Please enter a Username:");
                username = input.readLine();
                while (username.isEmpty()) {
                    output.println("Please enter a valid username");
                    username = input.readLine();
                }
                broadcastMessage(username + " Joined the Server");
                output.println(printHelpMenu());
                //so it prints on the console
                System.out.println(username + " Joined the Server");
                String message;

                //when message is not null
                while ((message = input.readLine()) != null) {
                    //checks if user wants to change username
                    if (message.startsWith("/username")) {
                        String[] msgSplit = message.split(" ");
                        if (msgSplit.length == 2) {
                            System.out.println(username + " changed their username to " + msgSplit[1]);
                            broadcastMessage(username + " changed their username to " + msgSplit[1]);
                            username = msgSplit[1];
                            output.println("You Changed your Username to " + username);
                        }
                        //condition reach when there's an error when trying to change the name
                        else {
                            output.println("Your username was not changed");
                            output.println("It must contain only one word");
                            output.println("Make sure you enter -> /username [new username]");
                            output.println("Where you see [new username] you can enter your new username");
                        }
                    }
                    //checks if user is trying to leave chat
                    else if (message.startsWith("/leave")) {
                        broadcastMessage(username + " has left the Chat");
                        System.out.println(username + " has left the server");
                        usersConnected.remove(this);
                        finish();
                    } else if (message.startsWith("/active")) {
                        output.println(printConnectedUsers());
                    } else if (message.startsWith("/lock")) {
                        lockServer();
                    } else if (message.startsWith("/unlock")) {
                        unlockServer();
                    } else if (message.startsWith("/help")) {
                        output.println(printHelpMenu());

                    } else {
                        //it's just a normal message, so server will send to everyone in the chat
                        broadcastMessage(username + ": " + message);

                    }
                }

            } catch (IOException e) {
                //if any errors occur will try to close the server
                finish();
            }


        }

        //used inside broadcast message which is a for loop that will send the message to all users connected
        public void send(String message) {
            output.println(message);
        }

        public synchronized void lockServer(){
         isLocked = true;
         broadcastMessage("Server is now CLOSED for new connections!");
        }
        public synchronized void unlockServer(){
            isLocked = false;
            broadcastMessage("Server is now OPEN for new connections!");
        }



        //closes the server when there is an error, otherwise the server will stay open
        public void finish() {

            try {
                input.close();
                output.close();
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("Server Closed");
            }


        }

        public synchronized String printConnectedUsers() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("##########################\n");
            stringBuilder.append("List of Active Users:\n");
            for (Connector connector : usersConnected) {
                stringBuilder.append(connector.username).append("\n");
            }
            stringBuilder.append("##########################\n");
            return stringBuilder.toString();
        }

        public synchronized String printHelpMenu() {
            String string;
            string = "##########################\n" +
                    "Help Menu:\n" +
                    "Welcome to the Chat Room" + "\n" +
                    "Available Commands:" + "\n" +
                    "/username [new username] -> In order to change your Username" + "\n" +
                    "/active -> In order to see who's connected to the server" + "\n" +
                    "/lock -> In order to block new connections to the server" + "\n" +
                    "/unlock -> In order to allow new connections in case server was locked" + "\n" +
                    "/leave -> In order to leave the server" + "\n" +
                    "/help -> In order to reopen this menu to see the available commands" + "\n" +
                    "##########################\n";
            return string;
        }


    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.run();
    }

}
