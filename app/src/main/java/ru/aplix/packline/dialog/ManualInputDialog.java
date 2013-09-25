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

public class ManualInputDialog extends Stage {

	public ManualInputDialog(final Window owner, ManualInputListener listener) {
		super();

		try {
			initStyle(StageStyle.UNDECORATED);
			initModality(Modality.WINDOW_MODAL);
			initOwner(owner);

			setResizable(false);

			String fxmlName = "/resources/fxml/manual.fxml";
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource(fxmlName));
			loader.setResources(ResourceBundle.getBundle("resources.messages.strings"));
			Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlName));

			ManualInputController mic = (ManualInputController) loader.getController();
			mic.setListener(listener);

			Scene scene = new Scene(rootNode);
			setScene(scene);
		} catch (IOException e) {
			RuntimeException re = new RuntimeException();
			re.addSuppressed(e);
			throw re;
		}
	}
}