package ru.aplix.packline.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Callback;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.OrderActScanAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.scanner.ImageListener;
import ru.aplix.packline.hardware.scanner.ImageScanner;
import ru.aplix.packline.workflow.WorkflowContext;

@SuppressWarnings("deprecation")
public class OrderActScanController extends StandardController<OrderActScanAction> implements ImageListener, BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Button acquireButton;
	@FXML
	private Button deleteImageButton;
	@FXML
	private Button cancelButton;
	@FXML
	private ScrollPane imageContainer;
	@FXML
	private ImageView imageView;
	@FXML
	private VBox paginationContainer;
	
	private BarcodeScanner<?> barcodeScanner = null;

	private Pagination pagination;
	private ConfirmationDialog confirmationDialog = null;

	private ImageScanner<?> imageScanner = null;

	private ObjectProperty<Image> imageProperty;
	private List<File> images;

	private Task<Void> task;

	public OrderActScanController() {
		images = new Vector<File>();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		imageProperty = new SimpleObjectProperty<Image>();
		imageView.imageProperty().bind(imageProperty);

		imageView.fitWidthProperty().bind(imageContainer.widthProperty());

		createPagination();
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		images.clear();
		imageProperty.set(null);
		pagination.setPageCount(1);
		
		// Initialize bar-code scanner
		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}

		// Initialize image scanner
		imageScanner = (ImageScanner<?>) context.getAttribute(Const.IMAGE_SCANNER);
		if (imageScanner != null) {
			imageScanner.addImageListener(this);
		}

		setProgress(false);
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		imageProperty.set(null);
		
		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		if (imageScanner != null) {
			imageScanner.removeImageListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		acquireButton.setDisable(imageScanner == null || value);
		deleteImageButton.setDisable(images.size() == 0 || value);
		cancelButton.setDisable(value);
	}
	
	public void cancelClick(ActionEvent event) {

		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.confirm", null, new ConfirmationListener() {

					@Override
					public void onAccept() {
						confirmationDialog = null;
						done(); // close action without act scanned
					}

					@Override
					public void onDecline() {
						confirmationDialog = null;
					}
				});

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.act.withoutActScanned");
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

				OrderActScanController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
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

	private File convertToJpeg(File originalFile) throws IOException, JAXBException {
		BufferedImage image = ImageIO.read(originalFile);

		File jpegFile = File.createTempFile("scan", ".jpg");
		jpegFile.deleteOnExit();

		FileOutputStream fos = new FileOutputStream(jpegFile);
		try {
			Float quality = Configuration.getInstance().getJpegCompressionQuality();
			if (quality != null) {
				quality = Math.max(0, Math.min(1, quality));

				// get all image writers for JPG format
				Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
				if (!writers.hasNext())
					throw new IllegalStateException("No writers found");

				ImageWriter writer = (ImageWriter) writers.next();
				ImageOutputStream ios = ImageIO.createImageOutputStream(fos);
				writer.setOutput(ios);
				try {
					ImageWriteParam param = writer.getDefaultWriteParam();

					// compress to a given quality
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionQuality(0.75f);

					// write image
					writer.write(null, new IIOImage(image, null, null), param);
				} finally {
					// close all streams
					ios.close();
					writer.dispose();
				}
			} else {
				ImageIO.write(image, "jpeg", fos);
			}
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

		final Image image = new Image(imageFile.toURI().toString(), true);
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
	protected boolean checkNoError() {
		boolean imageOk = ((imageScanner == null) || imageScanner.isConnected());
		if (imageOk) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.image.scanner"));
			errorVisibleProperty.set(true);
			return false;
		}
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (images.size() == 0) {
					errorMessageProperty.set(getResources().getString("error.order.scan.empty"));
					errorVisibleProperty.set(true);
				} else {
					processBarcode(value);
				}
			}
		});
	}
}
