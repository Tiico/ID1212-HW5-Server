package se.kth.ockel.id1212.hw5.shared;

import java.io.Serializable;

/**
 * A class containing the public information of a hangman game at a point in time.
 */
public class GameInfo implements Serializable {

    private GameState gameState;
    private char[] wordProgress;
    private char[] charsGuessed;
    private int remainingAttempts;
    private int score;
    private String secretWord;

    /**
     * Creates a public knowledge snapshot of the hangman game.
     * @param wordProgress The word-guessing progress.
     * @param remainingAttempts The remaining guess attempts.
     * @param charsGuessed The wrongly guessed characters.
     * @param score The session score.
     * @param word The actual word, revealed
     * @param gs The game-state, such as Ongoing, Won or Lost.
     */
    public GameInfo(char[] wordProgress, int remainingAttempts, char[] charsGuessed, int score, String word, GameState gs) {
        this.secretWord = word;
        this.wordProgress = wordProgress;
        this.charsGuessed = charsGuessed;
        this.remainingAttempts = remainingAttempts;
        this.score = score;
        this.gameState = gs;
    }

    public char[] getWordProgress() {
        return this.wordProgress;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public int getScore() {
        return score;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public GameState getGameState() {
        return gameState;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(gameState);
        sb.append("|");

        for (char c : wordProgress) {
            if (c == '\u0000')
                sb.append("_ ");
            else
                sb.append(c + " ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("|");

        if (charsGuessed.length == 0){
            sb.append("_");
        }else {
            for (char c : charsGuessed){
                sb.append(c);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length()-1);
        }
        sb.append("|");

        sb.append(remainingAttempts);
        sb.append("|");
        sb.append(score);
        sb.append("|");

        if (gameState != gameState.GAME_ONGOING)
            sb.append(secretWord);

        return sb.toString();
    }
}
