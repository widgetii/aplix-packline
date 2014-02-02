package ru.aplix.packline.jws;

import java.io.Serializable;

public class InstallerPreferences implements Serializable {

	private static final long serialVersionUID = 2124987829416715547L;

	public String executionPath;
	public String updatesVersion;

	public String getExecutionPath() {
		return executionPath;
	}

	public void setExecutionPath(String executionPath) {
		this.executionPath = executionPath;
	}

	public String getUpdatesVersion() {
		return updatesVersion;
	}

	public void setUpdatesVersion(String updatesVersion) {
		this.updatesVersion = updatesVersion;
	}
}
