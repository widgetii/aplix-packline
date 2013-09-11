package ru.aplix.packline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.DimentionsAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.workflow.WorkflowContext;

public class DimentionsController extends StandardController<DimentionsAction> implements BarcodeListener {

	private BarcodeScanner<?> barcodeScanner;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private Float length = Float.NaN;
	private Float height = Float.NaN;
	private Float width = Float.NaN;

	@FXML
	private TextField lengthEdit;
	@FXML
	private TextField heightEdit;
	@FXML
	private TextField widthEdit;

	public DimentionsController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		lengthEdit.setText(null);
		heightEdit.setText(null);
		widthEdit.setText(null);

		errorMessageProperty.set(null);
		errorVisibleProperty.set(false);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		barcodeScanner.addBarcodeListener(this);

		barcodeChecker.playFromStart();
	}

	@Override
	public void terminate() {
		barcodeChecker.stop();
		barcodeScanner.removeBarcodeListener(this);
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

	private void processBarcode(String value) {
		if (!checkDimentions()) {
			return;
		}

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (getAction().processBarcode(value, order, length, height, width)) {
			done();
		} else {
			barcodeCheckerEventHandler.reset();

			errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
			errorVisibleProperty.set(true);
		}
	}

	private boolean checkDimentions() {
		String error = null;

		try {
			width = Float.valueOf(widthEdit.getText());
		} catch (Exception e) {
			width = Float.NaN;
			error = "error.dimentions.width";
			widthEdit.requestFocus();
		}

		try {
			height = Float.valueOf(heightEdit.getText());
		} catch (Exception e) {
			height = Float.NaN;
			error = "error.dimentions.height";
			heightEdit.requestFocus();
		}

		try {
			length = Float.valueOf(lengthEdit.getText());
		} catch (Exception e) {
			length = Float.NaN;
			error = "error.dimentions.length";
			lengthEdit.requestFocus();
		}

		if (error != null) {
			barcodeCheckerEventHandler.reset();
			errorMessageProperty.set(getResources().getString(error));
			errorVisibleProperty.set(true);
		}

		return !length.isNaN() && !height.isNaN() && !width.isNaN();
	}

	private TextField getFocusedEdit() {
		if (lengthEdit.isFocused()) {
			return lengthEdit;
		} else if (heightEdit.isFocused()) {
			return heightEdit;
		} else if (widthEdit.isFocused()) {
			return widthEdit;
		} else {
			lengthEdit.requestFocus();
			return null;
		}
	}

	private void addSymbol(String s) {
		TextField tf = getFocusedEdit();
		if (tf != null) {
			String text = tf.getText();
			text = (text == null ? "" : text) + s;
			tf.setText(text);
			tf.positionCaret(text.length());
		}
	}

	public void numericKeybordEnterClick(ActionEvent event) {
		if (lengthEdit.isFocused()) {
			heightEdit.requestFocus();
			return;
		} else if (heightEdit.isFocused()) {
			widthEdit.requestFocus();
			return;
		} else {
			lengthEdit.requestFocus();
			return;
		}
	}

	public void numericKeybordBackClick(ActionEvent event) {
		TextField tf = getFocusedEdit();
		if (tf != null) {
			String text = tf.getText();
			if (text != null && text.length() > 0) {
				text = text.substring(0, Math.max(0, text.length() - 1));
				tf.setText(text);
				tf.positionCaret(text.length());
			}
		}
	}

	public void numericKeybordClearClick(ActionEvent event) {
		TextField tf = getFocusedEdit();
		if (tf != null) {
			tf.setText(null);
		}
	}

	public void numericKeybordDotClick(ActionEvent event) {
		addSymbol(".");
	}

	public void numericKeybord0Click(ActionEvent event) {
		addSymbol("0");
	}

	public void numericKeybord1Click(ActionEvent event) {
		addSymbol("1");
	}

	public void numericKeybord2Click(ActionEvent event) {
		addSymbol("2");
	}

	public void numericKeybord3Click(ActionEvent event) {
		addSymbol("3");
	}

	public void numericKeybord4Click(ActionEvent event) {
		addSymbol("4");
	}

	public void numericKeybord5Click(ActionEvent event) {
		addSymbol("5");
	}

	public void numericKeybord6Click(ActionEvent event) {
		addSymbol("6");
	}

	public void numericKeybord7Click(ActionEvent event) {
		addSymbol("7");
	}

	public void numericKeybord8Click(ActionEvent event) {
		addSymbol("8");
	}

	public void numericKeybord9Click(ActionEvent event) {
		addSymbol("9");
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
				if (barcodeScanner.isConnected()) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);
				} else {
					if (errorStr == null) {
						errorStr = DimentionsController.this.getResources().getString("error.barcode.scanner");
					}

					errorMessageProperty.set(errorStr);
					errorVisibleProperty.set(true);
				}
			} else {
				delayCount--;
			}
		}

		public void reset() {
			delayCount = 5;
		}
	}
}
