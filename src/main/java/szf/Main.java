package szf;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor.ANSI;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import org.jline.utils.Levenshtein;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;

public class Main {

  public static final boolean INSERT = false, COMMAND = true;

  public static void main(final String[] args) throws IOException {
    StringBuilder standardInput = new StringBuilder();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    String nextLine;

    while((nextLine = bufferedReader.readLine()) != null) {
      standardInput.append(nextLine);
    }

    final String[] input = standardInput.toString().split(" ");

    final DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    final Terminal terminal = defaultTerminalFactory.createTerminal();

    final TerminalPosition initial = terminal.getCursorPosition();
    final TextGraphics textGraphics = terminal.newTextGraphics();

    textGraphics.setCharacter(initial, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
    textGraphics.setCharacter(initial.withRelativeRow(1), new TextCharacter('>', ANSI.RED, ANSI.DEFAULT, SGR.BOLD));
    terminal.flush();

    updateWord(terminal, textGraphics, initial, "");
    updateList(terminal, textGraphics, initial, "", input);

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
        if (newInput.length != 0) {
          textGraphics.setForegroundColor(ANSI.DEFAULT);
          textGraphics.fillRectangle(initial, terminal.getTerminalSize(), ' ');
          terminal.setCursorPosition(initial);
          System.out.println(newInput[0]);
        }

        terminal.close();
        return;
      } else if (keyType == KeyType.Backspace) {
        if (mode == INSERT) {
          if (terminal.getCursorPosition().getColumn() != 2) {
            TerminalPosition tempPosition = terminal.getCursorPosition();
            textGraphics.setCharacter(initial.withRelativeColumn(word.length() + 1), ' ');
            word.deleteCharAt(tempPosition.getColumn() - 3);
            updateWord(terminal ,textGraphics, initial, word.toString());
            newInput = updateList(terminal, textGraphics, initial, word.toString(), input);
            terminal.setCursorPosition(tempPosition.withRelativeColumn(-1));
            terminal.flush();
          }
        }
      } else if (keyType == KeyType.Character) {
        final Character character = keyStroke.getCharacter();

        if (mode == COMMAND) {
          switch (character) {
            case 'q' -> {
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
          TerminalPosition tempPosition = terminal.getCursorPosition();
          word.insert(tempPosition.getColumn() - 2, character);
          updateWord(terminal, textGraphics, initial, word.toString());
          newInput = updateList(terminal, textGraphics, initial, word.toString(), input);
          terminal.setCursorPosition(tempPosition.withRelativeColumn(1));
          terminal.flush();
        }
      }
    }
  }

  public static void updateWord(final Terminal terminal, final TextGraphics textGraphics, final TerminalPosition initial, final String word)
          throws IOException {
    textGraphics.setForegroundColor(ANSI.BLUE);
    textGraphics.putString(initial.withRelativeColumn(2), word);
    textGraphics.setForegroundColor(ANSI.DEFAULT);
    terminal.flush();
  }

  public static String[] updateList(final Terminal terminal, final TextGraphics textGraphics, final TerminalPosition initial, final String word,
                                    final String[] input) throws IOException {
    final String[] newInput = Arrays.stream(input).filter((string) -> string.length() >= word.length())
            .sorted(Comparator.comparingInt(string -> Levenshtein.distance(word, string))).toArray(String[]::new);

    textGraphics.setForegroundColor(ANSI.RED);

    int initialRow = initial.getRow();
    int columns = terminal.getTerminalSize().getColumns();

    for (int i = initialRow + 1; i < terminal.getTerminalSize().getRows() && i - (initialRow + 1) < newInput.length; i++) {
      String curr = String.format("%-" + columns + "s", newInput[i - (initialRow + 1)]);
      textGraphics.putString(2, i, curr);
    }

    terminal.setForegroundColor(ANSI.DEFAULT);
    terminal.flush();
    return newInput;
  }

  /**
   * TODO Work in progress
   * <p>
   * Finds the shortest substring of a dictionary word
   * containing the most amount of letters from the input string.
   * <p>
   * Inspired by the algorithm from <a href="https://github.com/junegunn/fzf/blob/master/src/algo/algo.go#L5">fzf</a>.
   * Given affffffbffabjkc as a dictionary word and abc as the input word
   * <p>
   * Scan 1:
   * <p>
   * <pre>
   *     {@code
   * affffffbffabjkc
   * a      b  c
   *     }
   *   </pre>
   * <p>
   * Scan 2:
   * <p>
   * <pre>
   *     {@code
   * affffffbffabjkc
   *       ab  c
   *     }
   *   </pre>
   * <p>
   * Each scan moves to the next position and starts scanning.
   * Scan 2 is more successful (smaller string), so output is "abjkc".
   */

  @SuppressWarnings("all")
  public static String findSubstring(String inputString, String dictionaryWord) {
    final String forwards = forwards(dictionaryWord, inputString, 0, 0);
    final String backwards = backwards(dictionaryWord, inputString, dictionaryWord.length() - 1, inputString.length() - 1);

    if (forwards.length() > backwards.length()) {
      return forwards;
    } else {
      return backwards;
    }
  }

  @SuppressWarnings({"SameParameterValue", "SameReturnValue", "unused"})
  public static String forwards(final String inputString, final String dictionaryWord, final int i, final int j) {
    return null;
  }

  @SuppressWarnings({"SameParameterValue", "SameReturnValue", "unused"})
  public static String backwards(final String inputString, final String dictionaryWord, final int i, final int j) {
    return null;
  }

}