package szf;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor.ANSI;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The main class containing the entry point.
 */
public class Szf {

  /**
   * Constant to signify insert mode.
   */
  public static final boolean INSERT = false;

  /**
   * Constant to signify command mode.
   */
  public static final boolean COMMAND = true;

  /**
   * Official entry point.
   *
   * @param args The command line arguments representing what items
   *             are to be searched through. The items are a list of
   *             words separated by a single space.
   * @throws IOException When there is an error with the terminal emulator
   */
  public static void main(final String[] args) throws IOException {
    final String[] input = Arrays.stream(args)
            .map((string) -> string.replaceAll("\u001B\\[[;\\d]*m", ""))
            .toArray(String[]::new);

    final DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    final Terminal terminal = defaultTerminalFactory.createTerminal();

    final TerminalPosition initial = terminal.getCursorPosition();
    final TextGraphics textGraphics = terminal.newTextGraphics();
    final TerminalSize terminalSize = terminal.getTerminalSize();

    final int rows = terminalSize.getRows() - initial.getRow() - 1;

    if (initial.getRow() + 1 >= terminalSize.getRows()) {
      System.out.println("Not Enough Rows To Run");
      return;
    }

    textGraphics.setCharacter(initial, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
    textGraphics.setCharacter(initial.withRelativeRow(1), new TextCharacter('>', ANSI.RED, ANSI.DEFAULT, SGR.BOLD));
    terminal.flush();

    updateWord(terminal, textGraphics, initial, "");
    updateList(terminal, textGraphics, initial, terminalSize, "", input, rows);

    terminal.setCursorPosition(initial.withRelativeColumn(2));
    terminal.flush();

    String[] newInput = input;
    final StringBuilder word = new StringBuilder();
    boolean mode = INSERT;

    while (true) {
      final KeyStroke keyStroke = terminal.readInput();
      final KeyType keyType = keyStroke.getKeyType();

      if (keyType == KeyType.Escape) {
        if (mode == INSERT && terminal.getCursorPosition().getColumn() != 2) {
          terminal.setCursorPosition(terminal.getCursorPosition().withRelativeColumn(-1));
          terminal.flush();
        }

        mode = COMMAND;
      } else if (keyType == KeyType.Enter) {
        textGraphics.setForegroundColor(ANSI.DEFAULT);
        textGraphics.fillRectangle(initial, terminalSize, ' ');
        terminal.setCursorPosition(initial);

        if (newInput.length != 0) {
          System.out.println(newInput[0]);
        }

        terminal.flush();
        terminal.close();
        return;
      } else if (keyType == KeyType.Backspace) {
        if (mode == INSERT) {
          if (terminal.getCursorPosition().getColumn() != 2) {
            final TerminalPosition tempPosition = terminal.getCursorPosition();
            textGraphics.setCharacter(initial.withRelativeColumn(word.length() + 1), ' ');
            word.deleteCharAt(tempPosition.getColumn() - 3);
            updateWord(terminal, textGraphics, initial, word.toString());
            newInput = updateList(terminal, textGraphics, initial, terminalSize, word.toString(), input, rows);
            terminal.setCursorPosition(tempPosition.withRelativeColumn(-1));
            terminal.flush();
          }
        }
      } else if (keyType == KeyType.Character) {
        final Character character = keyStroke.getCharacter();

        if (mode == COMMAND) {
          switch (character) {
            case 'q' -> {
              textGraphics.setForegroundColor(ANSI.DEFAULT);
              textGraphics.fillRectangle(initial, terminalSize, ' ');
              terminal.setCursorPosition(initial);
              terminal.flush();
              terminal.close();
              return;
            }
            case 'i' -> mode = INSERT;
            case 'a' -> {
              mode = INSERT;
              if (terminal.getCursorPosition().getColumn() != word.length() + 2) {
                terminal.setCursorPosition(terminal.getCursorPosition().withRelativeColumn(1));
                terminal.flush();
              }
            }
            case 'h' -> {
              if (terminal.getCursorPosition().getColumn() != 2) {
                terminal.setCursorPosition(terminal.getCursorPosition().withRelativeColumn(-1));
                terminal.flush();
              }
            }
            case 'l' -> {
              if (terminal.getCursorPosition().getColumn() != word.length() + 1) {
                terminal.setCursorPosition(terminal.getCursorPosition().withRelativeColumn(1));
                terminal.flush();
              }
            }
          }
        } else {
          final TerminalPosition tempPosition = terminal.getCursorPosition();
          word.insert(tempPosition.getColumn() - 2, character);
          updateWord(terminal, textGraphics, initial, word.toString());
          newInput = updateList(terminal, textGraphics, initial, terminalSize, word.toString(), input, rows);
          terminal.setCursorPosition(tempPosition.withRelativeColumn(1));
          terminal.flush();
        }
      }
    }
  }

  /**
   * Updates the word that is being typed by the user. This method
   * is called whenever the user makes a change to his input.
   *
   * @param terminal     The {@link Terminal} object that is being used.
   * @param textGraphics An instance of a {@link TextGraphics} object.
   * @param initial      The initial position this program is run from in the terminal
   *                     emulator.
   * @param word         The updated word that the user typed.
   * @throws IOException When there is an error with the terminal emulator.
   */
  public static void updateWord(final Terminal terminal, final TextGraphics textGraphics,
                                final TerminalPosition initial, final String word) throws IOException {
    textGraphics.setForegroundColor(ANSI.BLUE);
    textGraphics.putString(initial.withRelativeColumn(2), word);
    textGraphics.setForegroundColor(ANSI.DEFAULT);
    terminal.flush();
  }

  /**
   * Updates the list of all items. When the input word is updated, the
   * the list is updated to match the query.
   *
   * @param terminal     The {@link Terminal} object being used.
   * @param textGraphics An instance of a {@link TextGraphics} object.
   * @param initial      The initial position this program is run from in the terminal
   *                     emulator.
   * @param terminalSize The size of the terminal.
   * @param word         The updated word that the user typed.
   * @param input        The list of all items currently showed.
   * @param rows         The amount of rows you can fill in with input words.
   * @return The new, updated list of all items currently showed.
   * @throws IOException When there is an error with the terminal emulator.
   */
  public static String[] updateList(final Terminal terminal, final TextGraphics textGraphics,
                                    final TerminalPosition initial, final TerminalSize terminalSize,
                                    final String word, final String[] input, final int rows) throws IOException {
    final String[] newInput = Arrays.stream(input).filter((string) -> string.length() >= word.length())
            .sorted(Comparator.comparingInt(string -> levenshtein(word, string))).toArray(String[]::new);
    textGraphics.setForegroundColor(ANSI.RED);

    final int initialRow = initial.getRow();
    final int columns = terminalSize.getColumns();

    for (int i = initialRow + 1; i < terminalSize.getRows()
            && i - (initialRow + 1) < newInput.length
            && i < (initialRow + 1) + rows - 1; i++) {

      textGraphics.putString(2, i, String.format("%-" + columns + "s", newInput[i - (initialRow + 1)]));
    }

    terminal.setForegroundColor(ANSI.DEFAULT);
    terminal.flush();
    return newInput;
  }

  /**
   * Calculates the <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">levenshtein distance</a>
   * between two strings.
   *
   * @param word1String The first word.
   * @param word2String The second word.
   * @return The levenshtein distance between both words.
   */
  public static int levenshtein(final String word1String, final String word2String) {
    final char[] word1 = word1String.toCharArray();
    final char[] word2 = word2String.toCharArray();
    final int[][] dp = new int[word1.length + 1][word2.length + 1];

    for (int i = 1; i < dp.length; i++) {
      dp[i][0] = i;
    }

    for (int i = 1; i < dp[0].length; i++) {
      dp[0][i] = i;
    }

    for (int i = 1; i < dp.length; i++) {
      for (int j = 1; j < dp[i].length; j++) {
        if (word1[i - 1] == word2[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1];
        } else {
          dp[i][j] = 1 + dp[i][j - 1];
        }
      }
    }

    return dp[dp.length - 1][dp[0].length - 1];
  }

  /**
   * TODO Work in progress
   * <p>
   * Finds the shortest substring of a dictionary word containing the most amount
   * of letters from the input string.
   * <p>
   * Inspired by the algorithm from <a href=
   * "https://github.com/junegunn/fzf/blob/master/src/algo/algo.go#L5">fzf</a>.
   * Given affffffbffabjkc as a dictionary word and abc as the input word
   * <p>
   * Scan 1:
   *
   * <pre>
   *     {@code
   * affffffbffabjkc
   * a      b  c
   *     }
   * </pre>
   * <p>
   * Scan 2:
   *
   * <pre>
   *     {@code
   * affffffbffabjkc
   *       ab  c
   *     }
   * </pre>
   * <p>
   * Each scan moves to the next position and starts scanning. Scan 2 is more
   * successful (smaller string), so output is "abjkc".
   *
   * @param inputString    The string entered by the user.
   * @param dictionaryWord The word in the dictionary that this method is trying
   *                       to find the substring of.
   * @return The smallest substring of dictionaryWord that contains the most letters from
   * inputString.
   */

  @SuppressWarnings({"SameReturnValue", "unused"})
  public static String findSubstring(final String inputString, final String dictionaryWord) {
    return "";
  }

}
