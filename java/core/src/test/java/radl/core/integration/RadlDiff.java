package radl.core.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;
import radl.core.code.RadlCode;
import radl.core.code.SourceFile;


public class RadlDiff implements Application {

  public static void main(String[] args) {
    Cli.run(RadlDiff.class, args);
  }

  @Override
  public int run(Arguments arguments) {
    if (!arguments.hasNext()) {
      Log.error("Missing original and revised RADL files");
      return 2;
    }
    File original = arguments.file(); // NOPMD PrematureDeclaration
    if (!arguments.hasNext()) {
      Log.error("Missing revised RADL file");
      return 1;
    }
    File revised = arguments.file();
    Collection<String> differences = new ArrayList<String>();
    diff(original, revised, differences);
    for (String difference : differences) {
      Log.error(difference);
    }
    return differences.size();
  }

  private void diff(File original, File revised, Collection<String> differences) {
    diff(toRadl(original), toRadl(revised), differences);
  }

  private RadlCode toRadl(File file) {
    return (RadlCode)new SourceFile(file.getPath()).code();
  }

  private void diff(RadlCode original, RadlCode revised, Collection<String> differences) {
    diffService(original, revised, differences);
    diffMediaTypes(original, revised, differences);
  }

  private void diffService(RadlCode original, RadlCode revised, Collection<String> differences) {
    diffItem("Service name", original.service(), revised.service(), differences);
  }

  private void diffItem(String type, String expected, String actual, Collection<String>  differences) {
    if (!expected.equalsIgnoreCase(actual)) {
      differences.add(String.format("%s: expected <%s> but got <%s>", type, expected, actual));
    }
  }

  private void diffMediaTypes(RadlCode original, RadlCode revised, Collection<String> differences) {
    diffIterable("Media type", original.mediaTypeNames(), revised.mediaTypeNames(), differences);
  }

  private void diffIterable(String type, Iterable<String> original, Iterable<String> revised,
      Collection<String> differences) {
    Iterator<String> expected = original.iterator();
    Iterator<String> actual = revised.iterator();
    while (expected.hasNext()) {
      String item = expected.next();
      if (actual.hasNext()) {
        diffItem(type, item, actual.next(), differences);
      } else {
        diffItem(type, item, null, differences);
        break;
      }
    }
    if (actual.hasNext()) {
      diffItem(type, null, actual.next(), differences);
    }
  }

}
