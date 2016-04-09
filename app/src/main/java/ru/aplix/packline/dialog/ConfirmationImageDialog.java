package ru.aplix.packline.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;

public class ConfirmationImageDialog extends Stage {

	private ConfirmationImageDialogController controller;

	public ConfirmationImageDialog(final Window owner, final String title, final String message, List<String> photos, int currentPageIndex, ConfirmationListener confirmationListener) {
		super();

		try {
			initStyle(StageStyle.UNDECORATED);
			initModality(Modality.WINDOW_MODAL);
			initOwner(owner);

			setResizable(false);

			String fxmlName = "/resources/fxml/confirm-image.fxml";
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource(fxmlName));
			loader.setResources(ResourceBundle.getBundle("resources.messages.strings"));
			Parent rootNode = loader.load(getClass().getResourceAsStream(fxmlName));

			controller = loader.getController();
			if (title != null) {
				setHeader(title);
			}
			if (message != null) {
				setMessage(message);
			}

			controller.setImageList(photos);
			controller.setCurrentPageIndex(currentPageIndex);

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

	public void setDescription(String value) {
		controller.setDescription(value);
	}

	public void setCount(String value, Object... params) {
		controller.setCount(value, params);
	}
}
