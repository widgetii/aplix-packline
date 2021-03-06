package ru.aplix.packline.controller;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.PhotoAction;
import ru.aplix.packline.hardware.camera.ImageListener;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraConnectionListener;
import ru.aplix.packline.hardware.camera.PhotoCameraImage;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesBundle;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class PhotoController extends StandardController<PhotoAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	private PhotoTask photoTask;

	@SuppressWarnings("unchecked")
	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		List<PhotoCamera<?>> pcs = (List<PhotoCamera<?>>) context.getAttribute(Const.PHOTO_CAMERAS);
		if (pcs != null && pcs.size() > 0) {
			// Get first photo camera in list
			// (weight can be inputed manually)
			PhotoCamera<?> pc = pcs.get(0);

			// Make photo only above active scales
			Scales<?> scales = (Scales<?>) getContext().getAttribute(Const.SCALES);
			if (scales != null && scales instanceof ScalesBundle) {
				ScalesBundle<?> scalesBundle = ((ScalesBundle<?>) scales);
				scales = scalesBundle.whoIsLoaded();
				if (scales != null) {
					int index = 0;
					scalesBundle.resetEnumerator();
					while (scalesBundle.hasMoreElements()) {
						if (scales.equals(scalesBundle.nextElement())) {
							break;
						}
						index++;
					}
					if (index > 0 && index < pcs.size()) {
						pc = pcs.get(index);
					}
				}
			}

			photoTask = new PhotoTask(pc);
			ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
			executor.submit(photoTask);
		} else {
			throw new SkipActionException();
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (photoTask != null) {
			photoTask.cancel(false);
			photoTask = null;
		}
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
								progressVisibleProperty.set(false);
								errorMessageProperty.set(getResources().getString("error.photo.camera"));
								errorVisibleProperty.set(true);
							}
						});

						terminateLatch.await(Const.ERROR_DISPLAY_DELAY, TimeUnit.SECONDS);
					}
				} finally {
					photoCamera.removeConnectionListener(this);
					photoCamera.removeImageListener(this);
				}
			} catch (Exception e) {
				LOG.error(null, e);
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

			progressVisibleProperty.set(true);
		}

		@Override
		protected void succeeded() {
			super.succeeded();

			if (result != null) {
				getAction().imageAcquired(result);
			}

			progressVisibleProperty.set(false);
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
