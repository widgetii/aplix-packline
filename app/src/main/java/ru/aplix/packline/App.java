package ru.aplix.packline;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.xml.bind.JAXBException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.barcode.BarcodeScannerFactory;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraFactory;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesFactory;
import ru.aplix.packline.idle.IdleListener;
import ru.aplix.packline.idle.UserActivityMonitor;
import ru.aplix.packline.workflow.StandardWorkflowContext;
import ru.aplix.packline.workflow.WorkflowAction;
import ru.aplix.packline.workflow.WorkflowContext;
import ru.aplix.packline.workflow.WorkflowController;

public class App extends Application implements IdleListener {

    private ApplicationContext applicationContext;
    private WorkflowContext workflowContext;

    public static void main(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void init() throws FileNotFoundException, MalformedURLException, JAXBException {
        applicationContext = new ClassPathXmlApplicationContext("/resources/spring/spring-context.xml");

        Configuration.setConfigFileName(getParameters().getNamed().get("config"));
        Configuration.getInstance();
    }

    private void initWindow(Stage stage, Rectangle2D screenBounds) {
        stage.setTitle("Aplix Pack Line");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
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
        }

        initializeHardware();

        WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.FIRST_WORKFLOW_ACTION_BEAN_NAME);
        wa.execute(workflowContext);

        UserActivityMonitor.setTreshold(Configuration.getInstance().getActivityMonitorConfiguration().getIdleTresholdInMillis());
        UserActivityMonitor.getInstance().setIdleListener(this);
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

        deinitializeHardware();
    }

    @Override
    public void idle() {
        WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.FIRST_WORKFLOW_ACTION_BEAN_NAME);
        wa.execute(workflowContext);
    }

    private void initializeHardware() throws ClassNotFoundException, FileNotFoundException, MalformedURLException, JAXBException {
        Configuration configuration = Configuration.getInstance();

        // Create barcode scanner instance
        BarcodeScanner<?> bs = BarcodeScannerFactory.createAnyInstance();
        bs.setConnectOnDemand(true);
        bs.setConfiguration(configuration.getHardwareConfiguration().getBarcodeScanner());
        workflowContext.setAttribute(Const.BARCODE_SCANNER, bs);

        // Create photo camera instance
        PhotoCamera<?> pc = PhotoCameraFactory.createAnyInstance();
        pc.setConnectOnDemand(true);
        pc.setConfiguration(configuration.getHardwareConfiguration().getPhotoCamera());
        workflowContext.setAttribute(Const.PHOTO_CAMERA, pc);

        // Create scales instance
        Scales<?> sc = ScalesFactory.createAnyInstance();
        sc.setConnectOnDemand(true);
        sc.setConfiguration(configuration.getHardwareConfiguration().getScales());
        workflowContext.setAttribute(Const.SCALES, sc);
    }

    private void deinitializeHardware() {
        // Stop barcode scanner
        BarcodeScanner<?> bs = (BarcodeScanner<?>) workflowContext.getAttribute(Const.BARCODE_SCANNER);
        bs.disconnect();

        // Stop photo camera
        PhotoCamera<?> pc = (PhotoCamera<?>) workflowContext.getAttribute(Const.PHOTO_CAMERA);
        pc.disconnect();

        // Stop scales
        Scales<?> sc = (Scales<?>) workflowContext.getAttribute(Const.SCALES);
        sc.disconnect();
    }
}
