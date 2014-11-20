package ru.aplix.packline.action;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.AuthController;
import ru.aplix.packline.idle.WorkflowActionWithUserActivityMonitor;
import ru.aplix.packline.post.GetOperatorResponse2;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.Utils;

public class AuthAction extends WorkflowActionWithUserActivityMonitor<AuthController> {

	@Override
	protected String getFormName() {
		return "auth";
	}

	public Operator authenticateOperator(String code) throws PackLineException, UnknownHostException, SocketException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (postServicePort instanceof BindingProvider) {
			Map<String, Object> requestContext = ((BindingProvider) postServicePort).getRequestContext();
			requestContext.put(BindingProvider.USERNAME_PROPERTY, code);
		}

		GetOperatorResponse2 result = null;
		try {
			result = postServicePort.getOperator2(Utils.getMACAddress());
		} catch (WebServiceException wse) {
			String msg = wse.getMessage() != null ? wse.getMessage() : "";
			if (msg.indexOf("HTTP status code 401") < 0) {
				throw wse;
			}
		}
		if (result.getError() != null && result.getError().length() > 0) {
			throw new PackLineException(result.getError());
		} else if (result == null || result.getOperator() == null || result.getOperator().getId() == null || result.getOperator().getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.auth.invalid.code"));
		}

		getContext().setAttribute(Const.OPERATOR, result.getOperator());
		return result.getOperator();
	}
}
