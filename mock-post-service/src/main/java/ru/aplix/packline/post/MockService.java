package ru.aplix.packline.post;

import java.util.List;

import javax.jws.WebService;

@WebService(name = "MockPostService", serviceName = "PackingLine", portName = "PackingLineSoap", endpointInterface = "ru.aplix.packline.post.PackingLinePortType", wsdlLocation = "WEB-INF/wsdl/PackingLine.1cws.wsdl", targetNamespace = "http://www.aplix.ru/PackingLine/1.0/ws")
public class MockService implements PackingLinePortType {

	@Override
	public Operator getOperator(String operatorId) {
		Operator operator = new Operator();
		operator.setId(operatorId);
		operator.setName("John Smith");
		return operator;
	}

	@Override
	public void setOperatorActivity(String operatorId, boolean isActive) {
	}

	@Override
	public Tag findTag(String tagId) {
		return null;
	}

	@Override
	public int addIncomingToOrder(String orderId, Incoming incoming) {
		return 0;
	}

	@Override
	public boolean deleteIncomingFromOrder(String orderId, String incomingId) {
		return false;
	}

	@Override
	public boolean carryOutOrder(String orderId) {
		return false;
	}

	@Override
	public boolean deleteOrder(String orderId) {
		return false;
	}

	@Override
	public boolean addContainer(Container container) {
		return false;
	}

	@Override
	public boolean updateContainer(Container container) {
		return false;
	}

	@Override
	public void setBoxSize(List<Tag> tags, PackingSize packingSize) {
	}

	@Override
	public PackingSize getBoxSize(String boxId) {
		return null;
	}

	@Override
	public int getBoxCount(PackingSize packingSize) {
		return 0;
	}

	@Override
	public List<Tag> generateTags(int count) {
		return null;
	}

	@Override
	public List<Field> gatherInfo(List<String> fields) {
		return null;
	}
}
