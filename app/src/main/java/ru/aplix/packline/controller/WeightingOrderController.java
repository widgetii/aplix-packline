package ru.aplix.packline.controller;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.animation.FadeTransitionBuilder;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.WeightingOrderAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesBundle;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WeightingOrderController extends StandardController<WeightingOrderAction> implements MeasurementListener {

	private final Log LOG = LogFactory.getLog(getClass());

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
	private float minStableWeight;

	private Scales<?> scales = null;

	private Task<Void> task;
	private boolean awaitingCompletion;

	private Transition transition;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		try {
			minStableWeight = Configuration.getInstance().getWeighting().getMinStableWeight();
		} catch (FileNotFoundException | MalformedURLException | JAXBException e) {
			e.printStackTrace();
		}

		transition = FadeTransitionBuilder.create().duration(Duration.millis(200)).node(nextButton).fromValue(1).toValue(0.6).cycleCount(Timeline.INDEFINITE)
				.autoReverse(true).build();
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);
		awaitingCompletion = false;

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

		// Check whether the weight has been stabilized before
		// we've get here
		Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		String barcode = (String) getContext().getAttribute(Const.BWL_BARCODE);
		if (StringUtils.equals(incoming.getId(), barcode)) {
			Float weight = (Float) getContext().getAttribute(Const.BWL_WEIGHT);
			if (weight != null) {
				getContext().setAttribute(Const.BWL_WEIGHT, null);

				onWeightStabled(weight);
			}
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (scales != null) {
			scales.removeMeasurementListener(this);
		}

		transition.stop();

		if (task != null) {
			task.cancel(false);
		}
	}

	public void nextClick(ActionEvent event) {
		if (!checkScales()) {
			return;
		}

		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().processMeasure(measure);
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
				return null;
			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
				nextButton.setDisable(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);

				awaitingCompletion = false;
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);

				WeightingOrderController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		awaitingCompletion = true;
	}

	private boolean checkScales() {
		if (measure <= 0f) {
			errorMessageProperty.set(getResources().getString("error.scales.negative.weight"));
			errorVisibleProperty.set(true);

			return false;
		}

		scales = (Scales<?>) getContext().getAttribute(Const.SCALES);
		if (scales != null && scales instanceof ScalesBundle && !((ScalesBundle<?>) scales).isNoMoreThanOneLoaded()) {
			errorMessageProperty.set(getResources().getString("error.scales.more.than.one"));
			errorVisibleProperty.set(true);

			return false;
		}

		return true;
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

				if (awaitingCompletion) {
					return;
				}

				if (value <= minStableWeight) {
					transition.play();
				} else {
					nextButton.fire();
				}
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
