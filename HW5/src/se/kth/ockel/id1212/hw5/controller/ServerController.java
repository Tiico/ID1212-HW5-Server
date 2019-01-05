package se.kth.ockel.id1212.hw5.controller;

import se.kth.ockel.id1212.hw5.net.ConnectionHandler;

/**
 * Creates a Game Controller to orchestrate the game and a connection handler to accept client connections.
 */
public class ServerController {

    private GameController gameController;

    /**
     * Creates a ServerController with the specified port to listen to.
     * @param serverPort The port to listen to.
     */
    public ServerController(int serverPort) {
        // Create the game se.kth.emibra.id1212.hw1.controller for the application
        this.gameController = new GameController();
        // Start the main connection listener/accepter
        new ConnectionHandler(serverPort, this.gameController);
    }

}
