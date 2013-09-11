package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ru.aplix.packline.utils.StringXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Hardware")
public class HardwareConfiguration {

    @XmlElement(name = "BarcodeScanner")
    @XmlJavaTypeAdapter(StringXmlAdapter.class)
    private String barcodeScanner;

    @XmlElement(name = "PhotoCamera")
    @XmlJavaTypeAdapter(StringXmlAdapter.class)
    private String photoCamera;

    @XmlElement(name = "Scales")
    @XmlJavaTypeAdapter(StringXmlAdapter.class)
    private String scales;

    public String getBarcodeScanner() {
        return barcodeScanner;
    }

    public void setBarcodeScanner(String value) {
        this.barcodeScanner = value;
    }

    public String getPhotoCamera() {
        return photoCamera;
    }

    public void setPhotoCamera(String photoCamera) {
        this.photoCamera = photoCamera;
    }

    public String getScales() {
        return scales;
    }

    public void setScales(String scales) {
        this.scales = scales;
    }
}
