package szf.algorithm;

import java.io.IOException;
import java.util.Arrays;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor.ANSI;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;

public class Updater {

  public static void updateWord(TextGraphics textGraphics, String word) throws IOException {
    for (int i = 2; i < word.length() + 2; i++) {
      textGraphics.setCharacter(i, 0,
          new TextCharacter(word.charAt(i - 2), ANSI.BLUE, ANSI.DEFAULT, SGR.UNDERLINE, SGR.BOLD));
    }

    textGraphics.setCharacter(0, 0, new TextCharacter('>', ANSI.GREEN, ANSI.DEFAULT, SGR.BOLD));
  }

  public static String[] updateList(TextGraphics textGraphics, String[] input, Screen screen, String word)
      throws IOException {
    textGraphics.fillRectangle(new TerminalPosition(0, 1), screen.getTerminalSize().withRelativeRows(-1), ' ');
    String[] newInput = Arrays.stream(input).filter(FilterAndSort.filter(word)).sorted(FilterAndSort.sort(word))
        .toArray(String[]::new);

    for (int i = 1; i < screen.getTerminalSize().getRows(); i++) {
      if (i - 1 >= newInput.length) {
        break;
      }

      textGraphics.putString(2, i, newInput[i - 1], SGR.BOLD);
    }

    textGraphics.setCharacter(0, 1, new TextCharacter('>', ANSI.RED, ANSI.DEFAULT));
    return newInput;
  }
}
