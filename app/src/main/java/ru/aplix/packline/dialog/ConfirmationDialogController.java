package ru.aplix.packline.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ConfirmationDialogController implements Initializable {

	@FXML
	private Label titleLabel;

	@FXML
	private Label messageLabel;

	private StringProperty titleProperty = new SimpleStringProperty();
	private StringProperty messageProperty = new SimpleStringProperty();

	private ResourceBundle resources;

	private ConfirmationListener confirmationListener;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;

		titleLabel.textProperty().bind(titleProperty);
		messageLabel.textProperty().bind(messageProperty);
	}

	public ConfirmationListener getConfirmationListener() {
		return confirmationListener;
	}

	public void setConfirmationListener(ConfirmationListener confirmationListener) {
		this.confirmationListener = confirmationListener;
	}

	public void setTitle(String value) {
		titleProperty.set(resources.getString(value));
	}

	public void setMessage(String value) {
		messageProperty.set(resources.getString(value));
	}

	public void setMessage(String value, Object... params) {
		messageProperty.set(String.format(resources.getString(value), params));
	}

	public void close() {
		Window owner = titleLabel.getScene().getWindow();
		if (owner instanceof Stage) {
			((Stage) owner).close();
		}
	}

	public void yesClick(ActionEvent event) {
		close();

		if (confirmationListener != null) {
			confirmationListener.onAccept();
		}
	}

	public void noClick(ActionEvent event) {
		close();

		if (confirmationListener != null) {
			confirmationListener.onDecline();
		}
	}
}
