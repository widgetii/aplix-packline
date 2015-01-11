package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.HardwareConfiguration;
import ru.aplix.packline.conf.PostService;
import ru.aplix.packline.hardware.Connectable;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.barcode.BarcodeScannerConnectionListener;
import ru.aplix.packline.hardware.barcode.BarcodeScannerFactory;
import ru.aplix.packline.hardware.camera.DVRCamera;
import ru.aplix.packline.hardware.camera.DVRCameraConnectionListener;
import ru.aplix.packline.hardware.camera.DVRCameraFactory;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraConnectionListener;
import ru.aplix.packline.hardware.camera.PhotoCameraFactory;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;
import ru.aplix.packline.hardware.scales.ScalesFactory;
import ru.aplix.packline.hardware.scanner.ImageScanner;
import ru.aplix.packline.hardware.scanner.ImageScannerConnectionListener;
import ru.aplix.packline.hardware.scanner.ImageScannerFactory;
import ru.aplix.packline.idle.IdleListener;
import ru.aplix.packline.idle.UserActivityMonitor;
import ru.aplix.packline.post.PackingLine;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.StandardWorkflowContext;
import ru.aplix.packline.workflow.WorkflowAction;
import ru.aplix.packline.workflow.WorkflowContext;
import ru.aplix.packline.workflow.WorkflowController;

public class App extends Application implements IdleListener {

	private final Log LOG = LogFactory.getLog(getClass());

	private static App instance = null;

	private ApplicationContext applicationContext;
	private WorkflowContext workflowContext;

	private ReentrantLock executorLock;
	private boolean shutdownExecutor;
	private ScheduledExecutorService executor;

	private List<Runnable> postStopActions = new ArrayList<Runnable>();

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	public static App getInstance() {
		if (instance == null) {
			throw new IllegalStateException();
		}
		return instance;
	}

	@Override
	public void init() throws FileNotFoundException, MalformedURLException, JAXBException {
		instance = this;

		if ("true".equalsIgnoreCase(getParameters().getNamed().get("debug"))) {
			Logger.getRootLogger().setLevel(Level.DEBUG);
			Logger.getRootLogger().info("Debug mode enabled.");
		}

		applicationContext = new ClassPathXmlApplicationContext("/resources/spring/spring-context.xml");

		Configuration.setConfigFileName(getParameters().getNamed().get("config"));
		Configuration.getInstance();

		shutdownExecutor = false;
		executorLock = new ReentrantLock();
		executor = Executors.newSingleThreadScheduledExecutor();
	}

	private void initWindow(final Stage stage, final Rectangle2D screenBounds) {
		stage.setTitle(Const.APP_NAME);
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

		UserActivityMonitor.setTreshold(Configuration.getInstance().getActivityMonitorConfiguration().getIdleShortTresholdInMillis());
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
			currentController.terminate(true);
		}

