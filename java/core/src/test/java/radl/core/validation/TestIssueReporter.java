package radl.core.validation;

import java.io.IOException;
import java.io.PrintWriter;


public class TestIssueReporter implements IssueReporter {

  public static final String FILE_NAME = TestIssueReporter.class.getName();
  public static final String ID = "test";

  @Override
  public void setReportFileName(String reportFileName) {
    try (PrintWriter writer = new PrintWriter(FILE_NAME, "UTF-8")) {
      writer.println(FILE_NAME);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void start() {
  }

  @Override
  public void file(String fileName) {
  }

  @Override
  public void issue(Issue issue) {
  }

  @Override
  public void end() {
  }

  @Override
  public String getId() {
    return ID;
  }

}
