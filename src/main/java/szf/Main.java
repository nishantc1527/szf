package szf;

import java.io.IOException;
import java.util.Arrays;

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

import picocli.CommandLine;
import picocli.CommandLine.Option;
import szf.algorithm.FilterAndSort;

public class Main {

  @Option(names = { "-i",
      "--input" }, description = "The Input Text, Separated By New Line Characters", required = true)
  private String input;

  private static String[] updateList(TextGraphics textGraphics, String[] input, Screen screen, String word)
      throws IOException {
    textGraphics.fillRectangle(new TerminalPosition(0, 1), screen.getTerminalSize().withRelativeRows(-1), ' ');
    String[] newInput = Arrays.stream(input).filter(FilterAndSort.filter(word)).sorted(FilterAndSort.sort(word))
        .toArray(String[]::new);

    for (int i = 1; i <= newInput.length; i++) {
      textGraphics.putString(0, i, newInput[i - 1], SGR.BOLD);
    }

    screen.refresh();

    return newInput;
  }

  public static void main(String[] args) throws IOException {
    Main main = new Main();
    new CommandLine(main).parseArgs(args);

    String[] input = main.input.split("\n");

    DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    Screen screen = new TerminalScreen(defaultTerminalFactory.createTerminal());

    screen.startScreen();
    screen.setCursorPosition(new TerminalPosition(2, 0));

    screen.refresh();

    TextGraphics textGraphics = screen.newTextGraphics().setTabBehaviour(TabBehaviour.CONVERT_TO_FOUR_SPACES);

    for (int i = 1; i <= input.length && i < screen.getTerminalSize().getRows(); i++) {
      textGraphics.putString(0, i, input[i - 1], SGR.BOLD);
    }

    screen.refresh();

    textGraphics.setCharacter(0, 0, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
    screen.refresh();

    String[] newInput = input;
    StringBuilder word = new StringBuilder();

    while (true) {
      KeyStroke keyStroke = screen.readInput();

      if (keyStroke.getKeyType() == KeyType.Escape) {
        screen.close();
        return;
      }

      if (keyStroke.getKeyType() == KeyType.Enter) {
        break;
      }

      if (keyStroke.getKeyType() == KeyType.Backspace) {
        if (word.length() > 0) {
          screen.setCharacter(word.length() + 1, 0, new TextCharacter(' ', ANSI.DEFAULT, ANSI.DEFAULT));
          word.deleteCharAt(word.length() - 1);
          screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(-1));
          screen.refresh();

          newInput = updateList(textGraphics, input, screen, word.toString());
        }
      } else {
        Character character = keyStroke.getCharacter();

        if (character != null) {
          word.append(character);
          screen.setCharacter(word.length() + 1, 0,
              new TextCharacter(character, ANSI.BLUE, ANSI.DEFAULT, SGR.BOLD, SGR.UNDERLINE));
          screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(1));
          screen.refresh();

          newInput = updateList(textGraphics, input, screen, word.toString());
        }
      }
    }

    screen.close();

    if (newInput.length != 0) {
      System.out.println(newInput[0]);
    }
  }

}
