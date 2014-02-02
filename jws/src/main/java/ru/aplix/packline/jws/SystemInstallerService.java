package ru.aplix.packline.jws;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import javax.jnlp.BasicService;
import javax.jnlp.ExtensionInstallerService;
import javax.jnlp.FileContents;
import javax.jnlp.PersistenceService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;

public class SystemInstallerService<T extends Serializable> {

	private static final String INSTALL_PATH_SUFFIX = "Aplix/";
	private static final long PERSISTENCE_MAX_SIZE = 10240L;

	private BasicService basicService;
	private ExtensionInstallerService extInstService;
	private PersistenceService persistenceService;

	private String installPath = null;

	public SystemInstallerService(boolean extended) throws UnavailableServiceException {
		basicService = (BasicService) ServiceManager.lookup(BasicService.class.getName());
		persistenceService = (PersistenceService) ServiceManager.lookup(PersistenceService.class.getName());
		if (extended) {
			extInstService = (ExtensionInstallerService) ServiceManager.lookup(ExtensionInstallerService.class.getName());
		}
	}

	public String getInstallPath() {
		if (installPath == null) {
			String basicPath;
			if (SystemUtils.IS_OS_WINDOWS) {
				basicPath = FilenameUtils.normalize(System.getenv("ProgramFiles"), true);
			} else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) {
				basicPath = "/Applications/";
			} else {
				basicPath = "/opt/";
			}

			if (basicPath == null || basicPath.length() == 0) {
				basicPath = "";
			} else {
				if (basicPath.charAt(basicPath.length() - 1) != '/') {
					basicPath += "/";
				}
			}

			installPath = String.format("%s%s", basicPath, INSTALL_PATH_SUFFIX);
		}
		return installPath;
	}

	public URL getCodeBase() throws MalformedURLException {
		return basicService.getCodeBase();
	}

	public void setHeading(String heading) {
		if (extInstService != null) {
			extInstService.setHeading(heading);
		}
	}

	public void setStatus(String status) {
		if (extInstService != null) {
			extInstService.setStatus(status);
		}
	}

	public void updateProgress(int value) {
		if (extInstService != null) {
			extInstService.updateProgress(value);
		}
	}

	public void installSucceeded(boolean needsReboot) {
		if (extInstService != null) {
			extInstService.installSucceeded(needsReboot);
		}
	}

	public void installFailed() {
		if (extInstService != null) {
			extInstService.installFailed();
		}
	}

	@SuppressWarnings("unchecked")
	public T readPreferences() throws IOException, ClassNotFoundException {
		FileContents fc;
		try {
			fc = persistenceService.get(getCodeBase());
		} catch (FileNotFoundException fnfe) {
			persistenceService.create(getCodeBase(), PERSISTENCE_MAX_SIZE);
			fc = persistenceService.get(getCodeBase());
		}

		ObjectInputStream ois = new ObjectInputStream(fc.getInputStream());
		try {
			return (T) ois.readObject();
		} finally {
			ois.close();
		}
	}

	public void writePreferences(T object) throws MalformedURLException, FileNotFoundException, IOException {
		FileContents fc;
		try {
			fc = persistenceService.get(getCodeBase());
		} catch (FileNotFoundException fnfe) {
			persistenceService.create(getCodeBase(), PERSISTENCE_MAX_SIZE);
			fc = persistenceService.get(getCodeBase());
		}

		ObjectOutputStream oos = new ObjectOutputStream(fc.getOutputStream(true));
		try {
			oos.writeObject(object);
			oos.flush();
		} finally {
			oos.close();
		}
	}
}
