package se.kth.ockel.id1212.hw5.controller;

import se.kth.ockel.id1212.hw5.model.Game;
import se.kth.ockel.id1212.hw5.model.GameCommand;
import se.kth.ockel.id1212.hw5.model.ParsedCommand;
import se.kth.ockel.id1212.hw5.net.ClientHandler;
import se.kth.ockel.id1212.hw5.shared.GameActionFeedback;
import se.kth.ockel.id1212.hw5.shared.GameInfo;
import se.kth.ockel.id1212.hw5.shared.GameState;
import se.kth.ockel.id1212.hw5.exceptions.AlreadyGuessedException;
import se.kth.ockel.id1212.hw5.exceptions.OngoingGameException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This class controls the flow of the game, and what should be sent to the client.
 */
public class GameController {

    private Map<Long, Game> games;

    /**
     * Creates a new game se.kth.emibra.id1212.hw1.controller
     */
    public GameController() {
        this.games = new HashMap<>();
    }

    /**
     * Makes a word or a letter guess in the hangman game.
     * @param guess The word or letter to guess.
     * @return The the <code>GameActionFeedback</code> which indicates the status to the client.
     */
    private GameActionFeedback makeGuess(String guess) {
        Game game;
        synchronized (this) {
            game = this.games.get(Thread.currentThread().getId());
        }
        try {
            if (game.getGameState() == GameState.GAME_LOST || game.getGameState() == GameState.GAME_WON)
                return GameActionFeedback.NO_GAME_STARTED;
            else
                game.makeGuess(guess);
        } catch (AlreadyGuessedException e) {
            return GameActionFeedback.DUPLICATE_GUESS;
        }
        if (game.getGameState() == GameState.GAME_LOST) {
            return GameActionFeedback.GAME_LOST;
        } else if (game.getGameState() == GameState.GAME_WON) {
            return GameActionFeedback.GAME_WON;
        } else {
            return GameActionFeedback.GAME_INFO;
        }
    }

    /**
     * Starts a game session for the player.
     * If a game is already ongoing an exception will be thrown
     * @throws OngoingGameException Exception indicating that a game is already ongoing, and no new game will
     * be started.
     */
    private void startGame() throws OngoingGameException {
        long threadID = Thread.currentThread().getId();
        synchronized (this) {
            if (this.games.containsKey(threadID)) {
                if (this.games.get(threadID).getGameState() == GameState.GAME_ONGOING) {
                    throw new OngoingGameException("There is alreadya a game ongoing");
                } else {
                    this.games.put(threadID, new Game(this.games.get(threadID)));
                }
            } else {
                this.games.put(threadID, new Game());
            }
        }
    }

    /**
     * Restarts the game, which results in -1 points. Or starts a new game if there's no ongoing game.
     */
    private void restartGame() {
        long threadID = Thread.currentThread().getId();
        synchronized (this) {
            if (this.games.containsKey(threadID)) {
                Game oldGame = this.games.get(threadID);
                if (oldGame.getGameState() == GameState.GAME_ONGOING) {
                    oldGame.concede();
                }
                this.games.put(threadID, new Game(oldGame));
            } else {
                try {
                    startGame();
                } catch (OngoingGameException e) {
                    e.printStackTrace(); // Won't happen though
                }
            }
        }
    }

    /**
     * Returns the game info of the game.
     * @return A <code>GameInfo</code> object that indicates the current state of the game.
     */
    public GameInfo getGameInfo() {
        Game game;
        synchronized (this) {
            game = this.games.get(Thread.currentThread().getId());
        }
        return game.getGameInfo();
    }


    /**
     * Parses the command and performs the requested action.
     * @param command The raw command from the client.
     * @param clientHandler The client handler to use for sending back responses.
     */
    public void parseCommand(String command, ClientHandler clientHandler) {
        ParsedCommand parsedCommand = getParsedCommand(command);
        GameCommand gc = parsedCommand.getGameCommand();

        // Take action on the command
        switch (gc) {
            case START_GAME:
                try {
                    startGame();
                    clientHandler.writeToClient(getGameInfo());
                } catch (OngoingGameException e) {
                    clientHandler.writeToClient(GameActionFeedback.GAME_ONGOING);
                    e.printStackTrace();
                }
                break;
            case RESTART:
                restartGame();
                //TODO: Reveal the word
                String finalWord;
                synchronized (this) {
                    finalWord = games.get(Thread.currentThread().getId()).getWord();
                }
                clientHandler.writeToClient(GameActionFeedback.GAME_RESTARTED);
                clientHandler.writeToClient(getGameInfo());
                break;
            case MAKE_GUESS:
                GameActionFeedback gaf = makeGuess(parsedCommand.getArguments()[0]);
                clientHandler.writeToClient(getGameInfo());
                clientHandler.writeToClient(gaf);
                break;
            case FETCH_INFO:
                // The boolean is introduced to avoid deadlocking since getGameInfo uses "this" as lock as well.
                boolean gameOngoing = false;
                synchronized (this) {
                    if (this.games.get(Thread.currentThread().getId()) == null) {
                        clientHandler.writeToClient(GameActionFeedback.NO_GAME_STARTED);
                    } else {
                        gameOngoing = true;
                    }
                }
                if (gameOngoing)
                    clientHandler.writeToClient(getGameInfo());
                break;
            case EXIT:
                long threadID = Thread.currentThread().getId();
                System.out.println("Quitting job(thread) #" + threadID);
                synchronized (this) {
                    this.games.remove(threadID);
                }
                clientHandler.disconnect();
                break;
            case INVALID_COMMAND:
                System.out.println("Invalid command received");
                clientHandler.writeToClient(GameActionFeedback.INVALID_COMMAND);
                break;
            default:
                System.out.println("This should never happen, but happened.");
                break;
        }
    }

    /**
     * Parses the a raw String sent to the server and creates a ParsedCommand which may be
     * used for performing the requested actions.
     * @param command The raw string command.
     * @return A ParsedCommand object which includes the Command and any arguments.
     */
    private ParsedCommand getParsedCommand(String command) {
        String[] commandsArray = command.split(" ");
        String[] arguments = null;
        GameCommand gc;
        if (commandsArray.length == 1 && commandsArray[0].length() == 1) {
            gc = GameCommand.MAKE_GUESS;
            arguments = new String[]{commandsArray[0]};
        } else if (commandsArray[0].equalsIgnoreCase("start")) {
            gc = GameCommand.START_GAME;
        } else if (commandsArray[0].equalsIgnoreCase("guess")) {
            gc = GameCommand.MAKE_GUESS;
            arguments = Arrays.copyOfRange(commandsArray, 1, commandsArray.length);
        } else if (commandsArray[0].equalsIgnoreCase("exit")) {
            gc = GameCommand.EXIT;
        } else if (commandsArray[0].equalsIgnoreCase("info")) {
            gc = GameCommand.FETCH_INFO;
        } else if(commandsArray[0].equalsIgnoreCase("restart")) {
            gc = GameCommand.RESTART;
        } else {
            gc = GameCommand.INVALID_COMMAND;
        }
        return new ParsedCommand(gc, arguments);
    }

}
