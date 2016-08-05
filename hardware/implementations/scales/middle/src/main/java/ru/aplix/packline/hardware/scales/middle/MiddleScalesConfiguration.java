package ru.aplix.packline.hardware.scales.middle;

import ru.aplix.packline.hardware.scales.ScalesConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Configuration")
public class MiddleScalesConfiguration implements ScalesConfiguration {

	@XmlAttribute(name = "portName")
	private String portName;
	@XmlAttribute(name = "protocolName")
	private String protocolName;
	@XmlAttribute(name = "weighingPeriod")
	private Long weighingPeriod;
	@XmlAttribute(name = "connectionTimeout")
	private Long connectionTimeout;
	@XmlAttribute(name = "commandRetries")
	private Long commandRetries;
	@XmlAttribute(name = "receiveTimeout")
	private Long responseTimeout;
	@XmlAttribute(name = "dataDelay")
	private Long dataDelay;
	@XmlAttribute(name = "steadyWeightSamples")
	private Integer steadyWeightSamples;

	public MiddleScalesConfiguration() {
		portName = "COM1";
		protocolName = "Auto";
		weighingPeriod = 1L;
		connectionTimeout = null;
		commandRetries = null;
		responseTimeout = null;
		dataDelay = null;
		steadyWeightSamples = 5;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public String getProtocolName() {
		return protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.protocolName = protocolName;
	}

	public Long getWeighingPeriod() {
		return weighingPeriod;
	}

	public void setWeighingPeriod(Long weighingPeriod) {
		this.weighingPeriod = weighingPeriod;
	}

	public Long getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Long getCommandRetries() {
		return commandRetries;
	}

	public void setCommandRetries(Long commandRetries) {
		this.commandRetries = commandRetries;
	}

	public Long getResponseTimeout() {
		return responseTimeout;
	}

	public void setResponseTimeout(Long responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public Long getDataDelay() {
		return dataDelay;
	}

	public void setDataDelay(Long dataDelay) {
		this.dataDelay = dataDelay;
	}

	public Integer getSteadyWeightSamples() {
		return steadyWeightSamples;
	}

	public void setSteadyWeightSamples(Integer steadyWeightSamples) {
		this.steadyWeightSamples = steadyWeightSamples;
	}
}
