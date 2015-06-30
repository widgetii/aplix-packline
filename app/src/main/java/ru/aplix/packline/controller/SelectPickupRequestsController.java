package ru.aplix.packline.controller;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import javafx.util.Callback;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.SelectPickupRequestsAction;
import ru.aplix.packline.post.PickupRequest;
import ru.aplix.packline.workflow.WorkflowContext;

public class SelectPickupRequestsController extends StandardController<SelectPickupRequestsAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@SuppressWarnings("rawtypes")
	@FXML
	private TableView pickupRequestsView;
	@FXML
	private Button completeButton;

	private Task<?> task;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		try {
			initTable();
		} catch (Throwable e) {
			LOG.error(null, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void initTable() throws FileNotFoundException, MalformedURLException, JAXBException {
		TableColumn<PickupRequest, String> firstColumn = new TableColumn<PickupRequest, String>(getResources().getString("pickupRequest.number"));
		firstColumn.setSortable(false);
		firstColumn.setResizable(false);
		firstColumn.setEditable(false);
		firstColumn.prefWidthProperty().bind(pickupRequestsView.widthProperty().multiply(0.10));
		firstColumn.setCellValueFactory(new Callback<CellDataFeatures<PickupRequest, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<PickupRequest, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getId());
			}
		});

		TableColumn<PickupRequest, String> secondColumn = new TableColumn<PickupRequest, String>(getResources().getString("pickupRequest.address"));
		secondColumn.setSortable(false);
		secondColumn.setResizable(false);
		secondColumn.setEditable(false);
		secondColumn.prefWidthProperty().bind(pickupRequestsView.widthProperty().multiply(0.30));
		secondColumn.setCellValueFactory(new Callback<CellDataFeatures<PickupRequest, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<PickupRequest, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getAddresss());
			}
		});

		TableColumn<PickupRequest, String> thirdColumn = new TableColumn<PickupRequest, String>(getResources().getString("pickupRequest.contact"));
		thirdColumn.setSortable(false);
		thirdColumn.setResizable(false);
		thirdColumn.setEditable(false);
		thirdColumn.prefWidthProperty().bind(pickupRequestsView.widthProperty().multiply(0.20));
		thirdColumn.setCellValueFactory(new Callback<CellDataFeatures<PickupRequest, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<PickupRequest, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getContact());
			}
		});

		TableColumn<PickupRequest, String> fourthColumn = new TableColumn<PickupRequest, String>(getResources().getString("pickupRequest.comment"));
		fourthColumn.setSortable(false);
		fourthColumn.setResizable(false);
		fourthColumn.setEditable(false);
		fourthColumn.prefWidthProperty().bind(pickupRequestsView.widthProperty().multiply(0.30));
		fourthColumn.setCellValueFactory(new Callback<CellDataFeatures<PickupRequest, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<PickupRequest, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getCommentForCourier());
			}
		});

		pickupRequestsView.getColumns().add(firstColumn);
		pickupRequestsView.getColumns().add(secondColumn);
		pickupRequestsView.getColumns().add(thirdColumn);
		pickupRequestsView.getColumns().add(fourthColumn);
		pickupRequestsView.setPlaceholder(new Text(getResources().getString("noPickupRequests")));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		pickupRequestsView.getItems().clear();
		loadList();
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}

	public void nextClick(ActionEvent event) {
		PickupRequest pr = (PickupRequest) pickupRequestsView.getSelectionModel().getSelectedItem();
		if (pr != null) {
			bindRegistryWithPickupRequest(pr);
		} else {
			done();
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		completeButton.setDisable(value);
	}

	private void loadList() {
		task = new Task<List<PickupRequest>>() {
			@Override
			public List<PickupRequest> call() throws Exception {
				try {
					return getAction().loadPickupRequests();
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

			@SuppressWarnings("unchecked")
			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				if (getValue() != null && getValue().size() > 0) {
					ObservableList<PickupRequest> data = FXCollections.observableArrayList(getValue());
					pickupRequestsView.setItems(data);
				} else {
					SelectPickupRequestsController.this.done();
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void bindRegistryWithPickupRequest(final PickupRequest pickupRequest) {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().bindRegistryWithPickupRequest(pickupRequest);
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

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				SelectPickupRequestsController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}
}
