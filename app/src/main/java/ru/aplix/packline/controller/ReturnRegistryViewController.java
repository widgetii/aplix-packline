package ru.aplix.packline.controller;

import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Callback;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ReturnRegistryViewAction;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.ActionType;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReturnRegistryViewController extends StandardController<ReturnRegistryViewAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@SuppressWarnings("rawtypes")
	@FXML
	private TableView incomingsTableView;
	@FXML
	private Label registryInfoLabel;
	@FXML
	private Label totalIncomingsLabel;
	@FXML
	private AnchorPane registryInfoContainer;
	@FXML
	private Button closeRegistryButton;
	@FXML
	private Button saveRegistryButton;
	@FXML
	private Button deleteRegistryButton;

	private DateFormat dateFormat;
	private DateFormat timeFormat;
	private Registry registry;

	private BarcodeScanner<?> barcodeScanner = null;

	private ConfirmationDialog confirmationDialog = null;
	private List<Task<?>> tasks;

	public ReturnRegistryViewController() {
		super();

		tasks = new ArrayList<Task<?>>();

		dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		initTable();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		setProgress(false);

		registry = (Registry) context.getAttribute(Const.REGISTRY);
		if (registry != null) {
			ObservableList<Incoming> data = FXCollections.observableArrayList(registry.getIncoming());
			incomingsTableView.setItems(data);

			Date date = registry.getDate() != null ? registry.getDate().toGregorianCalendar().getTime() : null;
			registryInfoLabel.setText(String.format(getResources().getString("registry.info"), registry.getId(), date != null ? dateFormat.format(date) : "",
					date != null ? timeFormat.format(date) : "", registry.getCustomer().getName()));

			totalIncomingsLabel.setText(String.format(getResources().getString("act.incomings.total"), incomingsTableView.getItems().size(),
					registry.getTotalIncomings()));
		}

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}

		closeRegistryButton.setDisable(registry != null && ActionType.DELETE.equals(registry.getActionType()) && registry.getIncoming().size() > 0);
	}

	@SuppressWarnings("unchecked")
	private void initTable() {
		TableColumn<Incoming, String> firstColumn = new TableColumn<Incoming, String>(getResources().getString("act.incoming.number"));
		firstColumn.setSortable(false);
		firstColumn.setResizable(false);
		firstColumn.setEditable(false);
		firstColumn.prefWidthProperty().bind(incomingsTableView.widthProperty().multiply(0.20));
		firstColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Incoming, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getId());
			}
		});

		TableColumn<Incoming, String> secondColumn = new TableColumn<Incoming, String>(getResources().getString("act.incoming.description"));
		secondColumn.setSortable(false);
		secondColumn.setResizable(false);
		secondColumn.setEditable(false);
		secondColumn.prefWidthProperty().bind(incomingsTableView.widthProperty().multiply(0.48));
		secondColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Incoming, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getContentDescription());
			}
		});

		TableColumn<Incoming, String> thirdColumn = new TableColumn<Incoming, String>(getResources().getString("act.incoming.datetime"));
		thirdColumn.setSortable(false);
		thirdColumn.setResizable(false);
		thirdColumn.setEditable(false);
		thirdColumn.prefWidthProperty().bind(incomingsTableView.widthProperty().multiply(0.15));
		thirdColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Incoming, String> p) {
				String s = "";
				if (p.getValue().getDate() != null) {
					Date d = p.getValue().getDate().toGregorianCalendar().getTime();
					s = String.format("%s\n%s", dateFormat.format(d), timeFormat.format(d));
				}
				return new ReadOnlyObjectWrapper<String>(s);
			}
		});

		TableColumn<Incoming, Float> fourthColumn = new TableColumn<Incoming, Float>(getResources().getString("act.incoming.weight"));
		fourthColumn.setSortable(false);
		fourthColumn.setResizable(false);
		fourthColumn.setEditable(false);
		fourthColumn.prefWidthProperty().bind(incomingsTableView.widthProperty().multiply(0.10));
		fourthColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, Float>, ObservableValue<Float>>() {
			public ObservableValue<Float> call(CellDataFeatures<Incoming, Float> p) {
				return new ReadOnlyObjectWrapper<Float>(p.getValue().getWeight());
			}
		});

		incomingsTableView.getColumns().add(firstColumn);
		incomingsTableView.getColumns().add(secondColumn);
		incomingsTableView.getColumns().add(thirdColumn);
		incomingsTableView.getColumns().add(fourthColumn);

		incomingsTableView.setPlaceholder(new Text(getResources().getString("act.noincomings")));

		registryInfoContainer.prefWidthProperty().bind(((AnchorPane) rootNode).widthProperty().multiply(0.25));
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		for (Task<?> task : tasks) {
			task.cancel(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		closeRegistryButton.setDisable(value);
		saveRegistryButton.setDisable(value);
		deleteRegistryButton.setDisable(value);
	}

	public void closeRegistryClick(ActionEvent event) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.carryOutAndClose", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				doCloseRegistry();
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		String orderDateStr = "";
		if (registry.getDate() != null) {
			Date orderDate = registry.getDate().toGregorianCalendar().getTime();
			orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));
		}

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.registry.carryOutAndClose", registry.getId(), orderDateStr);
		confirmationDialog.show();
	}

	private void doCloseRegistry() {
		Task<?> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().carryOutRegistry();
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

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				getAction().setNextAction(getAction().getBackAction());
				ReturnRegistryViewController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
	}

	public void saveRegistryClick(ActionEvent event) {
		getAction().setNextAction(getAction().getBackAction());
		done();
	}

	public void deleteRegistryClick(ActionEvent event) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.delete", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				doDeleteAct();
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		String orderDateStr = "";
		if (registry.getDate() != null) {
			Date orderDate = registry.getDate().toGregorianCalendar().getTime();
			orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));
		}

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.registry.delete", registry.getId(), orderDateStr);
		confirmationDialog.show();
	}

	private void doDeleteAct() {
		Task<?> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().deleteRegistry();
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

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				getAction().setNextAction(getAction().getBackAction());
				ReturnRegistryViewController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
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
		if (progressVisibleProperty.get() || confirmationDialog != null) {
			return;
		}

		Task<?> task = new Task<Boolean>() {
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

				Boolean result = getValue();
				if (result != null && result.booleanValue()) {
					getAction().setNextAction(getAction().getWeightingAction());
					ReturnRegistryViewController.this.done();
				} else {
					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
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
