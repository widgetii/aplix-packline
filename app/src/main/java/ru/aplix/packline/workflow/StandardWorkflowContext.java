package ru.aplix.packline.workflow;

import java.util.HashMap;
import java.util.Map;

public class StandardWorkflowContext implements WorkflowContext {

	private Map<String, Object> context;

	public StandardWorkflowContext() {
		this.context = new HashMap<String, Object>();
	}

	public StandardWorkflowContext(Map<String, Object> parameters) {
		if (parameters == null) {
			this.context = new HashMap<String, Object>();
		} else {
			this.context = parameters;
		}
	}

	@Override
	public synchronized Object getAttribute(String name) {
		return context.get(name);
	}

	@Override
	public synchronized void setAttribute(String name, Object value) {
		context.put(name, value);
	}
}
