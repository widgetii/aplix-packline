package ru.aplix.packline.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import ru.aplix.packline.action.PrintFormsAction;
import ru.aplix.packline.workflow.WorkflowContext;

public class PrintFormsController extends StandardController<PrintFormsAction> {

	@FXML
	private ProgressIndicator progressIndicator;
	@FXML
	private Label infoLabel;
	@FXML
	private GridPane reprintContainer;

	private ResourceBundle resources;

	private Task<Void> task;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		this.resources = resources;
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);
		
		progressIndicator.setVisible(false);
		reprintContainer.setDisable(true);

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
				infoLabel.setText(resources.getString("printing.info1"));
				progressIndicator.setVisible(true);
				reprintContainer.setDisable(true);
			}
		});

		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent arg0) {
				infoLabel.setText(resources.getString("printing.info2"));
				progressIndicator.setVisible(false);
				reprintContainer.setDisable(false);
			}
		});

		new Thread(task).start();
	}

	@Override
	public void terminate() {
		task.cancel(false);
	}

	public void nextClick(ActionEvent event) {
		done();
	}
}
