package ru.aplix.packline.utils;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class StringXmlAdapter extends XmlAdapter<Object, String> {

	@Override
	public Object marshal(String v) throws Exception {
		throw new Exception("Not Implemented");
	}

	@Override
	public String unmarshal(Object v) throws Exception {
		Node node = ((Node) v);
		Document document = node.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		String str = serializer.writeToString(node);
		return normalizeXml(str);
	}

	private String normalizeXml(String xml) {
		int beginIndex = xml.lastIndexOf("?>");
		if (beginIndex > -1) {
			beginIndex += 2;
		} else {
			beginIndex = 0;
		}
		return xml.substring(beginIndex).trim();
	}
}