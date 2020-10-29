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

  private static void updateWord(TextGraphics textGraphics, String word) {
    for (int i = 2; i < word.length() + 2; i++) {
      textGraphics.setCharacter(i, 0,
          new TextCharacter(word.charAt(i - 2), ANSI.BLUE, ANSI.DEFAULT, SGR.UNDERLINE, SGR.BOLD));
    }

    textGraphics.setCharacter(0, 0, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
  }

  private static String[] updateList(TextGraphics textGraphics, String[] input, Screen screen, String word) {
    textGraphics.fillRectangle(new TerminalPosition(0, 1), screen.getTerminalSize().withRelativeRows(-1), ' ');
    String[] newInput = Arrays.stream(input).filter((string) -> string.length() >= word.length())
        .sorted(Comparator.comparingInt(string -> levenshtein(word, string))).toArray(String[]::new);

    for (int i = 1; i < screen.getTerminalSize().getRows(); i++) {
      if (i - 1 >= newInput.length) {
        break;
      }

      textGraphics.putString(2, i, newInput[i - 1], SGR.BOLD);
    }

    textGraphics.setCharacter(0, 1, new TextCharacter('>', ANSI.RED, ANSI.DEFAULT));
    return newInput;
  }

  private static int levenshtein(String word1String, String word2String) {
    char[] word1 = word1String.toCharArray();
    char[] word2 = word2String.toCharArray();
    int[][] dp = new int[word1.length + 1][word2.length + 1];

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
          dp[i][j] = 1 + NumberUtils.min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]);
        }
      }
    }

    return dp[dp.length - 1][dp[0].length - 1];
  }

}
