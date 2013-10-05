package ru.aplix.packline;

import java.util.Map;

import javax.xml.ws.BindingProvider;

import junit.framework.TestCase;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.PackingLine;
import ru.aplix.packline.post.PackingLinePortType;

public class PostTestManual extends TestCase {

	public void testService() {
		//String serviceAddress = "http://z.aplix.ru/post/ws/PackingLine.1cws";
		String serviceAddress = "http://localhost:8080/mock-post-service-0.1/PackingLine?WSDL";
		String userName = "4627085462743";
		String password = "P8HLCtoOey";

		PackingLine postService = new PackingLine();
		assertNotNull(postService);

		PackingLinePortType postServicePort = postService.getPackingLineSoap();
		assertNotNull(postServicePort);

		if (postServicePort instanceof BindingProvider) {
			Map<String, Object> requestContext = ((BindingProvider) postServicePort).getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceAddress);
			requestContext.put(BindingProvider.USERNAME_PROPERTY, userName);
			requestContext.put(BindingProvider.PASSWORD_PROPERTY, password);
		}

		Operator operator = postServicePort.getOperator(userName);
		assertNotNull(operator);
		assertTrue(userName.equals(operator.getId()));
		assertTrue(operator.getName() != null && operator.getName().length() > 0);
	}
}
