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

public class ShutdownDialog extends Stage {

	public ShutdownDialog(final Window owner) {
		super();

		try {
			initStyle(StageStyle.UNDECORATED);
			initModality(Modality.WINDOW_MODAL);
			initOwner(owner);

			setResizable(false);

			String fxmlName = "/resources/fxml/shutdown.fxml";
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource(fxmlName));
			loader.setResources(ResourceBundle.getBundle("resources.messages.strings"));
			Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlName));

			ShutdownDialogController sfc = (ShutdownDialogController) loader.getController();
			sfc.setAnotherOwner(owner);

			Scene scene = new Scene(rootNode);
			setScene(scene);
		} catch (IOException e) {
			RuntimeException re = new RuntimeException();
			re.addSuppressed(e);
			throw re;
		}
	}
}