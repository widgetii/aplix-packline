package ru.aplix.packline.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MultipleDocumentHandling;
import javax.xml.ws.BindingProvider;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.MimeConstants;

import ru.aplix.converters.fr2afop.app.RenderXMLToOutputImpl;
import ru.aplix.converters.fr2afop.database.ValueResolver;
import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.Variable;
import ru.aplix.converters.fr2afop.fr.dataset.Connection;
import ru.aplix.converters.fr2afop.fr.dataset.Database;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Parameter;
import ru.aplix.converters.fr2afop.reader.InputStreamOpener;
import ru.aplix.converters.fr2afop.reader.ReportReader;
import ru.aplix.converters.fr2afop.reader.XMLReportReader;
import ru.aplix.converters.fr2afop.utils.PrintAttributesResolver;
import ru.aplix.converters.fr2afop.writer.OutputStreamOpener;
import ru.aplix.converters.fr2afop.writer.ReportWriter;
import ru.aplix.converters.fr2afop.writer.XMLReportWriter;
import ru.aplix.packline.Const;
import ru.aplix.packline.ContainerProblemException;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.conf.PrintMode;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.controller.PrintFormsController;
import ru.aplix.packline.jdbc.PostDriver;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowAction;

public class PrintFormsAction extends CommonAction<PrintFormsController> {

	private final Log LOG = LogFactory.getLog(getClass());

	private WorkflowAction weightingAction;
	private WorkflowAction normalAction;

	private static final int[] REPORT_TYPE_ZEBRA = { 105 };
	private static final int[] REPORT_TYPE_FRF = { 21, 22, 23 };

	private static final int BUFFER_SIZE = 10240;

	private ru.aplix.converters.fr2afop.fr.Configuration configuration = null;
	private Parameter containerIdParam = null;
	private Parameter postIdParam = null;
	private List<Parameter> queryIdParams = null;

	private String jarFolder = null;
	private File r2afopConfigFile;
	private String fr2afopConfigFileName;
	private String fopConfigFileName;
	private String queryId;

	public PrintFormsAction() {
		queryIdParams = new ArrayList<Parameter>();
	}

	public WorkflowAction getWeightingAction() {
		return weightingAction;
	}

	public void setWeightingAction(WorkflowAction weightingAction) {
		this.weightingAction = weightingAction;
	}

	public WorkflowAction getNormalAction() {
		return normalAction;
	}

	public void setNormalAction(WorkflowAction normalAction) {
		this.normalAction = normalAction;
	}

	@Override
	protected String getFormName() {
		return "printing";
	}

	public void prepare() {
		// updateQueryId
		queryId = RandomStringUtils.randomAlphanumeric(15);
	}

