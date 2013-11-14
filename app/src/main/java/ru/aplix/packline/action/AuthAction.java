package ru.aplix.packline.action;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.AuthController;
import ru.aplix.packline.idle.WorkflowActionWithUserActivityMonitor;
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

		Operator result = postServicePort.getOperator(Utils.getMACAddress());
		if (result == null || result.getId() == null || result.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		getContext().setAttribute(Const.OPERATOR, result);
		return result;
	}
}
