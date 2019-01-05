package se.kth.ockel.id1212.hw5.starter;

import se.kth.ockel.id1212.hw5.controller.ServerController;

/**
 * Starts up the server.
 */
public class ServerApp {

    private static final int SERVER_PORT = 54321;

    /**
     * Starts the server on specified port.x
     * @param args
     */
    public static void main(String[] args) {
        new ServerController(SERVER_PORT);
    }

}