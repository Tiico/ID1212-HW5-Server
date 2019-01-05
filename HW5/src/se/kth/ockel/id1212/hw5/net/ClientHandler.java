package se.kth.ockel.id1212.hw5.net;

import se.kth.ockel.id1212.hw5.controller.GameController;
import se.kth.ockel.id1212.hw5.shared.GameActionFeedback;

import java.io.*;
import java.net.Socket;
import java.util.Date;

/**
 * This is the main class for handling a single persistent connection.
 */
public class ClientHandler implements Runnable {

    private volatile Socket socket;
    private GameController gameController;
    private volatile boolean connected = true;
    private BufferedReader reader;
    private BufferedWriter writer;

    /**
     * Creates a client handler that orchestrates the communication network communication with the client against
     * a GameController.
     * @param socket The socket onto which the client has connected to.
     * @param gameController The <code>GameController</code> that orchestrates the game.
     * @throws IOException If the communication with the client fails, this exception is thrown.
     */
    public ClientHandler(Socket socket, GameController gameController) throws IOException {
        this.gameController = gameController;
        this.socket = socket;
        this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        System.out.printf("I got a task (%s)\n", socket.toString());
    }

    /**
     * Reads a line (to a line break) if input from the client.
     * @return Returns a String containing the client input.
     * @throws IOException If the communication with the client fails, this exception is thrown.
     */
    private String readFromClient() throws IOException {
        return this.reader.readLine();
    }

    /**
     * Writes an object to the client containing game information. If the server can't write to the client
     * it disconnects.
     * @param objectToWrite The object to send the client. Must be <code>GameActionFeedback</code>
     *                      or <code>GameInfo</code>
     * @throws IOException
     */
    public void writeToClient(Object objectToWrite) {
        if (objectToWrite == null){
            System.err.println("ERROR: SENDING NULL");
        }else {
            System.out.println("Sending: " + objectToWrite);
        }

        try {
            writer.write(objectToWrite.toString() + "\n");
            writer.flush();
            System.out.println("Done writing to client");
        } catch (IOException e) {
            disconnect();
            e.printStackTrace();
        }

    }

    /**
     * Disconnects the client.
     */
    public void disconnect() {
        this.connected = false;
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.printf("Connection to client %d ended...\n", Thread.currentThread().getId());
        }
    }

    /**
     * Continuously reads from the client and passes what's read to the se.kth.emibra.id1212.hw1.controller.
     */
    @Override
    public void run() {
        /*String object = "hello from server";
        writeToClient(object);*/
        writeToClient(GameActionFeedback.HELP);
        while (connected) {
            String inputString;
            try {
                inputString = readFromClient();
                System.out.println("inputstring: " + inputString);
                // The client sends a null byte when it disconnects.
                if (inputString == null) {
                    disconnect();
                    break;
                }
                gameController.parseCommand(inputString, this);
            } catch (IOException e) {
                e.printStackTrace();
                disconnect();
            }
        }
    }
}
