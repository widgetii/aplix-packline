package ru.aplix.packline.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.PrintFormsAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.workflow.WorkflowContext;

public class PrintFormsController extends StandardController<PrintFormsAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private GridPane reprintContainer;
	@FXML
	private Button weightingButton;
	@FXML
	private Button reprintButton1;
	@FXML
	private Button reprintButton2;
	@FXML
	private Button reprintButton3;
	@FXML
	private Button reprintButton4;

	private BarcodeScanner<?> barcodeScanner = null;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private Task<?> task;

	public PrintFormsController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		getAction().prepare();

		assignButtons();

		reprintContainer.setDisable(true);
		weightingButton.setDisable(true);

		Object scales = context.getAttribute(Const.SCALES);
		weightingButton.setVisible(scales != null);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
			barcodeChecker.playFromStart();
		}

		task = new PrintTask(new Button[] { reprintButton1, reprintButton2, reprintButton3, reprintButton4 }, false);
		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void assignButtons() {
		try {
			List<PrintForm> forms = Configuration.getInstance().getPrintForms();
			assignButton(reprintButton1, 0, forms);
			assignButton(reprintButton2, 1, forms);
			assignButton(reprintButton3, 2, forms);
			assignButton(reprintButton4, 3, forms);
		} catch (Exception e) {
			LOG.error(null, e);
		}
	}

	private void assignButton(Button button, int formIndex, List<PrintForm> forms) {
		if (formIndex < forms.size()) {
			PrintForm form = forms.get(formIndex);
			button.setUserData(form);
			button.setText(String.format(getResources().getString("button.reprint.form"), form.getName()));
		} else {
			button.setUserData(null);
			button.setDisable(true);
			button.setVisible(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		reprintContainer.setDisable(value);
		weightingButton.setDisable(value);
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
				processBarcode(value);
			}
		});
	}

	public void weightingClick(ActionEvent event) {
		getAction().setNextAction(getAction().getWeightingAction());
		done();
	}

	public void reprintClick(ActionEvent event) {
		task = new PrintTask(new Button[] { (Button) event.getSource() }, true);
		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					return getAction().processBarcode(value);
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				setProgress(true);
			}

			@Override
			protected void failed() {
				super.failed();

				setProgress(false);

				barcodeCheckerEventHandler.reset();

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

				setProgress(false);

				if (getValue()) {
					PrintFormsController.this.done();
				} else {
					barcodeCheckerEventHandler.reset();

					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	/**
	 * 
	 */
	private class PrintTask extends Task<Void> {

		private Container container;
		private Button[] buttons;
		private boolean skipAutoPrint;

		public PrintTask(Button[] buttons, boolean skipAutoPrint) {
			super();
			this.buttons = buttons;
			this.skipAutoPrint = skipAutoPrint;

			container = (Container) getContext().getAttribute(Const.TAG);
		}

		@Override
		public Void call() throws Exception {
			long t = System.currentTimeMillis();

			List<Throwable> exceptions = new ArrayList<Throwable>();
			boolean printedAtLeastOne = false;
			for (Button button : buttons) {
				try {
					if (printLikeButton(button)) {
						printedAtLeastOne = true;
					}
				} catch (Throwable e) {
					LOG.error(null, e);
					exceptions.add(e);
				}
			}

			t = System.currentTimeMillis() - t;
			if (printedAtLeastOne) {
				LOG.info(String.format("Printing time: %.1f sec", (float) t / 1000f));
			}

			if (exceptions.size() == 1) {
				throw new PackLineException(exceptions.get(0));
			} else if (exceptions.size() > 1) {
				StringBuffer sb = new StringBuffer();
				for (Throwable e : exceptions) {
					sb.append(e.getMessage());
					sb.append("\n");
				}
				throw new PackLineException(sb.toString());
			}

			return null;
		}

		private boolean printLikeButton(Button button) throws Exception {
			PrintForm printForm = (PrintForm) button.getUserData();
			if (printForm != null && (printForm.getAutoPrint() || skipAutoPrint)) {
				getAction().printForms(container.getId(), printForm);
				return true;
			}
			return false;
		}

		@Override
		protected void running() {
			super.running();

			infoLabel.setText(getResources().getString("printing.info1"));
			setProgress(true);
		}

		@Override
		protected void failed() {
			super.failed();

			setProgress(false);

			barcodeCheckerEventHandler.reset();

			String error = getException().getMessage() != null ? getException().getMessage() : getException().getClass().getSimpleName();
			errorMessageProperty.set(error);
			errorVisibleProperty.set(true);
		}

		@Override
		protected void succeeded() {
			super.succeeded();

			infoLabel.setText(getResources().getString("printing.info2"));
			setProgress(false);
		}
	};

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
