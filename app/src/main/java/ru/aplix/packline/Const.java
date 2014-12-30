package ru.aplix.packline;

public final class Const {

	private Const() {

	}

	public static final String APP_NAME = "Aplix Pack Line";

	public static final String FIRST_WORKFLOW_ACTION_BEAN_NAME = "firstWorkflowAction";
	public static final String START_WORKFLOW_ACTION_BEAN_NAME = "startWorkflowAction";
	public static final String GEN_STICK_ACTION_BEAN_NAME = "genStickAction";
	public static final String GEN_STICK_CUSTOMER_ACTION_BEAN_NAME = "genStickCustomerAction";
	public static final String ZEBRA_TEST_ACTION_BEAN_NAME = "zebraTestAction";
	public static final String WARRANTY_CARD_ACTION_BEAN_NAME = "warrantyCardAction";
	public static final String CONTROL_RETURNS_ACTION_BEAN_NAME = "controlReturnsAction";
	public static final String ACTIVE_POSTS_ACTION_BEAN_NAME = "activePostsAction";
	public static final String ASYNC_COMMAND_RUNNER = "asyncCommandRunner";
	public static final String BARCODE_LISTENERS = "barcodeListeners";
	public static final String MEASUREMENT_LISTENERS = "measurementListeners";

	public static final String APPLICATION_CONTEXT = "ApplicationContext";
	public static final String STAGE = "Stage";
	public static final String SCREEN_BOUNDS = "ScreenBounds";
	public static final String CURRENT_WORKFLOW_CONTROLLER = "CurrentWorkflowController";
	public static final String CURRENT_WORKFLOW_ACTION = "CurrentWorkflowAction";
	public static final String OPERATOR = "Operator";
	public static final String TAG = "Tag";
	public static final String ORDER = "Order";
	public static final String REGISTRY = "Registry";
	public static final String ROUTE_LIST = "RouteList";
	public static final String POST = "Post";
	public static final String BARCODE_SCANNER = "BarcodeScanner";
	public static final String PHOTO_CAMERA = "PhotoCamera";
	public static final String DVR_CAMERA = "DVRCamera";
	public static final String SCALES = "Scales";
	public static final String IMAGE_SCANNER = "ImageScanner";
	public static final String JUST_SCANNED_BARCODE = "JustScannedBarcode";
	public static final String POST_SERVICE_PORT = "PostServicePort";
	public static final String EXECUTOR = "Executor";
	public static final String WARNING_MESSAGE = "WarningMessage";
	public static final String WARNING_CODE = "WarningCode";
	public static final String SELECTED_CARRIER = "SelectedCarrier";
	public static final String BWL_BARCODE = "BWL-Barcode";
	public static final String BWL_WEIGHT = "BWL-Wegiht";

	public static final int ERROR_DISPLAY_DELAY = 5;

	public static final String PROPERTY_CONNECT_TIMEOUT = "com.sun.xml.internal.ws.connect.timeout";
	public static final String PROPERTY_REQUEST_TIMEOUT = "com.sun.xml.internal.ws.request.timeout";
	public static final int POST_CONNECT_TIMEOUT = 60000;
	public static final int POST_REQUEST_TIMEOUT = 60000;

	public static final String POST_DATABASE_NAME = "PackLine Post Database";
	public static final String POST_DATASET = "PostDataSet";
	public static final String ENCLOSURE_DATASET = "EnclosuresDataSet";
	public static final String MARKERS_FOR_CONTAINERS_DATASET = "MarkersForContainersDataSet";
	public static final String MARKERS_FOR_CUSTOMERS_DATASET = "MarkersForCustomersDataSet";
	public static final String QUERY_ID_PARAM = "queryId";
	public static final String CONTAINER_ID_PARAM = "containerId";
	public static final String POST_ID_PARAM = "postId";
	public static final String CUSTOMER_CODE_PARAM = "customerCode";
	public static final String COUNT_PARAM = "count";

	public static final String PRINT_MODE_VARIABLE = "PrintMode";
	public static final String CANCEL_PRINTING_VARIABLE = "CancelPrinting";
	public static final String CONTAINER_PROBLEM_VARIABLE = "ContainerProblem";
	public static final String CONTAINER_TRACKING_ID_VARIABLE = "barkod";

	public static final String FR2AFOP_CONF_FILE = "/conf/fr2afop.xconf";
	public static final String FOP_CONF_FILE = "/conf/fop.xconf";
	public static final String REPORT_FILE_TEMPLATE = "/reports/%s.xml";
	public static final String PDF_PRINTER_FILE = "/bin/pdfprinter.exe";
}
