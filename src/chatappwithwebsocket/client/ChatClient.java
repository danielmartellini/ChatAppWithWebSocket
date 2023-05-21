/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatappwithwebsocket.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient implements Runnable {
    private Socket clientSocket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean finished = false;


    @Override
    public void run() {
        try {
            clientSocket = new Socket("127.0.0.1", 9000);
            output = new PrintWriter(clientSocket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            InputAndOutputHandler inputAndOutputHandler = new InputAndOutputHandler();
            Thread thread = new Thread(inputAndOutputHandler);
            thread.start();
    //keeps checking for new message
            String inputMessage;
            while ((inputMessage = input.readLine()) != null) {
                System.out.println(inputMessage);

            }
        } catch (IOException e) {
            //this way this only prints when the server has been turned off
            if (!finished){
            System.out.println("your server might be off");
            System.out.println("make sure you first turn on your server");
            System.out.println("then relaunch client");}
            finish();

        }

    }

    //here we close the input and output handler with the client socket and the whole client side
    public void finish() {
        finished = true;
        try {
            //checking if connections are not null before attempting to close them
            if(input !=null)
                input.close();
            if(output != null)
                output.close();
            if (clientSocket != null && !clientSocket.isClosed() ) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("ERROR closing the connection");
        }
    }
//after some changes only handles client input while the other class is handling server output
    //so we can receive and send messages at the same time

   private class InputAndOutputHandler implements Runnable{

        @Override
        public void run() {
            try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

            while (!finished) {
                String message = inputReader.readLine();
                if(message.startsWith("/leave")){
                    //sending the leave message to the server
                    //the server won't broadcast the /leave command, it will just finish connection with this client
                    output.println(message);
                    inputReader.close();
                    finish();
                }
                else {
                    //if message is not empty will send message to server
                    if (!message.isEmpty()){
                    output.println(message);}
                }
            }
                } catch (IOException e) {
                    finish();
                }

            }

        }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.run();
    }}



