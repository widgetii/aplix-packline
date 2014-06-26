package ru.aplix.packline.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.WeightingOrderAction;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WeightingOrderController extends StandardController<WeightingOrderAction> implements MeasurementListener {

	@FXML
	private Label label1Caption;
	@FXML
	private Label label2Caption;
	@FXML
	private Label label3Caption;
	@FXML
	private Label label1Value;
	@FXML
	private Label label2Value;
	@FXML
	private Label label3Value;
	@FXML
	private Label weightLabel;
	@FXML
	private Button nextButton;

	private float measure = 0f;

	private Scales<?> scales = null;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		boolean valuesSet = false;
		Order order = (Order) context.getAttribute(Const.ORDER);
		if (order != null) {
			label1Caption.setText(getResources().getString("order.info.client"));
			label2Caption.setText(getResources().getString("order.info.delivery"));
			label3Caption.setText(getResources().getString("order.info.customer"));
			label1Value.setText(order.getClientName());
			label2Value.setText(order.getDeliveryMethod());
			label3Value.setText(order.getCustomer().getName());
			valuesSet = true;
		} else {
			Registry registry = (Registry) context.getAttribute(Const.REGISTRY);
			Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
			if (registry != null && incoming != null) {
				label1Caption.setText(getResources().getString("incoming.info.id"));
				label2Caption.setText(getResources().getString("incoming.info.description"));
				label3Caption.setText(getResources().getString("registry.info.customer"));
				label1Value.setText(incoming.getId());
				label2Value.setText(incoming.getContentDescription());
				label3Value.setText(registry.getCustomer().getName());
				valuesSet = true;
			}
		}

		if (!valuesSet) {
			label1Caption.setText(null);
			label2Caption.setText(null);
			label3Caption.setText(null);
			label1Value.setText(null);
			label2Value.setText(null);
			label3Value.setText(null);
		}

		scales = (Scales<?>) context.getAttribute(Const.SCALES);
		if (scales != null) {
			updateMeasure(scales.getLastMeasurement());
			scales.addMeasurementListener(this);
		} else {
			throw new SkipActionException();
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (scales != null) {
			scales.removeMeasurementListener(this);
		}
	}

	public void nextClick(ActionEvent event) {
		getAction().processMeasure(measure);
		done();
	}

	@Override
	public void onMeasure(final Float value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateMeasure(value);
			}
		});
	}

	@Override
	public void onWeightStabled(final Float value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateMeasure(value);
				nextButton.fire();
			}
		});
	}

	private void updateMeasure(Float value) {
		measure = value != null ? value : 0f;
		weightLabel.setText(String.format("%.3f", measure));
	}

	@Override
	protected boolean checkNoError() {
		if ((scales == null) || scales.isConnected()) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.scales"));
			errorVisibleProperty.set(true);

			return false;
		}
	}
}
