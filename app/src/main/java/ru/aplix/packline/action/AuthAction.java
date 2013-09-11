package ru.aplix.packline.action;

import ru.aplix.packline.controller.AuthController;
import ru.aplix.packline.idle.WorkflowActionWithUserActivityMonitor;
import ru.aplix.packline.model.Operator;

public class AuthAction extends WorkflowActionWithUserActivityMonitor<AuthController> {

	@Override
	protected String getFormName() {
		return "auth";
	}

	public Operator authenticateOperator(String code) {
		// TODO: place authentication code here
		if ("0036000291452".equals(code)) {
			Operator operator = new Operator();
			operator.setId(1L);
			operator.setName("Василий Теркин");
			operator.setCode(code);
			return operator;
		} else if ("8007141009277".equals(code)) {
			Operator operator = new Operator();
			operator.setId(2L);
			operator.setName("Михаил Лихачев");
			operator.setCode(code);
			return operator;
		} else if ("9780140318296".equals(code)) {
			Operator operator = new Operator();
			operator.setId(3L);
			operator.setName("Иван Фёдорович Крузенштерн");
			operator.setCode(code);
			return operator;
		} else {
			return null;
		}
	}
}
