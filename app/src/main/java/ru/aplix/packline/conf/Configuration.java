package ru.aplix.packline.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static transient Pattern pathPattern = Pattern.compile("\\s*\\$\\s*\\((.+)\\)\\s*", Pattern.CASE_INSENSITIVE);

	@XmlElement(name = "Hardware")
	private HardwareConfiguration hardwareConfiguration;

	@XmlElement(name = "ActivityMonitor")
	private ActivityMonitorConfiguration activityMonitorConfiguration;

	@XmlElement(name = "PostService")
	private PostService postService;

	@XmlElement(name = "Roles")
	private Roles roles;

	@XmlElement(name = "EmptyBoxThreshold")
	private Integer emptyBoxThreshold;

	@XmlElement(name = "ActivePostsUpdateInterval")
	private Integer activePostsUpdateInterval;

	@XmlElement(name = "Sounds")
	private Boolean soundsEnabled;

	@XmlElement(name = "TrolleyPackAutoClose")
	private Boolean trolleyPackAutoClose;

	@XmlElementWrapper(name = "Printing")
	@XmlElement(name = "Form", type = PrintForm.class)
	private List<PrintForm> printForms;

	@XmlElement(name = "Stickers")
	private StickersContainer stickersContainer;

	@XmlElement(name = "Weighting")
	private Weighting weighting;

	@XmlElement(name = "ZebraTest")
	private ZebraTest zebraTest;

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

		Printer printer = getHardwareConfiguration().lookupPrinter(getStickers().getForContainers().getPrinterId());
		getStickers().getForContainers().setPrinter(printer);

		printer = getHardwareConfiguration().lookupPrinter(getStickers().getForCustomers().getPrinterId());
		getStickers().getForCustomers().setPrinter(printer);

		printer = getHardwareConfiguration().lookupPrinter(getZebraTest().getPrinterId());
		getZebraTest().setPrinter(printer);

		// Resolve path settings
		getPostService().setRemoteStoragePath(resolvePath(getPostService().getRemoteStoragePath()));
	}

	private String resolvePath(String value) {
		Matcher matcher = pathPattern.matcher(value);
		if (matcher.matches()) {
			String property = matcher.group(1).trim();
			String result = System.getProperty(property);
			return result != null ? result : value;
		} else {
			return value;
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

	public Integer getEmptyBoxThreshold() {
		return emptyBoxThreshold != null ? emptyBoxThreshold : 0;
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

	public StickersContainer getStickers() {
		if (stickersContainer == null) {
			stickersContainer = new StickersContainer();
		}
		return stickersContainer;
	}

	public void setStickers(StickersContainer stickersContainer) {
		this.stickersContainer = stickersContainer;
	}

	public Boolean getSoundsEnabled() {
		return soundsEnabled != null ? soundsEnabled : true;
	}

	public void setSoundsEnabled(Boolean soundsEnabled) {
		this.soundsEnabled = soundsEnabled;
	}

	public Boolean getTrolleyPackAutoClose() {
		return trolleyPackAutoClose != null ? trolleyPackAutoClose : false;
	}

	public void setTrolleyPackAutoClose(Boolean trolleyPackAutoClose) {
		this.trolleyPackAutoClose = trolleyPackAutoClose;
	}

	public Weighting getWeighting() {
		if (weighting == null) {
			weighting = new Weighting();
		}
		return weighting;
	}

	public void setWeighting(Weighting weighting) {
		this.weighting = weighting;
	}

	public Roles getRoles() {
		if (roles == null) {
			roles = new Roles();
		}
		return roles;
	}

	public void setRoles(Roles roles) {
		this.roles = roles;
	}

	public ZebraTest getZebraTest() {
		if (zebraTest == null) {
			zebraTest = new ZebraTest();
		}
		return zebraTest;
	}

	public void setZebraTest(ZebraTest zebraTest) {
		this.zebraTest = zebraTest;
	}

	public Integer getActivePostsUpdateInterval() {
		return activePostsUpdateInterval != null ? activePostsUpdateInterval : 0;
	}

	public void setActivePostsUpdateInterval(Integer activePostsUpdateInterval) {
		this.activePostsUpdateInterval = activePostsUpdateInterval;
	}

}
