package ru.aplix.packline.action;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.context.ApplicationContext;

import ru.aplix.packline.Const;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.utils.AsyncCommandRunner;
import ru.aplix.packline.workflow.StandardWorkflowController;

public abstract class NotificationAction<Controller extends StandardWorkflowController<?>> extends CommonAction<Controller> {

	protected void notifyAboutIncomingParcel(String parcelId) {
		sendNotification(new ControllerParcelPost(parcelId, true));
	}

	protected void notifyAboutOutgoingParcel(String parcelId) {
		sendNotification(new ControllerParcelPost(parcelId, false));
	}

	private void sendNotification(Runnable runnable) {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		AsyncCommandRunner acr = (AsyncCommandRunner) applicationContext.getBean(Const.ASYNC_COMMAND_RUNNER);
		acr.exec(runnable);
	}

	/**
	 * 
	 */
	private class ControllerParcelPost implements Runnable {

		private final Log LOG = LogFactory.getLog(getClass());

		private String parcelId;
		private boolean add;

		public ControllerParcelPost(String parcelId, boolean add) {
			this.parcelId = parcelId;
			this.add = add;
		}

		@Override
		public void run() {
			try {
				if (!Configuration.getInstance().getHardwareConfiguration().getController().isEnabled()) {
					return;
				}
				
				// Request parameters and other properties.
				List<NameValuePair> params = new ArrayList<NameValuePair>(1);
				params.add(new BasicNameValuePair("parcelId", parcelId));

				String address = Configuration.getInstance().getHardwareConfiguration().getController().getUrl();
				String url = String.format("%s%s", address, add ? "add" : "remove");

				HttpPost httppost = new HttpPost(url);
				httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

				// Execute and get the response.
				HttpClient httpclient = HttpClientBuilder.create().build();
				HttpResponse response = httpclient.execute(httppost);

				int responseCode = response.getStatusLine().getStatusCode();
				if (HttpURLConnection.HTTP_OK != responseCode && HttpURLConnection.HTTP_MOVED_TEMP != responseCode) {
					throw new Exception(response.getStatusLine().getReasonPhrase());
				}
			} catch (Exception e) {
				LOG.error(null, e);
			}
		}
	}
}
