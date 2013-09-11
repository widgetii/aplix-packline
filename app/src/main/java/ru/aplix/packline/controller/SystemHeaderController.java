package ru.aplix.packline.controller;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import ru.aplix.packline.dialog.ShutdownDialog;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Window;
import javafx.util.Duration;

public class SystemHeaderController implements Initializable {

	@FXML
	public Label dateLabel;
	@FXML
	public Label timeLabel;
	@FXML
	public ImageView logoImage;

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	public SystemHeaderController() {
		dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
		timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		showDateTime();

		Timeline timeline = new Timeline();
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				showDateTime();
			}
		}));
		timeline.playFromStart();
	}

	private void showDateTime() {
		Date now = new Date();
		dateLabel.setText(dateFormat.format(now));
		timeLabel.setText(timeFormat.format(now));
	}
	
	public void shutdownClick(ActionEvent event) {
		Window owner = logoImage.getScene().getWindow();
		ShutdownDialog sd = new ShutdownDialog(owner);
		sd.centerOnScreen();
		sd.show();
	}
}
