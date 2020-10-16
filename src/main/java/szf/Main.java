package szf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

import szf.algorithm.EditDistance;
import szf.algorithm.FilterAndSort;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class Main {

  @Option(names = { "-c", "--command" }, description = "The Command To Run To Get The Input Strings", required = true)
  private String command;

  private static void updateList(TextGraphics textGraphics, List<String> input, Screen screen, String word)
      throws IOException {
      textGraphics.fillRectangle(new TerminalPosition(0, 1), screen.getTerminalSize().withRelativeRows(-1), ' ');
      List<String> newInput = input.stream().filter((string) -> {
        return string.length() >= word.length();
      }).sorted(FilterAndSort.sort(word)).collect(Collectors.toList());

      for (int i = 1; i <= newInput.size(); i++) {
        textGraphics.putString(0, i, newInput.get(i - 1), SGR.BOLD);
      }

      screen.refresh();
  }

  public static void main(String[] args) throws IOException {
    Main main = new Main();
    new CommandLine(main).parseArgs(args);

    System.out.println(main.command);
    System.exit(0);

    List<String> input = new ArrayList<>();
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(Runtime.getRuntime().exec(main.command.split(" ")).getInputStream()));

    while (reader.ready()) {
      String nextLine = reader.readLine();
      input.add(nextLine);
    }

    DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    Screen screen = new TerminalScreen(defaultTerminalFactory.createTerminal());

    screen.startScreen();
    screen.setCursorPosition(new TerminalPosition(2, 0));

    screen.refresh();

    TextGraphics textGraphics = screen.newTextGraphics().setTabBehaviour(TabBehaviour.CONVERT_TO_FOUR_SPACES);

    for (int i = 1; i <= input.size(); i++) {
      textGraphics.putString(0, i, input.get(i - 1), SGR.BOLD);
    }

    screen.refresh();

    textGraphics.setCharacter(0, 0, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
    screen.refresh();

    StringBuilder word = new StringBuilder();
    List<String> newInput = new ArrayList<>(input);

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

          updateList(textGraphics, input, screen, word.toString());
        }
      } else {
        Character character = keyStroke.getCharacter();

        if (character != null) {
          word.append(character);
          screen.setCharacter(word.length() + 1, 0,
              new TextCharacter(character, ANSI.BLUE, ANSI.DEFAULT, SGR.BOLD, SGR.UNDERLINE));
          screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(1));
          screen.refresh();

          updateList(textGraphics, input, screen, word.toString());
        }
      }
    }

    screen.close();

    if (newInput.size() != 0) {
      System.out.println(input.get(0));
    }
  }

}
