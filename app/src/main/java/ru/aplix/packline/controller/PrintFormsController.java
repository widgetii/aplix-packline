package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.PrintFormsAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.workflow.WorkflowContext;

public class PrintFormsController extends StandardController<PrintFormsAction> implements BarcodeListener {

	@FXML
	private Label infoLabel;
	@FXML
	private GridPane reprintContainer;
	@FXML
	private Button nextButton;

	private BarcodeScanner<?> barcodeScanner = null;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private Task<Void> task;

	public PrintFormsController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		reprintContainer.setDisable(true);
		nextButton.setDisable(true);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
			barcodeChecker.playFromStart();
		}

		task = new Task<Void>() {
			@Override
			public Void call() {
				try {
					int max = 50;
					for (int i = 1; i <= max; i++) {
						if (isCancelled()) {
							break;
						}
						updateProgress(i, max);
						Thread.sleep(100);
					}
				} catch (InterruptedException ie) {
				}
				return null;
			}
		};

		task.setOnRunning(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent arg0) {
				infoLabel.setText(getResources().getString("printing.info1"));
				progressVisibleProperty.set(true);
				reprintContainer.setDisable(true);
				nextButton.setDisable(true);
			}
		});

		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent arg0) {
				infoLabel.setText(getResources().getString("printing.info2"));
				progressVisibleProperty.set(false);
				reprintContainer.setDisable(false);
				nextButton.setDisable(false);
			}
		});

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	@Override
	public void terminate() {
		if (barcodeScanner != null) {
			barcodeChecker.stop();
			barcodeScanner.removeBarcodeListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (!progressVisibleProperty.get()) {
					getContext().setAttribute(Const.JUST_SCANNED_BARCODE, value);
					done();
				}
			}
		});
	}

	public void nextClick(ActionEvent event) {
		done();
	}

	/**
	 *
	 */
	private class BarcodeCheckerEventHandler implements EventHandler<ActionEvent> {

		private int delayCount;
		private String errorStr;

		public BarcodeCheckerEventHandler() {
			reset();
		}

		@Override
		public void handle(ActionEvent event) {
			if (delayCount <= 1) {
				if ((barcodeScanner != null) && barcodeScanner.isConnected()) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);
				} else {
					if (errorStr == null) {
						errorStr = PrintFormsController.this.getResources().getString("error.barcode.scanner");
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
