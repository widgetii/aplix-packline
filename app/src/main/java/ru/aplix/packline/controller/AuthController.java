package ru.aplix.packline.controller;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.AuthAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.camera.DVRCamera;
import ru.aplix.packline.hardware.camera.RecorderListener;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.workflow.WorkflowContext;

public class AuthController extends StandardController<AuthAction> implements BarcodeListener, RecorderListener, MeasurementListener {

	private final Log LOG = LogFactory.getLog(getClass());

	private BarcodeScanner<?> barcodeScanner = null;
	private DVRCamera<?> dvrCamera = null;
	private Scales<?> scales = null;

	private Task<Operator> task;

	private BarcodeWeightListener barcodeWeightListener;

	private boolean blockWorkOnDVRCameraError = false;

	public AuthController() {
		barcodeWeightListener = new BarcodeWeightListener();
	}

	@Override
	public void prepare(WorkflowContext context) {
		context.setAttribute(Const.OPERATOR, null);
		super.prepare(context);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
			barcodeScanner.addBarcodeListener(barcodeWeightListener);
		}

		dvrCamera = (DVRCamera<?>) context.getAttribute(Const.DVR_CAMERA);
		if (dvrCamera != null) {
			dvrCamera.addRecorderListener(this);
			dvrCamera.disableRecording();
		}

		scales = (Scales<?>) context.getAttribute(Const.SCALES);
		if (scales != null) {
			updateMeasure(scales.getLastMeasurement());
			scales.addMeasurementListener(this);
			scales.addMeasurementListener(barcodeWeightListener);
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		if (dvrCamera != null) {
			dvrCamera.removeRecorderListener(this);

			if (!appIsStopping) {
				dvrCamera.enableRecording();
			}
		}

		if (scales != null) {
			scales.removeMeasurementListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				authenticateOperator(value);
			}
		});
	}

	private void authenticateOperator(final String value) {
		if (progressVisibleProperty.get() || blockWorkOnDVRCameraError) {
			return;
		}

		task = new Task<Operator>() {
			@Override
			public Operator call() throws Exception {
				try {
					Operator operator = getAction().authenticateOperator(value);
					return operator;
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);

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

				progressVisibleProperty.set(false);

				Object result = getValue();
				if (result != null) {
					AuthController.this.done();
				} else {
					errorMessageProperty.set(getResources().getString("error.auth.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	@Override
	protected boolean checkNoError() {
		if (((dvrCamera == null) || dvrCamera.isConnected()) && !blockWorkOnDVRCameraError) {
			if ((barcodeScanner == null) || barcodeScanner.isConnected()) {
				if ((scales == null) || scales.isConnected()) {
					return true;
				} else {
					errorMessageProperty.set(getResources().getString("error.scales"));
					errorVisibleProperty.set(true);

					return false;
				}
			} else {
				errorMessageProperty.set(getResources().getString("error.barcode.scanner"));
				errorVisibleProperty.set(true);

				return false;
			}
		} else {
			errorMessageProperty.set(getResources().getString("error.dvr.camera"));
			errorVisibleProperty.set(true);

			return false;
		}
	}

	@Override
	public void onRecordingStarted() {
	}

	@Override
	public void onRecordingStopped() {
	}

	@Override
	public void onRecordingFailed() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					if (Configuration.getInstance().getHardwareConfiguration().isBlockWorkOnDVRCameraError()) {
						blockWorkOnDVRCameraError = true;
					}
				} catch (FileNotFoundException | MalformedURLException | JAXBException e) {
				}

				errorMessageProperty.set(getResources().getString("error.dvr.camera"));
				errorVisibleProperty.set(true);
			}
		});
	}

	@Override
	public void onMeasure(final Float value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateMeasure(value);
			}
		});
	}

	@Override
	public void onWeightStabled(final Float value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				updateMeasure(value);
			}
		});
	}

	private void updateMeasure(Float value) {
		if (value != null && Math.abs(value) > 1e-6) {
			errorMessageProperty.set(getResources().getString("error.scales.nonzero.weight"));
			errorVisibleProperty.set(true);
		}
	}

	/**
	 * 
	 */
	class BarcodeWeightListener implements BarcodeListener, MeasurementListener {

		@Override
		public void onCatchBarcode(final String value) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					getContext().setAttribute(Const.BWL_BARCODE, value);
					getContext().setAttribute(Const.BWL_WEIGHT, null);
				}
			});
		}

		@Override
		public void onMeasure(Float value) {
		}

		@Override
		public void onWeightStabled(final Float value) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					getContext().setAttribute(Const.BWL_WEIGHT, value);
				}
			});
		}
	}
}
