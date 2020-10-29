package szf;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor.ANSI;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TabBehaviour;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import org.apache.commons.lang.math.NumberUtils;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class Main {

  @Option(names = { "-i",
      "--input" }, description = "The Input Text, Separated By New Line Characters", required = true)
  private String input;

  public static final boolean INSERT = false, COMMAND = true;

  public static void main(final String[] args) throws IOException {
    final Main main = new Main();
    new CommandLine(main).parseArgs(args);

    final String[] input = main.input.split("\n");

    final DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    final Screen screen = new TerminalScreen(defaultTerminalFactory.createTerminal());

    screen.startScreen();
    screen.setCursorPosition(new TerminalPosition(2, 0));

    screen.refresh();

    final TextGraphics textGraphics = screen.newTextGraphics().setTabBehaviour(TabBehaviour.CONVERT_TO_FOUR_SPACES);

    updateWord(textGraphics, "");
    updateList(textGraphics, input, screen, "");

    screen.refresh();

    String[] newInput = input;
    final StringBuilder word = new StringBuilder();
    boolean mode = INSERT;

    while (true) {
      final KeyStroke keyStroke = screen.readInput();
      final KeyType keyType = keyStroke.getKeyType();

      if (keyType == KeyType.Escape) {
        if (mode == INSERT && screen.getCursorPosition().getColumn() != 2) {
          screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(-1));
          screen.refresh();
        }

        mode = COMMAND;
      } else if (keyType == KeyType.Enter) {
        screen.close();

        if (newInput.length != 0) {
          System.out.println(newInput[0]);
        }

        return;
      } else if (keyType == KeyType.Backspace) {
        if (mode == INSERT) {
          if (word.length() > 0) {
            screen.setCharacter(word.length() + 1, 0, new TextCharacter(' ', ANSI.DEFAULT, ANSI.DEFAULT));
            word.deleteCharAt(word.length() - 1);
            screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(-1));
            screen.refresh();
            newInput = updateList(textGraphics, input, screen, word.toString());
          }
        }
      } else if (keyType == KeyType.Character) {
        final Character character = keyStroke.getCharacter();

        if (mode == COMMAND) {
          switch (character) {
            case 'q' -> {
              screen.close();
              return;
            }
            case 'i' -> mode = INSERT;
            case 'a' -> {
              mode = INSERT;
              if (screen.getCursorPosition().getColumn() != word.length() + 2) {
                screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(1));
                screen.refresh();
              }
            }
            case 'h' -> {
              if (screen.getCursorPosition().getColumn() != 2) {
                screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(-1));
                screen.refresh();
              }
            }
            case 'l' -> {
              if (screen.getCursorPosition().getColumn() != word.length() + 1) {
                screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(1));
                screen.refresh();
              }
            }
          }
        } else {
          word.insert(screen.getCursorPosition().getColumn() - 2, character);
          updateWord(textGraphics, word.toString());
          screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(1));
          newInput = updateList(textGraphics, input, screen, word.toString());
          screen.refresh();
        }
      }
    }
  }

  public static void updateWord(TextGraphics textGraphics, String word) {
    for (int i = 2; i < word.length() + 2; i++) {
      textGraphics.setCharacter(i, 0,
          new TextCharacter(word.charAt(i - 2), ANSI.BLUE, ANSI.DEFAULT, SGR.UNDERLINE, SGR.BOLD));
    }

    textGraphics.setCharacter(0, 0, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
  }

  public static String[] updateList(TextGraphics textGraphics, String[] input, Screen screen, String word) {
    textGraphics.fillRectangle(new TerminalPosition(0, 1), screen.getTerminalSize().withRelativeRows(-1), ' ');
    String[] newInput = Arrays.stream(input).filter((string) -> string.length() >= word.length())
        .sorted(Comparator.comparingInt(string -> minDistance(word, string))).toArray(String[]::new);

    for (int i = 1; i < screen.getTerminalSize().getRows(); i++) {
      if (i - 1 >= newInput.length) {
        break;
      }

      textGraphics.putString(2, i, newInput[i - 1], SGR.BOLD);
    }

    textGraphics.setCharacter(0, 1, new TextCharacter('>', ANSI.RED, ANSI.DEFAULT));
    return newInput;
  }

  public static int minDistance(String word1, String word2) {
    return minDistance(word1, word2, new int[word1.length()][word2.length()], 0, 0);
  }

  private static int minDistance(String word1, String word2, int[][] memo, int curr1, int curr2) {
    if (curr1 >= word1.length() && curr2 >= word2.length()) {
      return 0;
    }

    if (curr1 >= word1.length()) {
      return word2.length() - curr2;
    }

    if (curr2 >= word2.length()) {
      return word1.length() - curr1;
    }

    if (memo[curr1][curr2] != 0) {
      return memo[curr1][curr2];
    }

    if (word1.charAt(curr1) == word2.charAt(curr2)) {
      return memo[curr1][curr2] = minDistance(word1, word2, memo, curr1 + 1, curr2 + 1);
    }

    int insert = minDistance(word1, word2, memo, curr1 + 1, curr2) + 1;
    int delete = minDistance(word1, word2, memo, curr1, curr2 + 1) + 1;
    int replace = minDistance(word1, word2, memo, curr1 + 1, curr2 + 1) + 2;
    return memo[curr1][curr2] = NumberUtils.min(insert, delete, replace);
  }

}
