package ru.aplix.packline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.WeightingOrderAction;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WeightingOrderController extends StandardController<WeightingOrderAction> implements MeasurementListener {

	@FXML
	private Label clientLabel;
	@FXML
	private Label deliveryLabel;
	@FXML
	private Label customerLabel;
	@FXML
	private Label weightLabel;
	@FXML
	private Button nextButton;

	private float measure = 0f;

	private Scales<?> scales = null;
	private Timeline scalesChecker;
	private ScalesCheckerEventHandler scalesCheckerEventHandler;

	public WeightingOrderController() {
		scalesCheckerEventHandler = new ScalesCheckerEventHandler();

		scalesChecker = new Timeline();
		scalesChecker.setCycleCount(Timeline.INDEFINITE);
		scalesChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), scalesCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);
		
		Order order = (Order) context.getAttribute(Const.ORDER);
		if (order != null) {
			clientLabel.setText(order.getClientName());
			deliveryLabel.setText(order.getDeliveryMethod());
			customerLabel.setText(order.getCustomer().getName());
		}

		scales = (Scales<?>) context.getAttribute(Const.SCALES);
		if (scales != null) {
			updateMeasure(scales.getLastMeasurement());
			scales.addMeasurementListener(this);
			scalesChecker.playFromStart();
		} else {
			throw new SkipActionException();
		}
	}

	@Override
	public void terminate() {
		if (scales != null) {
			scalesChecker.stop();
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

	/**
	 *
	 */
	private class ScalesCheckerEventHandler implements EventHandler<ActionEvent> {

		private int delayCount;
		private String errorStr;

		public ScalesCheckerEventHandler() {
			reset();
		}

		@Override
		public void handle(ActionEvent event) {
			if (delayCount <= 1) {
				if ((scales != null) && scales.isConnected()) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);
				} else {
					if (errorStr == null) {
						errorStr = WeightingOrderController.this.getResources().getString("error.scales");
					}

					errorMessageProperty.set(errorStr);
					errorVisibleProperty.set(true);
				}
			} else {
				delayCount--;
			}
		}

		public void reset() {
			delayCount = Const.ERROR_DISPLAY_DELAY;
		}
	}
}
