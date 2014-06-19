package ru.aplix.packline.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

import ru.aplix.converters.fr2afop.utils.Utils;

public class ImagesToPDFConverter {

	public static final String CONF_FILE = "/conf/fop.xconf";
	public static final String XSLT_FILE = "/resources/xsl/images-to-pdf.xsl";
	private static final String outputFormat = MimeConstants.MIME_PDF;

	private FopFactory fopFactory = null;
	private Templates cachedXSLT = null;

	private FopFactory getFopFactory() throws SAXException, IOException {
		if (fopFactory == null) {
			String configFileName = Utils.getJarFolder(getClass()) + CONF_FILE;
			fopFactory = FopFactory.newInstance(new File(configFileName));
		}
		return fopFactory;
	}

	private Transformer getTransformer() throws TransformerConfigurationException {
		if (cachedXSLT == null) {
			// Setup XSLT
			InputStream xsltis = getClass().getResourceAsStream(XSLT_FILE);
			TransformerFactory factory = TransformerFactory.newInstance();
			cachedXSLT = factory.newTemplates(new StreamSource(xsltis));
		}
		return cachedXSLT.newTransformer();
	}

	public void convert(List<File> imageFiles, String outputFileName) throws ConfigurationException, SAXException, IOException, TransformerException {
		// Create fopFactory and Setup XSLT
		FopFactory fopFactory = getFopFactory();
		Transformer transformer = getTransformer();

		// Setup output
		OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFileName));
		try {
			// Setup input for XSLT transformation
			Source src = new StreamSource(new StringReader(listToXMLString(imageFiles)));

			Result res;
			if (MimeConstants.MIME_XSL_FO.equals(outputFormat)) {
				res = new StreamResult(out);

				// Start XSLT transformation and FOP processing
				transformer.transform(src, res);
			} else {
				// Set up a custom user agent so we can supply our own renderer
				// instance
				FOUserAgent userAgent = fopFactory.newFOUserAgent();
				Fop fop;

				// Construct fop with desired output format
				fop = fopFactory.newFop(outputFormat, userAgent, out);

				// Resulting SAX events (the generated FO)
				// must be piped through to FOP
				res = new SAXResult(fop.getDefaultHandler());

				// Start XSLT transformation and FOP processing
				transformer.transform(src, res);
			}
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private String listToXMLString(List<File> imageFiles) {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<files>\n");
		for (File file : imageFiles) {
			sb.append(String.format("<file>%s</file>\n", file.toURI()));
		}
		sb.append("</files>\n");
		return sb.toString();
	}
}
