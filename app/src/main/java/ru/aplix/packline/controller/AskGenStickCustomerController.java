package ru.aplix.packline.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.AskGenStickCustomerAction;
import ru.aplix.packline.post.Customer;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class AskGenStickCustomerController extends StandardController<AskGenStickCustomerAction> {

	@FXML
	public Button buttonComplete;
	@FXML
	public Button buttonGenerate;
	@FXML
	public Label customerInfoLabel;
	@FXML
	public Insets x2;
	@FXML
	public StackPane contentPane;
	@FXML
	public Label infoLabel;

	private String customerCode = null;

	private final Log LOG = LogFactory.getLog(getClass());

	private BooleanProperty doneProperty = new SimpleBooleanProperty();

	private Task<?> task;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		Post post = (Post) getContext().getAttribute(Const.POST);
		if (post != null) {
			Order order = (Order) getContext().getAttribute(Const.ORDER);
			Customer customer = null;
			if (order != null) {
				customer = order.getCustomer();
			}
			else {
				customer = null;
			}


			if (customer != null) {
				final int[] enclosureCount = new int[1];
				String enclosures = post.getEnclosure().stream()
						.limit(5)
						.map(p -> {
							        enclosureCount[0]++;
									return enclosureCount[0] + ". " + p.getContentDescription();
								}
						)
						.collect(Collectors.joining("\n"));

				String message = String.format(getResources().getString("confirmation.newMarker"), post.getId(), customer.getName(), enclosures, order.getDeliveryAddress());

				confirmGenStick(message);
				customerCode = customer.getId();
				customerInfoLabel.setText(message);
			} else {
				doneProperty.set(true);
				showErrorMessage(getResources().getString("sticking.no.customer.selected"));
			}
		}else {
			doneProperty.set(true);
			showErrorMessage(getResources().getString("error.post.container"));
		}

		setProgress(false);
	}

	private void confirmGenStick(String message) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle(Const.APP_NAME);
		alert.setHeaderText(getResources().getString("dialog.confirm"));
  	    alert.setContentText(message + " " + getResources().getString("confirmation.departure"));

		ButtonType buttonTypeYes = new ButtonType(
				getResources().getString("dialog.yes.newOrder"), ButtonBar.ButtonData.YES);
		ButtonType buttonTypeNo = new ButtonType(
				getResources().getString("dialog.no"), ButtonBar.ButtonData.NO);

		alert.getButtonTypes().setAll( buttonTypeYes, buttonTypeNo);

		Optional<ButtonType> resultConfirmGenStick = alert.showAndWait();
		if (resultConfirmGenStick.get() != buttonTypeYes) {
			throw new SkipActionException();
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (task != null) {
			task.cancel(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		buttonComplete.setDisable(value);
		buttonGenerate.setDisable(value);
	}

	public void generateClick(ActionEvent event) {
		generateAndPrintStickers(-1);
	}

	public void completeClick(ActionEvent event) {
		done();
	}

	private void generateAndPrintStickers(final int offset) {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().generateAndPrint(offset, 1, customerCode);
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
				return null;
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

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				doneProperty.set(false);
				showErrorMessage(errorStr);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				doneProperty.set(true);
				showWarningMessage(getResources().getString("askGenStickCustomer.info"), true);
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	@Override
	protected void hidePane() {
		super.hidePane();

		if (doneProperty.get())
		  done();
	}
}
