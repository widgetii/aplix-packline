package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.AuthAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.workflow.WorkflowContext;

public class AuthController extends StandardController<AuthAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	private BarcodeScanner<?> barcodeScanner = null;

	private Task<Operator> task;

	@Override
	public void prepare(WorkflowContext context) {
		context.setAttribute(Const.OPERATOR, null);
		super.prepare(context);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}
	}

	@Override
	public void terminate() {
		super.terminate();

		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
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
		if (progressVisibleProperty.get()) {
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
		if ((barcodeScanner != null) && barcodeScanner.isConnected()) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.barcode.scanner"));
			errorVisibleProperty.set(true);

			return false;
		}
	}
}
