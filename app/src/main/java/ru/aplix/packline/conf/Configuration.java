package ru.aplix.packline.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.utils.Utils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "packline")
public class Configuration {

	public static transient final String DEFAULT_CONF_FILE = "/conf/packline.xconf";

	private static transient String configFileName = null;
	private static transient Configuration instance = null;

	@XmlElement(name = "Hardware")
	private HardwareConfiguration hardwareConfiguration;

	@XmlElement(name = "ActivityMonitor")
	private ActivityMonitorConfiguration activityMonitorConfiguration;

	@XmlElement(name = "PostService")
	private PostService postService;

	@XmlElementWrapper(name = "Stickers")
	@XmlElement(name = "Quantity")
	private List<Integer> stickersQuantity;

	@XmlElement(name = "EmptyBoxThreshold")
	private Integer emptyBoxThreshold;

	@XmlElementWrapper(name = "Printing")
	@XmlElement(name = "Form", type = PrintForm.class)
	private List<PrintForm> printForms;

	private Configuration() {

	}

	public static Configuration getInstance() throws FileNotFoundException, MalformedURLException, JAXBException {
		if (instance == null) {
			if (configFileName == null) {
				configFileName = Utils.getJarFolder(Configuration.class) + DEFAULT_CONF_FILE;
			}
			File configurationFile = new File(configFileName);
			instance = Utils.fileToObject(configurationFile, Configuration.class);
			instance.postConstruct();
		}
		return instance;
	}

	private void postConstruct() {
		for (PrintForm form : getPrintForms()) {
			Printer printer = getHardwareConfiguration().lookupPrinter(form.getPrinterId());
			form.setPrinter(printer);
		}
	}

	public static String getConfigFileName() {
		return configFileName;
	}

	public static void setConfigFileName(String value) {
		if (instance != null) {
			throw new RuntimeException("Configuration is already instantiated.");
		}
		Configuration.configFileName = value;
	}

	public HardwareConfiguration getHardwareConfiguration() {
		if (hardwareConfiguration == null) {
			hardwareConfiguration = new HardwareConfiguration();
		}
		return hardwareConfiguration;
	}

	public void setHardwareConfiguration(HardwareConfiguration hardwareConfiguration) {
		this.hardwareConfiguration = hardwareConfiguration;
	}

	public ActivityMonitorConfiguration getActivityMonitorConfiguration() {
		if (activityMonitorConfiguration == null) {
			activityMonitorConfiguration = new ActivityMonitorConfiguration();
		}
		return activityMonitorConfiguration;
	}

	public void setActivityMonitorConfiguration(ActivityMonitorConfiguration activityMonitorConfiguration) {
		this.activityMonitorConfiguration = activityMonitorConfiguration;
	}

	public PostService getPostService() {
		if (postService == null) {
			postService = new PostService();
		}
		return postService;
	}

	public void setPostService(PostService postService) {
		this.postService = postService;
	}

	public List<Integer> getStickersQuantity() {
		if (stickersQuantity == null) {
			stickersQuantity = new ArrayList<Integer>();
		}
		return stickersQuantity;
	}

	public void setStickersQuantity(List<Integer> stickersQuantity) {
		this.stickersQuantity = stickersQuantity;
	}

	public Integer getEmptyBoxThreshold() {
		return emptyBoxThreshold;
	}

	public void setEmptyBoxThreshold(Integer emptyBoxThreshold) {
		this.emptyBoxThreshold = emptyBoxThreshold;
	}

	public List<PrintForm> getPrintForms() {
		if (printForms == null) {
			printForms = new ArrayList<PrintForm>();
		}
		return printForms;
	}

	public void setPrintForms(List<PrintForm> printForms) {
		this.printForms = printForms;
	}
}
