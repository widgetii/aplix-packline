package ru.aplix.packline.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.DimentionsAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.workflow.WorkflowContext;

public class DimentionsController extends StandardController<DimentionsAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private TextField lengthEdit;
	@FXML
	private TextField widthEdit;
	@FXML
	private TextField heightEdit;
	@FXML
	private Pane buttonsContainer;

	private BarcodeScanner<?> barcodeScanner = null;

	private Float length = Float.NaN;
	private Float width = Float.NaN;
	private Float height = Float.NaN;

	private Task<Void> task;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		// Add key event filter
		rootNode.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				switch (keyEvent.getCode()) {
				case ENTER:
					numericKeybordEnterClick(null);
					break;
				default:
					break;
				}
			}
		});
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		buttonsContainer.setDisable(false);

		lengthEdit.setText(null);
		heightEdit.setText(null);
		widthEdit.setText(null);

		lengthEdit.requestFocus();

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}
	}

	@Override
	public void terminate() {
		super.terminate();

		if (barcodeScanner != null) {
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

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get()) {
			return;
		}

		if (!checkDimentions()) {
			return;
		}

		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().processBarcode(value, length, height, width);
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
				buttonsContainer.setDisable(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				buttonsContainer.setDisable(false);

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
				buttonsContainer.setDisable(false);

				DimentionsController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private boolean checkDimentions() {
		String error = null;

		try {
			height = Float.valueOf(heightEdit.getText());
		} catch (Exception e) {
			height = Float.NaN;
			error = "error.dimentions.height";
			heightEdit.requestFocus();
		}

		try {
			width = Float.valueOf(widthEdit.getText());
		} catch (Exception e) {
			width = Float.NaN;
			error = "error.dimentions.width";
			widthEdit.requestFocus();
		}

		try {
			length = Float.valueOf(lengthEdit.getText());
		} catch (Exception e) {
			length = Float.NaN;
			error = "error.dimentions.length";
			lengthEdit.requestFocus();
		}

		if (error != null) {
			errorMessageProperty.set(getResources().getString(error));
			errorVisibleProperty.set(true);
		}

		return !length.isNaN() && !height.isNaN() && !width.isNaN();
	}

	private TextField getFocusedEdit() {
		if (lengthEdit.isFocused()) {
			return lengthEdit;
		} else if (widthEdit.isFocused()) {
			return widthEdit;
		} else if (heightEdit.isFocused()) {
			return heightEdit;
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
			widthEdit.requestFocus();
			return;
		} else if (widthEdit.isFocused()) {
			heightEdit.requestFocus();
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

	@Override
	protected boolean checkNoError() {
		if ((barcodeScanner == null) || barcodeScanner.isConnected()) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.barcode.scanner"));
			errorVisibleProperty.set(true);

			return false;
		}
	}
}
