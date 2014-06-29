package ru.aplix.packline.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.TilePane;
import javafx.stage.Window;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ReadBarcodeBoxAction;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReadBarcodeBoxController extends StandardController<ReadBarcodeBoxAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private TilePane tilePane;
	@FXML
	private Button nextButton;

	private ConfirmationDialog confirmationDialog = null;
	private BarcodeScanner<?> barcodeScanner = null;
	private Task<Void> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		tilePane.getChildren().clear();

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	private void addCodeButton(String value) {
		for (Node node : tilePane.getChildren()) {
			if (value.equals(((Button) node).getText())) {
				errorMessageProperty.set(getResources().getString("error.box.already.added"));
				errorVisibleProperty.set(true);
				return;
			}
		}

		Button button = new Button();
		button.getStyleClass().add("custom-box-button");
		button.setAlignment(Pos.CENTER);
		button.setContentDisplay(ContentDisplay.TOP);
		button.setText(value);
		button.setPrefSize(160, 160);

		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				confirmDeleteCode(((Button) actionEvent.getSource()).getText());
			}
		});

		tilePane.getChildren().add(button);
	}

	private void confirmDeleteCode(final String code) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.delete", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;

				for (Node node : tilePane.getChildren()) {
					if (code.equals(((Button) node).getText())) {
						tilePane.getChildren().remove(node);
						break;
					}
				}
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.box.delete", code);
		confirmationDialog.show();
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (progressVisibleProperty.get() || confirmationDialog != null) {
					return;
				}

				addCodeButton(value);
			}
		});
	}

	public void nextClick(ActionEvent event) {
		final List<String> codes = new ArrayList<String>();
		for (Node node : tilePane.getChildren()) {
			codes.add(((Button) node).getText());
		}

		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().process(codes);
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
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);

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

				ReadBarcodeBoxController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
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
