/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import radl.core.validation.Issue.Level;

import com.thaiopensource.relaxng.jaxp.CompactSyntaxSchemaFactory;


/**
 * Validate a RADL document against the Relax NG schema.
 */
public class RelaxNgValidator implements Validator {

  private static final Pattern SAX_ERROR = Pattern.compile("org.xml.sax.SAXParseException; "
      + "lineNumber: (\\d+); columnNumber: (\\d+); (.+)");
  private static final String RADL_SCHEMA = "radl.rnc";

  @Override
  public void validate(final InputStream stream, final Collection<Issue> issues) {
    javax.xml.validation.Validator validator = newValidator();
    validator.setErrorHandler(new ErrorHandler() {
      @Override
      public void warning(SAXParseException exception) throws SAXException {
        issues.add(newIssue(Level.WARNING, exception));
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        issues.add(newIssue(Level.ERROR, exception));
      }

      @Override
      public void error(SAXParseException exception) throws SAXException {
        issues.add(newIssue(Level.ERROR, exception));
      }
    });
    try {
      validator.validate(new StreamSource(stream));
    } catch (Exception e) {
      issues.add(newIssue(Level.ERROR, e));
    }
  }

  private javax.xml.validation.Validator newValidator() {
    try {
      Schema schema = newRelaxNgSchema(new InputSource(getSchema(RADL_SCHEMA)));
      return schema.newValidator();
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  private InputStream getSchema(String schema) {
    return getClass().getResourceAsStream("/schema/" + schema);
  }

  private Schema newRelaxNgSchema(InputSource source) throws SAXException {
    CompactSyntaxSchemaFactory factory = new CompactSyntaxSchemaFactory();
    factory.setResourceResolver(new LSResourceResolver() {
      @Override
      public LSInput resolveResource(String type, String nsUri, String publicId, String systemId, String baseUri) {
        return new ClassLoaderInput(systemId, baseUri, publicId);
      }
    });
    return factory.newSchema(new SAXSource(source));
  }

  private Issue newIssue(Level level, Exception exception) {
    String message = exception.toString();
    int line = 0;
    int column = 0;
    Matcher matcher = SAX_ERROR.matcher(message);
    if (matcher.matches()) {
      line = Integer.parseInt(matcher.group(1));
      column = Integer.parseInt(matcher.group(2));
      message = matcher.group(3);
    }
    return new Issue(getClass(), level, line, column, message);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }


  private final class ClassLoaderInput implements LSInput {

    private final String systemId;
    private final String baseUri;
    private final String publicId;

    private ClassLoaderInput(String systemId, String baseUri, String publicId) {
      this.systemId = systemId;
      this.baseUri = baseUri;
      this.publicId = publicId;
    }

    @Override
    public InputStream getByteStream() {
      return getSchema(systemId);
    }

    @Override
    public String getBaseURI() {
      return baseUri;
    }

    @Override
    public String getSystemId() {
      return systemId;
    }

    @Override
    public String getPublicId() {
      return publicId;
    }

    @Override
    public void setSystemId(String systemId) {
      // Nothing to do
    }

    @Override
    public void setStringData(String stringData) {
      // Nothing to do
    }

    @Override
    public void setPublicId(String publicId) {
      // Nothing to do
    }

    @Override
    public void setEncoding(String encoding) {
      // Nothing to do
    }

    @Override
    public void setCharacterStream(Reader characterStream) {
      // Nothing to do
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {
      // Nothing to do
    }

    @Override
    public void setByteStream(InputStream byteStream) {
      // Nothing to do
    }

    @Override
    public void setBaseURI(String baseURI) {
      // Nothing to do
    }

    @Override
    public String getStringData() {
      return null;
    }

    @Override
    public String getEncoding() {
      return null;
    }

    @Override
    public Reader getCharacterStream() {
      return null;
    }

    @Override
    public boolean getCertifiedText() { // NOPMD BooleanGetMethodName
      return false;
    }

  }

}
