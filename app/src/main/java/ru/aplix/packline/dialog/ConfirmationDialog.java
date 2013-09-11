package ru.aplix.packline.dialog;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class ConfirmationDialog extends Stage {

	private ConfirmationDialogController controller;

	public ConfirmationDialog(final Window owner, final String title, final String message, ConfirmationListener confirmationListener) {
		super();

		try {
			initStyle(StageStyle.UNDECORATED);
			initModality(Modality.WINDOW_MODAL);
			initOwner(owner);

			setResizable(false);

			String fxmlName = "/resources/fxml/confirm.fxml";
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource(fxmlName));
			loader.setResources(ResourceBundle.getBundle("resources.messages.strings"));
			Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlName));

			controller = (ConfirmationDialogController) loader.getController();
			if (title != null) {
				controller.setTitle(title);
			}
			if (message != null) {
				controller.setMessage(message);
			}
			controller.setConfirmationListener(confirmationListener);

			Scene scene = new Scene(rootNode);
			setScene(scene);
		} catch (IOException e) {
			RuntimeException re = new RuntimeException();
			re.addSuppressed(e);
			throw re;
		}
	}

	public void setHeader(String value) {
		controller.setTitle(value);
	}

	public void setMessage(String value) {
		controller.setMessage(value);
	}

	public void setMessage(String value, Object... params) {
		controller.setMessage(value, params);
	}
}
