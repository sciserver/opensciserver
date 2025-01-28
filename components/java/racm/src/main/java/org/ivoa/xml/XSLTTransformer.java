package org.ivoa.xml;


import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;


/**
 * XSLT transformer with Saxon 9 API (xslt 2 engine)
 *
 * @author Laurent Bourges (voparis) / Gerard Lemson (mpe)
 */
public final class XSLTTransformer {
  //~ Methods ----------------------------------------------------------------------------------------------------------

  /**
   * Transforms the xml stream with the given xslt script
   *
   * @param sheetFile file name of XSLT style sheet, could be a URL
   * @param parameters assumes simple parameters
   * @param in the input stream containing the document to be transformed
   * @param out the output stream to which the result should be written
   *
   * @throws Exception if failure
   */
  public static void transform(final String sheetFile, final java.util.Map<String, String> parameters,
                               final InputStream in, final OutputStream out)
                        throws Exception {
    // use Saxon's s9api.
    // Could use Saxon's JAXP implementation, interfaces compatible with other XSLT processors such as Xalan. 
    final Processor    proc = new Processor(false);
    final XsltCompiler comp = proc.newXsltCompiler();

    // next could be passed in if precompiled, or exists as singleton for the style sheet
    final XsltExecutable exp = comp.compile(new StreamSource(new File(sheetFile)));

    // doc should probably be passed in as stream.
    final XdmNode source = proc.newDocumentBuilder().build(new StreamSource(in));

    final XsltTransformer trans = exp.load();

    trans.setInitialContextNode(source);

    for (final String key : parameters.keySet()) {
      trans.setParameter(new QName(key), new XdmAtomicValue(parameters.get(key)));
    }

    final Serializer ser = new Serializer();

    ser.setOutputProperty(Serializer.Property.METHOD, "xml");
    ser.setOutputProperty(Serializer.Property.INDENT, "yes");
    ser.setOutputStream(out);

    trans.setDestination(ser);
    trans.transform();
  }

  /**
   * Transforms the xml stream with the given xslt script
   *
   * @param sheetFile file name of XSLT style sheet, could be a URL
   * @param parameters assumes simple parameters
   * @param in the input stream containing the document to be transformed
   * @param out the output stream to which the result should be written
   *
   * @throws Exception if failure
   */
  public static void transform(final String sheetFile, final java.util.Map<String, String> parameters,
                               final InputStream in, final Writer out)
                        throws Exception {
    // use Saxon's s9api.
    // Could use Saxon's JAXP implementation, interfaces compatible with other XSLT processors such as Xalan. 
    final Processor    proc = new Processor(false);
    final XsltCompiler comp = proc.newXsltCompiler();

    // next could be passed in if precompiled, or exists as singleton for the style sheet
    final XsltExecutable exp = comp.compile(new StreamSource(new File(sheetFile)));

    // doc should probably be passed in as stream.
    final XdmNode source = proc.newDocumentBuilder().build(new StreamSource(in));

    final XsltTransformer trans = exp.load();

    trans.setInitialContextNode(source);

    for (final String key : parameters.keySet()) {
      trans.setParameter(new QName(key), new XdmAtomicValue(parameters.get(key)));
    }

    final Serializer ser = new Serializer();

    ser.setOutputProperty(Serializer.Property.METHOD, "xml");
    ser.setOutputProperty(Serializer.Property.INDENT, "yes");
    ser.setOutputWriter(out);

    trans.setDestination(ser);
    trans.transform();
  }

  /**
   * Transforms the xml stream with the given xslt script to a string
   *
   * @param sheetFile file name of XSLT style sheet, could be a URL
   * @param parameters assumes simple parameters
   * @param xmldoc_url the file URL containing the document to be transformed
   *
   * @return String value
   *
   * @throws Exception if failure
   */
  public static String transform2string(final String sheetFile, final java.util.Map<String, String> parameters,
                                        final String xmldoc_url)
                                 throws Exception {
    final StringWriter out    = new StringWriter();
    final URL          xmldoc = new URL(xmldoc_url);
    final InputStream  in     = xmldoc.openStream();

    try {
        transform(sheetFile, parameters, in, out);
    } finally {
        in.close();
    }

    return out.toString();
  }

}
//~ End of file --------------------------------------------------------------------------------------------------------
