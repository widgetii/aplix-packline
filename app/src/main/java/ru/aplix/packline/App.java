package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.xml.bind.JAXBException;
import javax.xml.ws.BindingProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.HardwareConfiguration;
import ru.aplix.packline.conf.PostService;
import ru.aplix.packline.hardware.Connectable;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.barcode.BarcodeScannerConnectionListener;
import ru.aplix.packline.hardware.barcode.BarcodeScannerFactory;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraConnectionListener;
import ru.aplix.packline.hardware.camera.PhotoCameraFactory;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;
import ru.aplix.packline.hardware.scales.ScalesFactory;
import ru.aplix.packline.idle.IdleListener;
import ru.aplix.packline.idle.UserActivityMonitor;
import ru.aplix.packline.post.PackingLine;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.StandardWorkflowContext;
import ru.aplix.packline.workflow.WorkflowAction;
import ru.aplix.packline.workflow.WorkflowContext;
import ru.aplix.packline.workflow.WorkflowController;

// TODO 35 Scales weight stable algorithm
// TODO 40 Drivers list and registry
// TODO 50 Marking check, scan for track number
// TODO 60 HTTP 401 check and invalid login message
// TODO 90 styles of act-table, menu and quantity buttons
// FIXME logout doesn't work in ubuntu
// FIXME error checker should hide error message after 5s even though barcode scanner is turned off
// FIXME f116: vertical lines shifting 

public class App extends Application implements IdleListener {

	private final Log LOG = LogFactory.getLog(getClass());

	private ApplicationContext applicationContext;
	private WorkflowContext workflowContext;

	private ReentrantLock executorLock;
	private boolean shutdownExecutor;
	private ScheduledExecutorService executor;

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	@Override
	public void init() throws FileNotFoundException, MalformedURLException, JAXBException {
		applicationContext = new ClassPathXmlApplicationContext("/resources/spring/spring-context.xml");

		Configuration.setConfigFileName(getParameters().getNamed().get("config"));
		Configuration.getInstance();

		shutdownExecutor = false;
		executorLock = new ReentrantLock();
		executor = Executors.newSingleThreadScheduledExecutor();
	}

