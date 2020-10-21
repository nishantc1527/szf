package szf;

import java.io.IOException;

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

import szf.algorithm.Updater;

public class Main {

  public static final boolean INSERT = false, COMMAND = true;

  public static void main(final String[] args) throws IOException {
    final String[] input = args[0].split("\n");

    final DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    final Screen screen = new TerminalScreen(defaultTerminalFactory.createTerminal());

    screen.startScreen();
    screen.setCursorPosition(new TerminalPosition(2, 0));

    screen.refresh();

    final TextGraphics textGraphics = screen.newTextGraphics().setTabBehaviour(TabBehaviour.CONVERT_TO_FOUR_SPACES);

    for (int i = 1; i <= input.length && i < screen.getTerminalSize().getRows(); i++) {
      textGraphics.putString(2, i, input[i - 1], SGR.BOLD);
    }

    textGraphics.setCharacter(0, 0, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
    textGraphics.setCharacter(0, 1, new TextCharacter('>', ANSI.RED, ANSI.DEFAULT));

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
            newInput = Updater.updateList(textGraphics, input, screen, word.toString());
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
          Updater.updateWord(screen, word.toString());
          screen.setCursorPosition(screen.getCursorPosition().withRelativeColumn(1));

          newInput = Updater.updateList(textGraphics, input, screen, word.toString());

          screen.refresh();
        }
      }
    }
  }

}
