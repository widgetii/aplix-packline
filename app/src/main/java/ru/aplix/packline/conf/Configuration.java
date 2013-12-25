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

	@XmlElement(name = "Roles")
	private Roles roles;

	@XmlElement(name = "EmptyBoxThreshold")
	private Integer emptyBoxThreshold;

	@XmlElement(name = "Sounds")
	private Boolean soundsEnabled;

	@XmlElement(name = "TrolleyPackAutoClose")
	private Boolean trolleyPackAutoClose;

	@XmlElementWrapper(name = "Printing")
	@XmlElement(name = "Form", type = PrintForm.class)
	private List<PrintForm> printForms;

	@XmlElement(name = "Stickers")
	private StickersContainer stickersContainer;

	@XmlElementWrapper(name = "Weighting")
	@XmlElement(name = "Restriction", type = WeightingRestriction.class)
	private List<WeightingRestriction> weightingRestrictions;

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
		return soundsEnabled != null ? soundsEnabled : false;
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

	public List<WeightingRestriction> getWeightingRestrictions() {
		if (weightingRestrictions == null) {
			weightingRestrictions = new ArrayList<WeightingRestriction>();
		}
		return weightingRestrictions;
	}

	public void setWeightingRestrictions(List<WeightingRestriction> weightingRestrictions) {
		this.weightingRestrictions = weightingRestrictions;
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
}
