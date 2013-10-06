package ru.aplix.packline.post;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

public class ConfigurationTest extends TestCase {

	private final Log LOG = LogFactory.getLog(getClass());

	private Reader getConfigurationReader() throws UnsupportedEncodingException {
		InputStream is = getClass().getResourceAsStream("/configuration.xml");
		Reader reader = new InputStreamReader(is, "UTF-8");
		return reader;
	}

	public void test() throws JAXBException, IOException, SAXException {
		JAXBContext inst = JAXBContext.newInstance(Configuration.class);
		Unmarshaller unmarshaller = inst.createUnmarshaller();
		Marshaller marshaller = inst.createMarshaller();

		Configuration conf = (Configuration) unmarshaller.unmarshal(getConfigurationReader());
		assertNotNull(conf);

		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		StringWriter out = new StringWriter();
		marshaller.marshal(conf, out);

		String test = out.toString();
		String control = IOUtils.toString(getConfigurationReader());

		LOG.debug(test);
		LOG.debug(control);

		XMLUnit.setIgnoreWhitespace(true);
		XMLAssert.assertXMLEqual("Configurations differ.", control, test);
	}
}
