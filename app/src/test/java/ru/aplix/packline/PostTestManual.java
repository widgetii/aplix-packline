package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;

import junit.framework.TestCase;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PostService;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.PackingLine;
import ru.aplix.packline.post.PackingLinePortType;

public class PostTestManual extends TestCase {

	public void testService() throws FileNotFoundException, MalformedURLException, JAXBException {
		PostService psConf = Configuration.getInstance().getPostService();

		PackingLine postService = new PackingLine();
		assertNotNull(postService);

		PackingLinePortType postServicePort = postService.getPackingLineSoap();
		assertNotNull(postServicePort);

		if (postServicePort instanceof BindingProvider) {
			Map<String, Object> requestContext = ((BindingProvider) postServicePort).getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, psConf.getServiceAddress());
			requestContext.put(BindingProvider.USERNAME_PROPERTY, psConf.getUserName());
			requestContext.put(BindingProvider.PASSWORD_PROPERTY, psConf.getPassword());
		}

		Operator operator = postServicePort.getOperator();
		assertNotNull(operator);
		assertTrue(psConf.getUserName().equals(operator.getId()));
		assertTrue(operator.getName() != null && operator.getName().length() > 0);
	}
}
