package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;

import junit.framework.TestCase;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

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

	public void testController() throws Exception {
		HttpPost httppost = new HttpPost("http://localhost:8080/packline/controller/add");

		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("parcelId", "1234578"));
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		// Execute and get the response.
		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpResponse response = httpclient.execute(httppost);

		int responseCode = response.getStatusLine().getStatusCode();
		if (HttpURLConnection.HTTP_OK != responseCode && HttpURLConnection.HTTP_MOVED_TEMP != responseCode) {
			throw new Exception(response.getStatusLine().getReasonPhrase());
		}
	}
}
