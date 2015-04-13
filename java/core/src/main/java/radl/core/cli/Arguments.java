/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Arguments provided on the command-line to an application.
 * <p>
 * The {@linkplain Iterator} interface is implemented, but additional convenience methods are also available, for
 * instance, to get a {@linkplain File} from a file name argument. Such methods have overloaded versions that take a
 * default value as parameter, which will be used in case the argument was not supplied on the command-line.
 * <p>
 * Arguments are positional only; named arguments are not supported.
 */
public class Arguments implements Iterator<String> {

  private final List<String> args = new ArrayList<String>();
  private int index;

  /**
   * @param args The command-line arguments, typically as provided to the <code>main()</code> method
   */
  public Arguments(String[]... args) {
    for (String[] arguments : args) {
      this.args.addAll(Arrays.asList(arguments));
    }
  }

  @Override
  public boolean hasNext() {
    return index < args.size();
  }

  @Override
  public String next() {
    if (hasNext()) {
      return args.get(index++);
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    args.remove(index);
  }

  /**
   * @param defaultValue The value to use in case there is no more argument
   * @return the next argument, or the provided default value if there is none
   */
  public String next(String defaultValue) {
    return hasNext() ? next() : defaultValue;
  }

  /**
   * @param defaultValue The file name to use in case there is no more argument
   * @return a file named by the next argument, or by the provided default value if there is none
   */
  public File file(String defaultValue) {
    return new File(next(defaultValue));
  }

  /**
   * @return a file named by the next argument
   */
  public File file() {
    return new File(next());
  }

  /**
   * Move back to the previous argument, so that it can be processed again.
   */
  public void prev() {
    index--;
  }

}
