package szf.algorithm;

import java.util.Comparator;
import java.util.function.Predicate;

public class FilterAndSort {
  public static Predicate<String> filter(String word) {
    return (string) -> string.length() >= word.length();
  }

  public static Comparator<String> sort(String word) {
    return (string1, string2) -> Integer.compare(EditDistance.minDistance(word, string1),
        EditDistance.minDistance(word, string2));
  }
}
