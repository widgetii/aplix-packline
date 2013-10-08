package ru.aplix.packline.action;

import java.util.Map;

import javax.xml.ws.BindingProvider;

import ru.aplix.packline.Const;
import ru.aplix.packline.controller.AuthController;
import ru.aplix.packline.idle.WorkflowActionWithUserActivityMonitor;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.PackingLinePortType;

public class AuthAction extends WorkflowActionWithUserActivityMonitor<AuthController> {

	@Override
	protected String getFormName() {
		return "auth";
	}

	public Operator authenticateOperator(String code) {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (postServicePort instanceof BindingProvider) {
			Map<String, Object> requestContext = ((BindingProvider) postServicePort).getRequestContext();
			requestContext.put(BindingProvider.USERNAME_PROPERTY, code);
		}

		Operator result = postServicePort.getOperator(code);
		getContext().setAttribute(Const.OPERATOR, result);
		return result;
	}
}
