package ru.aplix.packline.controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.PhotoAction;
import ru.aplix.packline.hardware.camera.ImageListener;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraConnectionListener;
import ru.aplix.packline.hardware.camera.PhotoCameraImage;
import ru.aplix.packline.workflow.WorkflowContext;

public class PhotoController extends StandardController<PhotoAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private ProgressIndicator progressIndicator;

	private PhotoTask photoTask;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		errorMessageProperty.set(null);
		errorVisibleProperty.set(false);

		PhotoCamera<?> pc = (PhotoCamera<?>) context.getAttribute(Const.PHOTO_CAMERA);

		photoTask = new PhotoTask(pc);
		new Thread(photoTask).start();
	}

	@Override
	public void terminate() {
		photoTask.cancel(false);
	}

	private void imageAcquired(PhotoCameraImage result) {
		// TODO: place image processing logic here
	}

	/**
	 *
	 */
	private class PhotoTask extends Task<Void> implements PhotoCameraConnectionListener, ImageListener {

		private PhotoCamera<?> photoCamera;
		private volatile PhotoCameraImage result = null;

		private CountDownLatch connectLatch;
		private CountDownLatch photoLatch;
		private CountDownLatch terminateLatch;

		public PhotoTask(PhotoCamera<?> photoCamera) {
			this.photoCamera = photoCamera;

			connectLatch = new CountDownLatch(1);
			photoLatch = new CountDownLatch(1);
			terminateLatch = new CountDownLatch(1);
		}

		@Override
		public Void call() {
			if (!photoCamera.getConfiguration().isEnabled()) {
				return null;
			}

			try {
				boolean waitForConnection = !photoCamera.isConnected();

				photoCamera.addConnectionListener(this);
				photoCamera.addImageListener(this);
				try {
					if (!isCancelled() && waitForConnection) {
						connectLatch.await();
					}

					if (!isCancelled() && photoCamera.isConnected()) {
						photoCamera.makePhoto();
						photoLatch.await();
					}

					if (!isCancelled() && result == null) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								progressIndicator.setVisible(false);
								errorMessageProperty.set(getResources().getString("error.camera"));
								errorVisibleProperty.set(true);
							}
						});

						terminateLatch.await(5, TimeUnit.SECONDS);
					}
				} finally {
					photoCamera.removeConnectionListener(this);
					photoCamera.removeImageListener(this);
				}
			} catch (Exception e) {
				LOG.error(e);
			}
			return null;
		}

		@Override
		public boolean cancel(boolean arg0) {
			connectLatch.countDown();
			photoLatch.countDown();
			terminateLatch.countDown();
			return super.cancel(arg0);
		}

		@Override
		protected void running() {
			super.running();

			progressIndicator.setVisible(true);
		}

		@Override
		protected void succeeded() {
			super.succeeded();

			if (result != null) {
				imageAcquired(result);
			}

			progressIndicator.setVisible(false);
			PhotoController.this.done();
		}

		@Override
		public void onConnected() {
			connectLatch.countDown();
		}

		@Override
		public void onDisconnected() {
			connectLatch.countDown();
		}

		@Override
		public void onConnectionFailed() {
			connectLatch.countDown();
		}

		@Override
		public void onImageAcquired(PhotoCameraImage value) {
			result = value;
			photoLatch.countDown();
		}

		@Override
		public void onImageAcquisitionFailed() {
			photoLatch.countDown();
		}
	}
}
