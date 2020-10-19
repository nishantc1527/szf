package szf.algorithm;

import java.io.IOException;
import java.util.Arrays;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;

public class ListUpdater {

  public static String[] updateList(TextGraphics textGraphics, String[] input, Screen screen, String word)
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

}
