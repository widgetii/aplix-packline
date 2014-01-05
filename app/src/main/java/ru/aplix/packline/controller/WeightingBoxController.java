package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.WeightingBoxAction;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingSize;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WeightingBoxController extends StandardController<WeightingBoxAction> implements MeasurementListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label packingCode;
	@FXML
	private Label packingType;
	@FXML
	private Label packingSize;
	@FXML
	private Label weightLabel;
	@FXML
	private Button nextButton;

	private float measure = 0f;

	private Scales<?> scales = null;

	private Task<Void> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);

		Container container = (Container) context.getAttribute(Const.TAG);
		if (container != null) {
			packingCode.setText(container.getId());

			PackingSize ps = container.getPackingSize();
			if (ps != null) {
				packingSize.setText(String.format("%.1f x %.1f x %.1f %s", ps.getLength(), ps.getHeight(), ps.getWidth(),
						getResources().getString("dimentions.unit")));
			}

			PackingType pt = container.getPackingType();
			if (pt != null) {
				switch (pt) {
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
			}
		}

		scales = (Scales<?>) context.getAttribute(Const.SCALES);
		if (scales != null) {
			updateMeasure(scales.getLastMeasurement());
			scales.addMeasurementListener(this);
		} else {
			getAction().setNextAction(getAction().getPrintingAction());
			throw new SkipActionException();
		}
	}

	@Override
	public void terminate() {
		super.terminate();

		if (scales != null) {
			scales.removeMeasurementListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	public void nextClick(ActionEvent event) {
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
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);

				WeightingBoxController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
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
		if ((scales != null) && scales.isConnected()) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.scales"));
			errorVisibleProperty.set(true);

			return false;
		}
	}
}
