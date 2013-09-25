package ru.aplix.packline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.WeightingBoxAction;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WeightingBoxController extends StandardController<WeightingBoxAction> implements MeasurementListener {

	@FXML
	private Label packingCode;
	@FXML
	private Label packingType;
	@FXML
	private Label packingSize;
	@FXML
	private Label weightLabel;

	private float measure = 0f;

	private Scales<?> scales = null;
	private Timeline scalesChecker;
	private ScalesCheckerEventHandler scalesCheckerEventHandler;

	public WeightingBoxController() {
		scalesCheckerEventHandler = new ScalesCheckerEventHandler();

		scalesChecker = new Timeline();
		scalesChecker.setCycleCount(Timeline.INDEFINITE);
		scalesChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), scalesCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		Order order = (Order) context.getAttribute(Const.ORDER);

		packingCode.setText(order.getPacking().getPackingCode());
		packingSize.setText(String.format("%s %s", order.getPacking().getPackingSize().toString(), getResources().getString("dimentions.unit")));
		switch (order.getPacking().getPackingType()) {
		case BOX:
			packingType.setText(getResources().getString("packtype.box"));
			break;
		case PACKET:
			packingType.setText(getResources().getString("packtype.packet"));
			break;
		case PAPER:
			packingType.setText(getResources().getString("packtype.paper"));
			break;
		}

		errorMessageProperty.set(null);
		errorVisibleProperty.set(false);

		updateMeasure(0f);

		scales = (Scales<?>) context.getAttribute(Const.SCALES);
		if (scales != null) {
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
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		getAction().processMeasure(measure, order);
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

	private void updateMeasure(Float value) {
		measure = value;
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
						errorStr = WeightingBoxController.this.getResources().getString("error.scales");
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
