package ru.aplix.packline.dialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ConfirmationImageDialogController implements Initializable {

	@FXML
	public Insets x1;

	@FXML
	public Button yesButton;

	@FXML
	public Button noButton;

	@FXML
	public Pagination pagination;

	@FXML
	public Label descriptionLabel;

	@FXML
	public Label countLabel;

	@FXML
	private Label titleLabel;

	@FXML
	private Label messageLabel;

	private StringProperty titleProperty = new SimpleStringProperty();
	private StringProperty messageProperty = new SimpleStringProperty();
	private StringProperty descriptionProperty = new SimpleStringProperty();
	private StringProperty countProperty = new SimpleStringProperty();

	private ResourceBundle resources;

	private ConfirmationListener confirmationListener;

	private List<String> imageList = new ArrayList<>();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;

		pagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
		pagination.setPageFactory(this::createPage);

		titleLabel.textProperty().bind(titleProperty);
		messageLabel.textProperty().bind(messageProperty);
		descriptionLabel.textProperty().bind(descriptionProperty);
		countLabel.textProperty().bind(countProperty);
	}

	private Image image;

	private VBox createPage(Integer index) {
		ImageView imageView = new ImageView();

        if (imageList != null && imageList.size() > 0) {
			Platform.runLater(() -> {
				image = new Image(imageList.get(index));
				imageView.setImage(image);
			});
		}
		else {
			image = new Image(getClass().getResource("/resources/images/nophoto.png").toExternalForm());
			imageView.setImage(image);
		}

		imageView.setFitWidth(600);
		imageView.setFitHeight(400);
		imageView.setPreserveRatio(true);
		imageView.setCache(true);

		VBox vBox = new VBox();
		vBox.setAlignment(Pos.CENTER);
		vBox.getChildren().add(imageView);
		return vBox;
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

	public void setImageList(List<String> imageList) {
		this.imageList = imageList;

		if (imageList != null && imageList.size() > 0)
			pagination.setPageCount(imageList.size());
		else
			pagination.setPageCount(1);
	}

	public void setDescription(String value) {
		descriptionProperty.set(value);
	}

	public void setCount(String value, Object[] params) {
		countProperty.set(String.format(resources.getString(value), params));
	}

	public void setCurrentPageIndex(int index) {
		if (index > -1 && index < pagination.getPageCount()) {
			pagination.setCurrentPageIndex(index);
		}
	}
}
