package ru.aplix.packline.controller;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.PrintBarcodeAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.workflow.WorkflowContext;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class PrintBarcodeController extends StandardController<PrintBarcodeAction> {

	private final Log log = LogFactory.getLog(getClass());

	@FXML
	public StackPane contentPane;
	@FXML
	public ToggleButton countButton1;
	@FXML
	public Insets x1;
	@FXML
	public ToggleButton countButton2;
	@FXML
	public ToggleButton countButton3;
	@FXML
	public ToggleButton countButton4;
	@FXML
	public Button buttonGenerate;
	@FXML
	public Button buttonComplete;
	@FXML
	public Label infoLabel;
	@FXML
	public Insets x2;

	private Task<?> task;

	private ToggleGroup countGroup;

	private final int copies = 1;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		countGroup = new PersistentButtonToggleGroup();
		countButton1.setToggleGroup(countGroup);
		countButton2.setToggleGroup(countGroup);
		countButton3.setToggleGroup(countGroup);
		countButton4.setToggleGroup(countGroup);
		countGroup.selectToggle(countButton2);
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		setCount(countButton1, 1);
		setCount(countButton2, 2);
		setCount(countButton3, 3);
		setCount(countButton4, 4);
		countButton2.setSelected(true);

		setProgress(false);
	}

	private void setCount(ToggleButton toggleButton, int index) {
		try {
			List<Integer> quantity = Configuration.getInstance().getBarcodeLine().getQuantity();
			Integer value = quantity.get(index - 1);

			toggleButton.setText(String.format(getResources().getString("button.print.barcode.item"), value));
			toggleButton.setUserData(value);
		} catch (Exception e) {
			toggleButton.setText("-");
			toggleButton.setUserData(null);
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (task != null) {
			task.cancel(false);
		}
	}

	public void completeClick() {
		done();
	}

	public void generateClick() {
		Platform.runLater(this::printBarcodeLine);
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		buttonGenerate.setDisable(value);
	}

	private void printBarcodeLine() {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					final Integer length = (Integer) countGroup.getSelectedToggle().getUserData();
					Printer printer = Configuration.getInstance().getBarcodeLine().getPrinter();

					return length != null && printer != null && getAction().printBarcodeLine(length, printer, copies);
				} catch (Throwable e) {
					log.error(null, e);
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
				Throwable e = getException();
				if (e != null) {
					String errorStr = e.getMessage();
					errorMessageProperty.set(errorStr);
					errorVisibleProperty.set(true);
				}
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				if (!getValue()) {
					errorMessageProperty.set(getResources().getString("error.settings"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private class PersistentButtonToggleGroup extends ToggleGroup {

		public PersistentButtonToggleGroup() {
			super();
			getToggles().addListener((ListChangeListener<Toggle>) c -> {
                while (c.next())
                    for (final Toggle addedToggle : c.getAddedSubList())
                        ((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
                            if (addedToggle.equals(getSelectedToggle()))
                                mouseEvent.consume();
                        });
            });
		}
	}
}
