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
		if ("4627085462743".equals(code)) {
			Operator operator = new Operator();
			operator.setId(1L);
			operator.setName("Ильин ДВ");
			operator.setCode(code);
			return operator;
		} else if ("4627085462750".equals(code)) {
			Operator operator = new Operator();
			operator.setId(2L);
			operator.setName("Палагин СА");
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