		shutdownScheduler();
		deinitializeHardware();
		executePostStopActions();
	}

	@Override
	public void idle() {
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.FIRST_WORKFLOW_ACTION_BEAN_NAME);
		wa.execute(workflowContext);
	}

	private void initializeHardware() throws ClassNotFoundException, FileNotFoundException, MalformedURLException, JAXBException {
		HardwareConfiguration configuration = Configuration.getInstance().getHardwareConfiguration();

		// Create barcode scanner instance
		if (!StringUtils.isEmpty(configuration.getBarcodeScanner().getName()) && configuration.getBarcodeScanner().isEnabled()) {
			BarcodeScanner<?> bs = BarcodeScannerFactory.createInstance(configuration.getBarcodeScanner().getName());

			// Proxy barcode scanner in order to intercept listener methods
			ProxyFactory factory = new ProxyFactory(bs);
			BarcodeListenerStaticMethodMatcherPointcut blsmmp = new BarcodeListenerStaticMethodMatcherPointcut();
			factory.addAdvisor(new DefaultPointcutAdvisor(blsmmp, blsmmp));
			bs = (BarcodeScanner<?>) factory.getProxy();

			// Continue with proxied barcode scanner
			bs.setConnectOnDemand(true);
			bs.setConfiguration(configuration.getBarcodeScanner().getConfiguration());
			bs.addConnectionListener(new BarcodeScannerConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					// A workaround for http://jira.aplix.ru/browse/APLXPOST-328
					Scales<?> sc = (Scales<?>) workflowContext.getAttribute(Const.SCALES);
					if (sc != null && sc.isConnected()) {
						sc.disconnect();
					}
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
		if (!StringUtils.isEmpty(configuration.getPhotoCamera().getName()) && configuration.getPhotoCamera().isEnabled()) {
			PhotoCamera<?> pc = PhotoCameraFactory.createInstance(configuration.getPhotoCamera().getName());
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

		// Create DVR camera instance
		if (!StringUtils.isEmpty(configuration.getDVRCamera().getName()) && configuration.getDVRCamera().isEnabled()) {
			final DVRCamera<?> dc = DVRCameraFactory.createInstance(configuration.getDVRCamera().getName());
			dc.setConnectOnDemand(true);
			dc.setConfiguration(configuration.getDVRCamera().getConfiguration());
			dc.addConnectionListener(new DVRCameraConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					postConnectToHardware(Const.DVR_CAMERA);
				}

				@Override
				public void onConnectionFailed() {
					postConnectToHardware(Const.DVR_CAMERA);
				}
			});
			workflowContext.setAttribute(Const.DVR_CAMERA, dc);
		}

		// Create scales instance
		if (!StringUtils.isEmpty(configuration.getScales().getName()) && configuration.getScales().isEnabled()) {
			Scales<?> sc = ScalesFactory.createInstance(configuration.getScales().getName());

			// Proxy scales in order to intercept listener methods
			ProxyFactory factory = new ProxyFactory(sc);
			ScalesListenerStaticMethodMatcherPointcut slsmmp = new ScalesListenerStaticMethodMatcherPointcut();
			factory.addAdvisor(new DefaultPointcutAdvisor(slsmmp, slsmmp));
			sc = (Scales<?>) factory.getProxy();

			sc.setConnectOnDemand(true);
			sc.setConfiguration(configuration.getScales().getConfiguration());
			sc.addConnectionListener(new ScalesConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					// A workaround for http://jira.aplix.ru/browse/APLXPOST-328
					BarcodeScanner<?> bc = (BarcodeScanner<?>) workflowContext.getAttribute(Const.BARCODE_SCANNER);
					if (bc != null && bc.isConnected()) {
						bc.disconnect();
					}
					postConnectToHardware(Const.SCALES);
				}

				@Override
				public void onConnectionFailed() {
					postConnectToHardware(Const.SCALES);
				}
			});
			workflowContext.setAttribute(Const.SCALES, sc);
		}

		// Create scales instance
		if (!StringUtils.isEmpty(configuration.getImageScanner().getName()) && configuration.getImageScanner().isEnabled()) {
			final ImageScanner<?> is = ImageScannerFactory.createInstance(configuration.getImageScanner().getName());
			is.setConnectOnDemand(true);
			is.setConfiguration(configuration.getImageScanner().getConfiguration());
			is.addConnectionListener(new ImageScannerConnectionListener() {
				@Override
				public void onConnected() {
				}

				@Override
				public void onDisconnected() {
					postConnectToHardware(Const.IMAGE_SCANNER);
				}

				@Override
				public void onConnectionFailed() {
					postConnectToHardware(Const.IMAGE_SCANNER);
				}
			});
			workflowContext.setAttribute(Const.IMAGE_SCANNER, is);
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

		// Stop DVR camera
		DVRCamera<?> dc = (DVRCamera<?>) workflowContext.getAttribute(Const.DVR_CAMERA);
		if (dc != null) {
			dc.disableRecording();
			dc.disconnect();
		}

		// Stop scales
		Scales<?> sc = (Scales<?>) workflowContext.getAttribute(Const.SCALES);
		if (sc != null) {
			sc.disconnect();
		}

		// Stop scales
		ImageScanner<?> is = (ImageScanner<?>) workflowContext.getAttribute(Const.IMAGE_SCANNER);
		if (is != null) {
			is.disconnect();
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
				}, Math.max(Configuration.getInstance().getHardwareConfiguration().getReconnectInterval(), 1), TimeUnit.SECONDS);
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
			requestContext.put(Const.PROPERTY_CONNECT_TIMEOUT, Const.POST_CONNECT_TIMEOUT);
			requestContext.put(Const.PROPERTY_REQUEST_TIMEOUT, Const.POST_REQUEST_TIMEOUT);
		}

		workflowContext.setAttribute(Const.POST_SERVICE_PORT, postServicePort);
	}

	public List<Runnable> getPostStopActions() {
		return postStopActions;
	}

	private void executePostStopActions() {
		if (postStopActions != null) {
			for (Runnable r : postStopActions) {
				r.run();
			}
		}
	}

	/**
	 *
	 */
	class BarcodeListenerStaticMethodMatcherPointcut extends StaticMethodMatcherPointcut implements AfterReturningAdvice {

		@Override
		public boolean matches(Method method, Class<?> cls) {
			return method.getName().matches("(add|remove)BarcodeListener");
		}

		@Override
		public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
			if (args != null && args.length > 0 && args[0] instanceof BarcodeListener) {
				BarcodeListener listener = (BarcodeListener) args[0];

				@SuppressWarnings("unchecked")
				List<BarcodeListener> list = (List<BarcodeListener>) applicationContext.getBean(Const.BARCODE_LISTENERS);

				if (method.getName().startsWith("add")) {
					list.add(listener);
				} else {
					list.remove(listener);
				}
			}
		}
	}

	/**
	 *
	 */
	class ScalesListenerStaticMethodMatcherPointcut extends StaticMethodMatcherPointcut implements AfterReturningAdvice {

		@Override
		public boolean matches(Method method, Class<?> cls) {
			return method.getName().matches("(add|remove)MeasurementListener");
		}

		@Override
		public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
			if (args != null && args.length > 0 && args[0] instanceof MeasurementListener) {
				MeasurementListener listener = (MeasurementListener) args[0];

				@SuppressWarnings("unchecked")
				List<MeasurementListener> list = (List<MeasurementListener>) applicationContext.getBean(Const.MEASUREMENT_LISTENERS);

				if (method.getName().startsWith("add")) {
					list.add(listener);
				} else {
					list.remove(listener);
				}
			}
		}
	}
}
