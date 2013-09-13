package ru.aplix.packline.hardware.scales.mera;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.scales.ScalesConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Scales")
public class RS232Configuration implements ScalesConfiguration {

    @XmlAttribute(name = "portName")
    private String portName;
    @XmlAttribute(name = "portSpeed")
    private int portSpeed;
    @XmlAttribute(name = "enabled")
    private boolean enabled;
    
    public RS232Configuration() {
		portName = "COM1";
		portSpeed = 14400;
		enabled = true;
	}

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public int getPortSpeed() {
        return portSpeed;
    }

    public void setPortSpeed(int portSpeed) {
        this.portSpeed = portSpeed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
