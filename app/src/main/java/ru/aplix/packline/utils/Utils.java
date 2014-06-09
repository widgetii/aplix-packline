package ru.aplix.packline.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.GregorianCalendar;

import javafx.application.Application;
import javafx.scene.media.AudioClip;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.post.PackingSize;

/**
 * Utilities.
 */
public final class Utils {

	private static final Log LOG = LogFactory.getLog(Utils.class);

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

	public static void sendDataToSocket(String host, int port, byte[] data, Integer copies) throws IOException {
		Socket socket = new Socket(host, port);
		try {
			OutputStream os = socket.getOutputStream();
			try {
				int count = (copies != null && copies > 0) ? copies : 1;
				for (int i = 0; i < count; i++) {
					os.write(data);
					os.flush();
				}
			} finally {
				os.close();
			}
		} finally {
			socket.close();
		}
	}

	public static String getMACAddress() throws UnknownHostException, SocketException {
		String result = "";
		Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
		while ((result == null || result.length() == 0) && nis.hasMoreElements()) {
			NetworkInterface ni = nis.nextElement();

			byte[] mac = ni.getHardwareAddress();
			if (mac == null || mac.length == 0) {
				continue;
			}

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
			}
			result = sb.toString();
		}
		return result;
	}

	public static final int SOUND_ERROR = 0x01;
	public static final int SOUND_WARNING = 0x02;

	public static void playSound(int soundType) {
		try {
			if (!Configuration.getInstance().getSoundsEnabled()) {
				return;
			}

			URL url = null;
			switch (soundType) {
			case SOUND_ERROR:
				url = Utils.class.getResource("/resources/sounds/error.mp3");
				break;
			case SOUND_WARNING:
				url = Utils.class.getResource("/resources/sounds/warning.mp3");
				break;
			}

			if (url != null) {
				AudioClip plonkSound = new AudioClip(url.toExternalForm());
				plonkSound.play();
			}
		} catch (Exception e) {
			LOG.error(null, e);
		}
	}

	public static boolean isPackingSizeEmpty(PackingSize ps) {
		if (ps != null) {
			return !(isValid(ps.getHeight()) && isValid(ps.getWidth()) && isValid(ps.getLength()));
		} else {
			return true;
		}
	}

	private final static double EPSILON = 1E-5;

	private static boolean isValid(float a) {
		return !Float.isInfinite(a) && !Float.isNaN(a) && a > EPSILON;
	}
}