	public void markAsProblem(String code) throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.markAsProblem(container.getId(), code)) {
			throw new PackLineException(getResources().getString("error.post.mark.problem"));
		}
	}

	public boolean processBarcode(String code) throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);

		boolean match = container != null && code != null && code.equals(container.getTrackingId());
		if (match) {
			PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
			if (!postServicePort.markPostAsShipped(container.getId())) {
				throw new PackLineException(getResources().getString("error.post.container.mark.shipped"));
			}

			setNextAction(getNormalAction());
		}
		return match;
	}

	public void printForms(String containerId, String postId, PrintForm printForm) throws PackLineException {
		if (printForm.getPrinter() == null) {
			throw new PackLineException(getResources().getString("error.printer.not.assigned"));
		}

		try {
			if (jarFolder == null) {
				File confFolder = new File(Configuration.getConfigFileName()).getParentFile();
				jarFolder = confFolder != null ? confFolder.getParent() : "";
				fr2afopConfigFileName = jarFolder + Const.FR2AFOP_CONF_FILE;
				fopConfigFileName = jarFolder + Const.FOP_CONF_FILE;
			}

			String reportFileName = jarFolder + String.format(Const.REPORT_FILE_TEMPLATE, printForm.getFile());
			printForm(containerId, postId, reportFileName, printForm.getPrinter(), printForm.getName(), printForm.getCopies());
		} catch (PackLineException ple) {
			throw ple;
		} catch (Throwable e) {
			PackLineException ple = new PackLineException(String.format(getResources().getString("error.printing"), printForm.getPrinter().getName()), e);
			throw ple;
		}
	}

	public void printForm(String containerId, String postId, final String reportFileName, Printer printer, String formName, Integer copies) throws Exception {
		// Get config file
		if (configuration == null) {
			r2afopConfigFile = new File(fr2afopConfigFileName);
			configuration = ru.aplix.converters.fr2afop.utils.Utils.fileToObject(r2afopConfigFile, ru.aplix.converters.fr2afop.fr.Configuration.class);
			prepareDatabase(configuration);
		}

		// Read report from file
		ReportReader reportReader = new XMLReportReader();
		Report report = reportReader.readFromStream(new InputStreamOpener() {
			@Override
			public InputStream openStream() throws IOException {
				return new FileInputStream(reportFileName);
			}
		}, null, configuration);

		// Select data from datasets
		if (containerIdParam != null) {
			containerIdParam.setValue(containerId);
		}
		if (postIdParam != null) {
			postIdParam.setValue(postId);
		}
		for (Parameter queryIdParam : queryIdParams) {
			queryIdParam.setValue(queryId);
		}
		ValueResolver vr = new ValueResolver();
		vr.resolve(report, configuration);

		// Check if the report determined to cancel printing
		checkPrintingStatus(report);

		// Render report
		if (ArrayUtils.contains(REPORT_TYPE_FRF, report.getFileVersion())) {
			printUsingApacheFop(report, printer, formName, copies);
		} else if (ArrayUtils.contains(REPORT_TYPE_ZEBRA, report.getFileVersion())) {
			printOnZebraPrepared(report, printer, copies);
		} else {
			throw new PackLineException(getResources().getString("error.report.invalid.type"));
		}
	}

	private void printUsingApacheFop(Report report, final Printer printer, final String formName, final Integer copies) throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
		try {
			// Convert report to XMl
			ReportWriter reportWriter = new XMLReportWriter();
			reportWriter.writeToStream(report, new OutputStreamOpener() {
				@Override
				public OutputStream openStream() throws IOException {
					return baos;
				}
			});

			// Render report using Apache FOP
			ByteArrayOutputStream baos2 = null;
			try {
				RenderXMLToOutputImpl rxto = new RenderXMLToOutputImpl();
				rxto.setFopConfigFileName(fopConfigFileName);
				rxto.setInputStreamOpener(new InputStreamOpener() {
					@Override
					public InputStream openStream() throws IOException {
						return new ByteArrayInputStream(baos.toByteArray());
					}
				});

				// Select renderer
				if (PrintMode.JAVA2D.equals(printer.getPrintMode())) {
					rxto.setOutputFileName(printer.getName());
					rxto.setOutputFormat(MimeConstants.MIME_FOP_PRINT);
					rxto.setPrintAttributesResolver(new PrintAttributesResolver() {
						@Override
						public PrintRequestAttributeSet createPrintAttributes(PrintService printService) {
							return createPrintAttributesFromList(printService, printer.getMediaAttributes(), formName, copies);
						}
					});
				} else if (PrintMode.POSTSCRIPT.equals(printer.getPrintMode())) {
					final ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
					baos2 = memoryBuffer;

					rxto.setOutputFormat(MimeConstants.MIME_POSTSCRIPT);
					rxto.setOutputStreamOpener(new OutputStreamOpener() {
						@Override
						public OutputStream openStream() throws IOException {
							return memoryBuffer;
						}
					});
				} else if (PrintMode.PCL.equals(printer.getPrintMode())) {
					final ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
					baos2 = memoryBuffer;

					rxto.setOutputFormat(MimeConstants.MIME_PCL);
					rxto.setOutputStreamOpener(new OutputStreamOpener() {
						@Override
						public OutputStream openStream() throws IOException {
							return memoryBuffer;
						}
					});
				} else {
					throw new PackLineException(String.format(getResources().getString("error.print.mode.invalid"), printer.getPrintMode()));
				}

				// Go!
				rxto.execute();

				// If we rendered in memory buffer, then the contents
				// of this buffer should be sent to printer directly
				if (baos2 != null) {
					Utils.sendDataToSocket(printer.getIpAddress(), printer.getPort(), baos2.toByteArray(), copies);
				}
			} finally {
				if (baos2 != null) {
					baos2.close();
				}
			}
		} finally {
			if (baos != null) {
				baos.close();
			}
		}
	}

	private void printOnZebraPrepared(Report report, Printer printer, Integer copies) throws Exception {
		File xsltFileAfter = null;
		if (configuration.getReplacementFile().getAfter() != null) {
			xsltFileAfter = new File(configuration.getReplacementFile().getAfter());
			if (xsltFileAfter == null || !xsltFileAfter.exists()) {
				xsltFileAfter = new File(r2afopConfigFile.getParent(), configuration.getReplacementFile().getAfter());
			}
		}

		final ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
		try {
			ReportWriter reportWriter = new XMLReportWriter(xsltFileAfter);
			reportWriter.writeToStream(report, new OutputStreamOpener() {
				@Override
				public OutputStream openStream() throws IOException {
					return baos;
				}
			});

			// Send xml to printer directly
			Utils.sendDataToSocket(printer.getIpAddress(), printer.getPort(), baos.toByteArray(), copies);
		} finally {
			baos.close();
		}
	}

	private void prepareDatabase(ru.aplix.converters.fr2afop.fr.Configuration configuration) {
		// Modify database connection settings
		for (Database database : configuration.getDatabases()) {
			if (Const.POST_DATABASE_NAME.equalsIgnoreCase(database.getName())) {
				Connection connection = new Connection();
				connection.setDriver(PostDriver.class.getName());
				database.setConnection(connection);

				PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
				if (postServicePort instanceof BindingProvider) {
					Map<String, Object> requestContext = ((BindingProvider) postServicePort).getRequestContext();

					connection.setUrl((String) requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
					connection.setUserName((String) requestContext.get(BindingProvider.USERNAME_PROPERTY));
					connection.setPassword((String) requestContext.get(BindingProvider.PASSWORD_PROPERTY));
				}
			}
		}

		// Get reference to container Id parameter
		for (Dataset dataset : configuration.getDatasets()) {
			if (Const.POST_DATASET.equals(dataset.getName())) {
				for (Parameter parameter : dataset.getParameters()) {
					if (Const.CONTAINER_ID_PARAM.equals(parameter.getName())) {
						containerIdParam = parameter;
					}
					if (Const.QUERY_ID_PARAM.equals(parameter.getName())) {
						if (!queryIdParams.contains(parameter)) {
							queryIdParams.add(parameter);
						}
					}
				}
			} else if (Const.ENCLOSURE_DATASET.equals(dataset.getName())) {
				for (Parameter parameter : dataset.getParameters()) {
					if (Const.POST_ID_PARAM.equals(parameter.getName())) {
						postIdParam = parameter;
					}
					if (Const.QUERY_ID_PARAM.equals(parameter.getName())) {
						if (!queryIdParams.contains(parameter)) {
							queryIdParams.add(parameter);
						}
					}
				}
			}
		}
	}

	private PrintRequestAttributeSet createPrintAttributesFromList(PrintService printService, List<String> attrNames, String formName, Integer copies) {
		// Checking input array
		if (attrNames == null || attrNames.size() == 0) {
			return null;
		}

		// Create return object
		PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
		attributes.add(new JobName(String.format("%s - %s", Const.APP_NAME, formName), Locale.getDefault()));
		if (copies != null && copies > 0) {
			attributes.add(new Copies(copies));
			attributes.add(MultipleDocumentHandling.SINGLE_DOCUMENT);
		}
		int oldAttrSize = attributes.size();

		// Get list of supported attributes
		Object o = printService.getSupportedAttributeValues(Media.class, DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
		if (o != null && o.getClass().isArray()) {
			Media[] medias = (Media[]) o;

			// Sort list of attributes
			Arrays.sort(medias, new Comparator<Media>() {
				@Override
				public int compare(Media o1, Media o2) {
					int res = o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
					if (res != 0) {
						return res;
					}
					res = Integer.compare(o1.getValue(), o2.getValue());
					if (res != 0) {
						return res;
					}
					return o1.toString().compareTo(o2.toString());
				}
			});

			// Find attribute by name and add it to result set
			for (String attrName : attrNames) {
				boolean added = false;
				for (Media media : medias) {
					if (media.toString().equals(attrName)) {
						attributes.add((Media) media.clone());
						added = true;
					}
				}

				// If attribute not found, notify about that
				if (!added) {
					LOG.warn(String.format("Attribute \"%s\" is not supported by \"%s\"", attrName, printService.getName()));
				}
			}

			// If at least one attribute was not added,
			// then list all of them for info
			if ((attributes.size() - oldAttrSize) != attrNames.size()) {
				LOG.info(String.format("Enumerating supported attributes for \"%s\":", printService.getName()));
				for (Media media : medias) {
					LOG.info(String.format("  %s: %s", media.getClass().getSimpleName(), media));
				}
			}
		}

		return attributes;
	}

	private void checkPrintingStatus(Report report) throws PackLineException {
		// Check container problem
		Variable variable = (Variable) CollectionUtils.find(report.getVariables(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return Const.CONTAINER_PROBLEM_VARIABLE.equals(((Variable) item).getName());
			}
		});

		if (variable != null && variable.getValue() != null && variable.getValue().length() > 0) {
			if ("SenderAddress".equalsIgnoreCase(variable.getValue())) {
				throw new ContainerProblemException(getResources().getString("error.post.container.problem.address.sender"), variable.getValue());
			} else if ("ReceiverAddress".equalsIgnoreCase(variable.getValue())) {
				throw new ContainerProblemException(getResources().getString("error.post.container.problem.address.receiver"), variable.getValue());
			} else {
				throw new ContainerProblemException(String.format(getResources().getString("error.post.container.problem.other"), variable.getValue()),
						variable.getValue());
			}
		}
	}
}
