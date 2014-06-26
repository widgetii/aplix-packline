package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PostService")
public class PostService {

	@XmlElement(name = "ServiceAddress")
	private String serviceAddress;
	@XmlElement(name = "UserName")
	private String userName;
	@XmlElement(name = "Password")
	private String password;
	@XmlElement(name = "RemoteStoragePath")
	private String remoteStoragePath;

	public PostService() {
		serviceAddress = "";
		userName = "";
		password = "";
		remoteStoragePath = "";
	}

	public String getServiceAddress() {
		return serviceAddress;
	}

	public void setServiceAddress(String serviceAddress) {
		this.serviceAddress = serviceAddress;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRemoteStoragePath() {
		return remoteStoragePath;
	}

	public void setRemoteStoragePath(String remoteStoragePath) {
		this.remoteStoragePath = remoteStoragePath;
	}
}
