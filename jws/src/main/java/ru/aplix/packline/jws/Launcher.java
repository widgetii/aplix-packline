package ru.aplix.packline.jws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jnlp.UnavailableServiceException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Launcher extends BaseClass {

	private static final Log LOG = LogFactory.getLog(Installer.class);

	private static final String UPDATES_DIR = "updates";
	private static final String VERSION_FILE = "version";

	private SystemInstallerService<InstallerPreferences> sis;
	private InstallerPreferences preferences;
	private String macAddr;

	public Launcher() throws UnavailableServiceException {
		LOG.info("Launching application");
		this.sis = new SystemInstallerService<InstallerPreferences>(false);
	}

	public void go() throws ClassNotFoundException, IOException, InterruptedException {
		LOG.info("Reading preferences");
		preferences = sis.readPreferences();
		macAddr = getMACAddress();

		List<RemoteFile> files = new ArrayList<RemoteFile>();
		String relativeUrl = findAdressUpdates(files, false);
		if (relativeUrl != null && files != null && files.size() > 0) {
			downloadUpdates(relativeUrl, files);

			LOG.info("Writing preferences");
			sis.writePreferences(preferences);
		}

		startMainProgram();
	}

	private void startMainProgram() throws IOException, InterruptedException {
		LOG.info(String.format("Executing command: %s", preferences.getExecutionPath()));
		if (SystemUtils.IS_OS_WINDOWS) {
			Runtime.getRuntime().exec(String.format("\"%s\"", preferences.getExecutionPath()));
		} else if (SystemUtils.IS_OS_UNIX) {
			LOG.info("Granting permissions first");
			Process p = Runtime.getRuntime().exec(new String[] { "chmod", "+x", preferences.getExecutionPath() });
			p.waitFor();

			Runtime.getRuntime().exec(preferences.getExecutionPath());
		} else {
			Runtime.getRuntime().exec(preferences.getExecutionPath());
		}
	}

	private void downloadUpdates(String relativeUrl, List<RemoteFile> files) throws IOException {
		LOG.info(String.format("Copying files to %s", sis.getInstallPath()));
		for (RemoteFile file : files) {
			if (file.directory) {
				continue;
			}

			LOG.info("Copying: " + file.fileName);

			URL httpUrl = new URL(relativeUrl + file.fileName);
			URLConnection connection = httpUrl.openConnection();

			File f = new File(sis.getInstallPath(), file.fileName);
			FileUtils.copyInputStreamToFile(connection.getInputStream(), f);
		}
	}

	private String findAdressUpdates(List<RemoteFile> files, boolean raiseOnError) throws IOException {
		try {
			String url = String.format("%s%s/%s/", sis.getCodeBase(), UPDATES_DIR, macAddr);
			LOG.info(String.format("Checking updates at %s", url));

			// Checking version
			if (!checkVersion(url)) {
				return url;
			}

			// Find all updates
			findFiles(url, "", files, null);

			if (files.size() > 0) {
				for (RemoteFile file : files) {
					if (!file.directory) {
						LOG.info("Found: " + file.fileName);
					}
				}
			} else {
				LOG.info("Updates not found");
			}

			return url;
		} catch (ConnectException ce) {
			LOG.warn("Server is not available");
		} catch (IOException e) {
			LOG.error("Error while checking updates", e);
			if (raiseOnError) {
				throw e;
			}
		}
		return null;
	}

	private boolean checkVersion(String url) throws IOException {
		try {
			URL httpUrl = new URL(url + VERSION_FILE);
			URLConnection connection = httpUrl.openConnection();
			String version = IOUtils.toString(connection.getInputStream());
			if (version.equals(preferences.getUpdatesVersion())) {
				LOG.info("Updates version is the same, skipping");
				return false;
			} else {
				LOG.info("Updates version is different, going further");
				preferences.setUpdatesVersion(version);
			}
		} catch (HttpStatusException hse) {
			switch (hse.getStatusCode()) {
			case HttpURLConnection.HTTP_NOT_FOUND:
				break;
			default:
				throw hse;
			}
		} catch (FileNotFoundException fnf) {
		}
		return true;
	}

	private void findFiles(String url, String node, List<RemoteFile> files, Pattern pattern) throws IOException {
		try {
			Connection con = Jsoup.connect(String.format("%s/%s", url, node));
			Document doc = con.get();

			String server = con.response().headers().get("Server");
			if (server != null && server.contains("Apache")) {
				// Parse Apache directory listing page
				Elements elements = doc.select("td a[href]");

				// Start from 1 because first item is a link to parent directory
				for (int i = 1; i < elements.size(); i++) {
					Element element = elements.get(i);

					RemoteFile rf = new RemoteFile();
					rf.fileName = node + element.attr("href");

					if (!files.contains(rf)) {
						files.add(rf);

						if (rf.fileName.endsWith("/")) {
							rf.directory = true;
							findFiles(url, rf.fileName, files, pattern);
						}
					}
				}
			} else if (server != null && server.contains("IIS")) {
				// Parse IIS directory listing page
				Elements elements = doc.select("a[href]");

				if (pattern == null) {
					pattern = Pattern.compile(String.format(".+/%s/%s/(.+)", UPDATES_DIR, macAddr));
				}

				// Start from 1 because first item is a link to parent directory
				for (int i = 1; i < elements.size(); i++) {
					Element element = elements.get(i);
					String file = element.attr("href");

					Matcher matcher = pattern.matcher(file);
					if (matcher.matches() && matcher.groupCount() == 1) {
						RemoteFile rf = new RemoteFile();
						rf.fileName = matcher.group(1);

						if (!files.contains(rf)) {
							files.add(rf);

							if (rf.fileName.endsWith("/")) {
								rf.directory = true;
								findFiles(url, rf.fileName, files, pattern);
							}
						}
					}
				}
			}
		} catch (HttpStatusException hse) {
			switch (hse.getStatusCode()) {
			case HttpURLConnection.HTTP_NOT_FOUND:
				break;
			default:
				throw hse;
			}
		}
	}

	private String getMACAddress() throws UnknownHostException, SocketException {
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

	/**
	 * Main function.
	 * 
	 * @param args
	 *            command arguments
	 */
	public static void main(String[] args) {
		try {
			Launcher l = new Launcher();
			l.go();
		} catch (Exception e) {
			LOG.error("Error while launching application", e);
		}
	}

	/**
	 * 
	 */
	private class RemoteFile {

		String fileName;
		boolean directory;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RemoteFile other = (RemoteFile) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (fileName == null) {
				if (other.fileName != null)
					return false;
			} else if (!fileName.equals(other.fileName))
				return false;
			return true;
		}

		private Launcher getOuterType() {
			return Launcher.this;
		}
	}
}