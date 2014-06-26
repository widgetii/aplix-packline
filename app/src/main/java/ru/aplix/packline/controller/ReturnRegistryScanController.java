package ru.aplix.packline.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.PaginationBuilder;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Callback;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ReturnRegistryScanAction;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.scanner.ImageListener;
import ru.aplix.packline.hardware.scanner.ImageScanner;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReturnRegistryScanController extends StandardController<ReturnRegistryScanAction> implements ImageListener, BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Button acquireButton;
	@FXML
	private Button deleteImageButton;
	@FXML
	private Pane imageContainer;
	@FXML
	private ImageView imageView;
	@FXML
	private ImageView carrierImage;
	@FXML
	private VBox paginationContainer;

	private Pagination pagination;
	private ConfirmationDialog confirmationDialog = null;

	private ImageScanner<?> imageScanner = null;
	private BarcodeScanner<?> barcodeScanner = null;

	private ObjectProperty<Image> imageProperty;
	private List<File> images;

	private Task<Void> task;

	public ReturnRegistryScanController() {
		images = new Vector<File>();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		imageView.fitWidthProperty().bind(imageContainer.widthProperty().subtract(15));
		imageView.fitHeightProperty().bind(imageContainer.heightProperty().subtract(15));

		imageProperty = new SimpleObjectProperty<Image>();
		imageView.imageProperty().bind(imageProperty);

		createPagination();
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		images.clear();
		imageProperty.set(null);
		pagination.setPageCount(1);

		String selectedCarrier = (String) context.getAttribute(Const.SELECTED_CARRIER);
		if (selectedCarrier != null) {
			Image image = new Image(getClass().getResource(String.format("/resources/images/logo-%s.png", selectedCarrier.toLowerCase())).toExternalForm());
			carrierImage.setImage(image);
		} else {
			carrierImage.setImage(null);
		}

		// Initialize image scanner
		imageScanner = (ImageScanner<?>) context.getAttribute(Const.IMAGE_SCANNER);
		if (imageScanner != null) {
			imageScanner.addImageListener(this);
		}

		// Initialize bar-code scanner
		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}

		setProgress(false);
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		imageProperty.set(null);

		if (imageScanner != null) {
			imageScanner.removeImageListener(this);
		}
		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		acquireButton.setDisable(imageScanner == null || value);
		deleteImageButton.setDisable(images.size() == 0 || value);
	}

	public void acquireClick(ActionEvent event) {
		if (imageScanner != null) {
			setProgress(true);
			imageScanner.acquireImage();
		}
	}

	public void deleteClick(ActionEvent event) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.delete", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				doDeleteImage();
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.image.delete");
		confirmationDialog.show();
	}

	private void doDeleteImage() {
		setProgress(true);
		try {
			int index = pagination.getCurrentPageIndex();
			imageProperty.set(null);
			images.remove(index);
			pagination.setPageCount(images.size());
			pagination.setCurrentPageIndex(index - 1 >= 0 ? index - 1 : 0);
		} finally {
			setProgress(false);
		}
	}

	@Override
	public void onImageAcquired(File imageFile) {
		try {
			File newFile = convertToJpeg(imageFile);
			images.add(newFile);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if (pagination.getPageCount() != images.size()) {
						pagination.setPageCount(images.size());
						pagination.setCurrentPageIndex(images.size() - 1);
					} else {
						setImage(images.get(pagination.getCurrentPageIndex()));
					}
				}
			});
		} catch (Exception e) {
			LOG.error(null, e);
		}
	}

	private void createPagination() {
		pagination = PaginationBuilder.create().pageFactory(new Callback<Integer, Node>() {
			@Override
			public Node call(final Integer pageIndex) {
				Label label = new Label();
				label.setVisible(false);

				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						try {
							setImage(pageIndex < images.size() ? images.get(pageIndex) : null);
						} catch (Exception e) {
							LOG.error(null, e);
						}
					}
				});

				return label;
			}
		}).maxPageIndicatorCount(10).pageCount(1).build();

		paginationContainer.getChildren().add(pagination);
	}

	private File convertToJpeg(File originalFile) throws IOException {
		BufferedImage image = ImageIO.read(originalFile);

		File jpegFile = File.createTempFile("scan", ".jpg");
		jpegFile.deleteOnExit();

		FileOutputStream fos = new FileOutputStream(jpegFile);
		try {
			ImageIO.write(image, "jpeg", fos);
		} finally {
			fos.close();
		}

		return jpegFile;
	}

	private void setImage(File imageFile) {
		if (imageFile == null) {
			imageProperty.set(null);
			return;
		}

		Image image = new Image(imageFile.toURI().toString(), true);
		imageProperty.set(image);
	}

	@Override
	public void onImageAcquisitionCompleted() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				setProgress(false);
			}
		});
	}

	@Override
	public void onImageAcquisitionFailed() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				setProgress(false);

				errorMessageProperty.set(getResources().getString("error.image.acquisition"));
				errorVisibleProperty.set(true);
			}
		});
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (images.size() == 0) {
					confirmNoImages(value);
				} else {
					processBarcode(value);
				}
			}
		});
	}

	public void confirmNoImages(final String value) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.delete", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				processBarcode(value);
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.continue.no.images");
		confirmationDialog.show();
	}

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get() || confirmationDialog != null) {
			return;
		}

		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().processBarcode(value, images);
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

				ReturnRegistryScanController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	@Override
	protected boolean checkNoError() {
		boolean barcodeOk = ((barcodeScanner == null) || barcodeScanner.isConnected());
		boolean imageOk = ((imageScanner == null) || imageScanner.isConnected());
		if (barcodeOk && imageOk) {
			return true;
		} else {
			if (!barcodeOk) {
				errorMessageProperty.set(getResources().getString("error.barcode.scanner"));
			} else {
				errorMessageProperty.set(getResources().getString("error.image.scanner"));
			}

			errorVisibleProperty.set(true);
			return false;
		}
	}
}
