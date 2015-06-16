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


public class RadlComparer implements Application {

  public static void main(String[] args) {
    Cli.run(RadlComparer.class, args);
  }

  private final Collection<String> differences = new ArrayList<String>();

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
    assertRadl(original, revised);
    for (String difference : differences) {
      Log.error(difference);
    }
    return differences.size();
  }

  private void assertRadl(File original, File revised) {
    assertRadl(toRadl(original), toRadl(revised));
  }

  private RadlCode toRadl(File file) {
    return (RadlCode)new SourceFile(file.getPath()).code();
  }

  private void assertRadl(RadlCode original, RadlCode revised) {
    assertService(original, revised);
    assertMediaTypes(original, revised);
    assertResources(original, revised);
  }

  private void assertService(RadlCode original, RadlCode revised) {
    assertItem("Service name", original.service(), revised.service());
  }

  private void assertItem(String type, String expected, String actual) {
    if (!expected.equalsIgnoreCase(actual)) {
      differences.add(String.format("%s: expected <%s> but got <%s>", type, expected, actual));
    }
  }

  private void assertMediaTypes(RadlCode original, RadlCode revised) {
    assertItems("Media type", original.mediaTypeNames(), revised.mediaTypeNames());
  }

  private Iterable<String> assertItems(String type, Iterable<String> original, Iterable<String> revised) {
    Iterator<String> expected = original.iterator();
    Iterator<String> actual = revised.iterator();
    while (expected.hasNext()) {
      String item = expected.next();
      if (actual.hasNext()) {
        assertItem(type, item, actual.next());
      } else {
        assertItem(type, item, null);
        break;
      }
    }
    if (actual.hasNext()) {
      assertItem(type, null, actual.next());
    }
    return original;
  }

  private void assertResources(RadlCode original, RadlCode revised) {
    for (String resource : assertItems("Resource", original.resourceNames(), revised.resourceNames())) {
      assertItem("Location of " + resource, original.resourceLocation(resource), revised.resourceLocation(resource));
      for (String method : assertItems("Methods for " + resource, original.methodNames(resource),
          revised.methodNames(resource))) {
        assertItems("Request representations for " + resource + "." + method,
            original.methodRequestRepresentations(resource, method),
            revised.methodRequestRepresentations(resource, method));
        assertItems("Response representations for " + resource + "." + method,
            original.methodResponseRepresentations(resource, method),
            revised.methodResponseRepresentations(resource, method));
      }
    }
  }

}
