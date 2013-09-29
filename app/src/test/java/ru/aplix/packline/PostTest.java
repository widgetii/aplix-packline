package ru.aplix.packline;

import java.util.Map;

import javax.xml.ws.BindingProvider;

import junit.framework.TestCase;
import ru.aplix.packline.post.PackingLine;
import ru.aplix.packline.post.PackingLinePortType;

public class PostTest extends TestCase {

	public void testEcho() {
		String serviceAddress = "http://z.aplix.ru/post/ws/PackingLine.1cws";
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

		/*String response = postServicePort.echo(userName);
		assertNotNull(response);
		assertTrue(response.indexOf(userName) >= 0);*/
	}
}