	private void initWindow(final Stage stage, final Rectangle2D screenBounds) {
		stage.setTitle("Aplix Pack Line");
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setResizable(false);

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				stage.setX(screenBounds.getMinX());
				stage.setY(screenBounds.getMinY());
				stage.setWidth(screenBounds.getWidth());
				stage.setHeight(screenBounds.getHeight());
			}
		});
	}

	@Override
	public void start(Stage stage) throws Exception {
		Screen screen = Screen.getPrimary();
		Rectangle2D screenBounds = screen.getVisualBounds();
		initWindow(stage, screenBounds);

		// Starting workflow
		if (workflowContext == null) {
			workflowContext = new StandardWorkflowContext();
			workflowContext.setAttribute(Const.APPLICATION_CONTEXT, applicationContext);
			workflowContext.setAttribute(Const.STAGE, stage);
			workflowContext.setAttribute(Const.SCREEN_BOUNDS, screenBounds);
			workflowContext.setAttribute(Const.EXECUTOR, executor);
		}

		initializeHardware();
		initializeRemoteServices();

		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.FIRST_WORKFLOW_ACTION_BEAN_NAME);
		wa.execute(workflowContext);

		UserActivityMonitor.setTreshold(Configuration.getInstance().getActivityMonitorConfiguration().getIdleTresholdInMillis());
		UserActivityMonitor.getInstance().setIdleListener(this);
		UserActivityMonitor.getInstance().setWorkflowContext(workflowContext);
		UserActivityMonitor.getInstance().start();
	}

	@Override
	public void stop() throws Exception {
		UserActivityMonitor.getInstance().cancel();

		// Terminate current action(controller) if exists
		WorkflowController currentController = (WorkflowController) workflowContext.getAttribute(Const.CURRENT_WORKFLOW_CONTROLLER);
		if (currentController != null) {
			currentController.terminate();
		}

		shutdownScheduler();
		deinitializeHardware();
	}

	@Override
	public void idle() {
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.FIRST_WORKFLOW_ACTION_BEAN_NAME);
		wa.execute(workflowContext);
	}

	private void initializeHardware() throws ClassNotFoundException, FileNotFoundException, MalformedURLException, JAXBException {
		HardwareConfiguration configuration = Configuration.getInstance().getHardwareConfiguration();

		// Create barcode scanner instance
		if (configuration.getBarcodeScanner().isEnabled()) {
			final BarcodeScanner<?> bs = BarcodeScannerFactory.createInstance(configuration.getBarcodeScanner().getName());
			bs.setConnectOnDemand(true);
			bs.setConfiguration(configuration.getBarcodeScanner().getConfiguration());
			bs.addConnectionListener(new BarcodeScannerConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					postConnectToHardware(Const.BARCODE_SCANNER);
				}

				@Override
				public void onConnectionFailed() {
					postConnectToHardware(Const.BARCODE_SCANNER);
				}
			});
			workflowContext.setAttribute(Const.BARCODE_SCANNER, bs);
		}

		// Create photo camera instance
		if (configuration.getPhotoCamera().isEnabled()) {
			final PhotoCamera<?> pc = PhotoCameraFactory.createInstance(configuration.getPhotoCamera().getName());
			pc.setConnectOnDemand(true);
			pc.setConfiguration(configuration.getPhotoCamera().getConfiguration());
			pc.addConnectionListener(new PhotoCameraConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					postConnectToHardware(Const.PHOTO_CAMERA);
				}

				@Override
				public void onConnectionFailed() {
					postConnectToHardware(Const.PHOTO_CAMERA);
				}
			});
			workflowContext.setAttribute(Const.PHOTO_CAMERA, pc);
		}

		// Create scales instance
		if (configuration.getScales().isEnabled()) {
			final Scales<?> sc = ScalesFactory.createInstance(configuration.getScales().getName());
			sc.setConnectOnDemand(true);
			sc.setConfiguration(configuration.getScales().getConfiguration());
			sc.addConnectionListener(new ScalesConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					postConnectToHardware(Const.SCALES);
				}

				@Override
				public void onConnectionFailed() {
					postConnectToHardware(Const.SCALES);
				}
			});
			workflowContext.setAttribute(Const.SCALES, sc);
		}
	}

	private void deinitializeHardware() {
		// Stop barcode scanner
		BarcodeScanner<?> bs = (BarcodeScanner<?>) workflowContext.getAttribute(Const.BARCODE_SCANNER);
		if (bs != null) {
			bs.disconnect();
		}

		// Stop photo camera
		PhotoCamera<?> pc = (PhotoCamera<?>) workflowContext.getAttribute(Const.PHOTO_CAMERA);
		if (pc != null) {
			pc.disconnect();
		}

		// Stop scales
		Scales<?> sc = (Scales<?>) workflowContext.getAttribute(Const.SCALES);
		if (sc != null) {
			sc.disconnect();
		}
	}

	private void shutdownScheduler() {
		executorLock.lock();
		try {
			shutdownExecutor = true;

			try {
				if (executor != null) {
					executor.shutdown();
				}
			} catch (Exception e) {
				LOG.error(null, e);
			}
		} finally {
			executorLock.unlock();
		}
	}

	private void postConnectToHardware(final String hardwareName) {
		executorLock.lock();
		try {
			if (shutdownExecutor) {
				return;
			}

			try {
				if (executor == null) {
					executor = Executors.newSingleThreadScheduledExecutor();
				}

				executor.schedule(new Runnable() {
					@Override
					public void run() {
						executorLock.lock();
						try {
							// Execute task only if executor is still active
							if (!shutdownExecutor) {
								Connectable c = (Connectable) workflowContext.getAttribute(hardwareName);
								c.connect();
							}
						} finally {
							executorLock.unlock();
						}
					}
				}, Configuration.getInstance().getHardwareConfiguration().getReconnectInterval(), TimeUnit.SECONDS);
			} catch (Exception e) {
				LOG.error(null, e);
			}
		} finally {
			executorLock.unlock();
		}
	}

	private void initializeRemoteServices() throws FileNotFoundException, MalformedURLException, JAXBException {
		PackingLine postService = new PackingLine();
		PackingLinePortType postServicePort = postService.getPackingLineSoap();

		if (postServicePort instanceof BindingProvider) {
			PostService psConf = Configuration.getInstance().getPostService();

			Map<String, Object> requestContext = ((BindingProvider) postServicePort).getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, psConf.getServiceAddress());
			requestContext.put(BindingProvider.USERNAME_PROPERTY, psConf.getUserName());
			requestContext.put(BindingProvider.PASSWORD_PROPERTY, psConf.getPassword());
		}

		workflowContext.setAttribute(Const.POST_SERVICE_PORT, postServicePort);
	}
}
