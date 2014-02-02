package ru.aplix.packline.jws;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.jnlp.UnavailableServiceException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class Installer extends BaseClass {

	private static final Log LOG = LogFactory.getLog(Installer.class);

	private static final String DISTRIBUTION_PACKAGE_NAME = "packline-distributive-package";

	private SystemInstallerService<InstallerPreferences> sis;
	private InstallerPreferences preferences;
	private String appExecutionCommand;

	public Installer() throws UnavailableServiceException {
		LOG.info("Installing application");
		this.sis = new SystemInstallerService<InstallerPreferences>(true);
		this.preferences = new InstallerPreferences();
		this.appExecutionCommand = System.getProperty("app.execute");
	}

	public void install() throws Exception {
		boolean installed = false;
		try {
			prepare();
			findAndInflate();
			save();

			installed = true;
		} finally {
			if (installed) {
				sis.installSucceeded(false);
			} else {
				sis.installFailed();
			}
		}
	}

	public void uninstall() throws Exception {
		LOG.info("Application has been uninstalled");
	}

	private void prepare() throws Exception {
		sis.setHeading("Installing application");
		sis.setStatus("Preparing install");
		sis.updateProgress(0);

		LOG.info(String.format("App code base: %s", sis.getCodeBase().toExternalForm()));
		LOG.info(String.format("App install path: %s", sis.getInstallPath()));
		LOG.info(String.format("App execute command: %s", appExecutionCommand));
	}

	private void findAndInflate() throws Exception {
		sis.setStatus("Looking for distribution package");
		sis.updateProgress(5);

		String locationPattern = String.format("jar:%s%s.jar!/*.zip", sis.getCodeBase().toExternalForm(), DISTRIBUTION_PACKAGE_NAME);
		PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		Resource[] resources = pmrpr.getResources(locationPattern);
		if (resources == null || resources.length == 0) {
			throw new Exception("Distribution package not found");
		}

		sis.setStatus("Inflating zip, please wait....");
		sis.updateProgress(10);

		for (Resource resource : resources) {
			LOG.info(String.format("Distribution package found: %s", resource.getURL()));
			inflateResource(resource, sis.getInstallPath());
		}

		sis.updateProgress(90);
	}

	private void inflateResource(Resource resource, String destDir) throws Exception {
		// Saving resource to a temporary file
		File temp = File.createTempFile(DISTRIBUTION_PACKAGE_NAME, null);
		temp.deleteOnExit();

		LOG.info(String.format("Copying distribution package to the temp file: %s", temp.getPath()));
		FileUtils.copyInputStreamToFile(resource.getInputStream(), temp);

		// Inflating zip to the destination directory
		LOG.info(String.format("Extracting package to: %s", sis.getInstallPath()));
		ZipFile zipFile = new ZipFile(temp);
		try {
			// Count total number of entries
			int count = 0;
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				entries.nextElement();
				count++;
			}

			// Enumarate all entries
			int i = 0;
			entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				i++;

				// If entry name matches execution command pattern,
				// remember it for launching the program
				if (entry.getName().endsWith(appExecutionCommand)) {
					preferences.setExecutionPath(sis.getInstallPath() + entry.getName());
				}

				// Update status and progress
				LOG.info(String.format("Extracting %s", entry.getName()));
				sis.setStatus(String.format("Extracting %s%s", sis.getInstallPath(), entry.getName()));
				sis.updateProgress(10 + 80 * i / count);

				// Extract entry to disk
				File entryDestination = new File(destDir, entry.getName());
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				} else {
					entryDestination.getParentFile().mkdirs();

					InputStream in = zipFile.getInputStream(entry);
					OutputStream out = new FileOutputStream(entryDestination);
					try {
						IOUtils.copy(in, out);
					} finally {
						IOUtils.closeQuietly(in);
						IOUtils.closeQuietly(out);
					}
				}
			}
		} finally {
			zipFile.close();
		}
		LOG.info("Extraction completed");
	}

	private void save() throws Exception {
		sis.setStatus("Saving preferences");
		sis.updateProgress(95);

		sis.writePreferences(preferences);

		sis.updateProgress(100);
	}

	public static void main(String[] args) {
		try {
			Installer i = new Installer();

			if (has(args, "uninstall")) {
				i.uninstall();
			} else {
				i.install();
			}
		} catch (Exception e) {
			LOG.error("Error while installing application", e);
		}
	}

	private static boolean has(String[] args, String valueOfInterest) {
		boolean result = false;
		for (String arg : args) {
			result = arg != null && arg.equalsIgnoreCase(valueOfInterest);
		}
		return result;
	}
}
