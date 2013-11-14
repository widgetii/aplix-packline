package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;

import junit.framework.TestCase;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PostService;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.PackingLine;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.Utils;

public class PostTestManual extends TestCase {

	public void testService() throws FileNotFoundException, MalformedURLException, JAXBException, UnknownHostException, SocketException {
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

		Operator operator = postServicePort.getOperator(Utils.getMACAddress());
		assertNotNull(operator);
		assertTrue(operator.getName() != null && operator.getName().length() > 0);
	}
}
