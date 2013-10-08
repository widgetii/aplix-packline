package ru.aplix.packline.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.GregorianCalendar;

import javafx.application.Application;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.xml.sax.InputSource;

/**
 * Utilities.
 */
public final class Utils {

	/**
	 * Hidden constructor.
	 */
	private Utils() {
	}

	/**
	 * Convert any object instance to XML string.
	 * 
	 * @param o
	 *            object instance
	 * @return XML string
	 * @throws JAXBException
	 *             if any unexpected problem occurs during the marshalling
	 */
	public static String objectToXMLString(Object o) throws JAXBException {
		JAXBContext inst = JAXBContext.newInstance(o.getClass());
		Marshaller marshaller = inst.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		StringWriter out = new StringWriter();
		marshaller.marshal(o, out);
		return out.toString();
	}

	/**
	 * Convert xml string to an object instance.
	 * 
	 * @param xml
	 *            string
	 * @param objectClass
	 *            class of returned object
	 * @return a new class instance
	 * @throws JAXBException
	 *             if any unexpected problem occurs during the unmarshalling
	 */
	@SuppressWarnings("unchecked")
	public static <T> T xmlStringToObject(String xml, Class<T> objectClass) throws JAXBException {
		JAXBContext inst = JAXBContext.newInstance(objectClass);
		Unmarshaller unmarshaller = inst.createUnmarshaller();
		return (T) unmarshaller.unmarshal(new StringReader(xml));
	}

	/**
	 * Convert file to an object instance.
	 * 
	 * @param xml
	 *            string
	 * @param objectClass
	 *            class of returned object
	 * @return a new class instance
	 * @throws JAXBException
	 *             if any unexpected problem occurs during the unmarshalling
	 * @throws FileNotFoundException
	 *             if file not found
	 * @throws MalformedURLException
	 *             if a protocol handler for the URL could not be found
	 */
	@SuppressWarnings("unchecked")
	public static <T> T fileToObject(File file, Class<T> objectClass) throws JAXBException, FileNotFoundException, MalformedURLException {
		JAXBContext inst = JAXBContext.newInstance(objectClass);
		Unmarshaller unmarshaller = inst.createUnmarshaller();
		@SuppressWarnings("deprecation")
		String systemId = file.toURL().toString();
		return (T) unmarshaller.unmarshal(new InputSource(systemId));
	}

	/**
	 * Return full path to jar file.
	 * 
	 * @param c
	 *            class
	 * @return path
	 */
	public static String getJarFolder(Class<?> c) {
		File currentJavaJarFile = new File(c.getProtectionDomain().getCodeSource().getLocation().getPath());
		String currentJavaJarFilePath = currentJavaJarFile.getAbsolutePath();
		String currentRootDirectoryPath = currentJavaJarFilePath.replace(currentJavaJarFile.getName(), "");
		return currentRootDirectoryPath;
	}

	/**
	 * 
	 */
	public static final boolean isJ2DPipelineUsed;

	static {
		isJ2DPipelineUsed = isJ2DPipelineUsed();
	}

	public static boolean isJ2DPipelineUsed() {
		java.lang.reflect.Method m;
		try {
			m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
			m.setAccessible(true);
			ClassLoader cl = Application.class.getClassLoader();
			Object test = m.invoke(cl, "com.sun.prism.j2d.J2DPipeline");
			return test != null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static XMLGregorianCalendar now() throws DatatypeConfigurationException {
		GregorianCalendar gcal = new GregorianCalendar();
		XMLGregorianCalendar xmlGC = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
		return xmlGC;
	}
}
