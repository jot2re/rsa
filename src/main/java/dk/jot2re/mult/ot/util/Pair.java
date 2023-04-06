package dk.jot2re.mult.ot.util;

import java.io.Serializable;

public class Pair<S, T> implements Serializable {

  private final S first;
  private final T second;

  public Pair(S first, T second) {
    this.first = first;
    this.second = second;
  }

  public S getFirst() {
    return first;
  }

  public T getSecond() {
    return second;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Pair<?, ?>) {
      Pair<?, ?> other = (Pair<?, ?>) obj;
      if (other.getFirst().equals(first) && other.getSecond().equals(second)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "<" + first + ", " + second + ">";
  }

  @Override
  public int hashCode() {
    return (first.hashCode() + second.hashCode()) % Integer.MAX_VALUE;
  }

}
