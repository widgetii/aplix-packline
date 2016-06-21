package ru.aplix.packline.action;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.MimeConstants;
import ru.aplix.converters.fr2afop.app.RenderXMLToOutputImpl;
import ru.aplix.converters.fr2afop.database.ValueResolver;
import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.dataset.Connection;
import ru.aplix.converters.fr2afop.fr.dataset.Database;
import ru.aplix.converters.fr2afop.reader.ReportReader;
import ru.aplix.converters.fr2afop.reader.XMLReportReader;
import ru.aplix.converters.fr2afop.writer.ReportWriter;
import ru.aplix.converters.fr2afop.writer.XMLReportWriter;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintMode;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.jdbc.PostDriver;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.StandardWorkflowController;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MultipleDocumentHandling;
import javax.xml.ws.BindingProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BasePrintAction<Controller extends StandardWorkflowController<?>> extends CommonAction<Controller> {

	protected final Log LOG = LogFactory.getLog(getClass());

	private static final int[] REPORT_TYPE_ZEBRA = { 105 };
	private static final int[] REPORT_TYPE_FRF = { 21, 22, 23, 172 };

	private static final int BUFFER_SIZE = 10240;

	private ru.aplix.converters.fr2afop.fr.Configuration configuration = null;

	private String jarFolder = null;
	private File r2afopConfigFile;
	private String fr2afopConfigFileName;
	private String fopConfigFileName;

	public BasePrintAction() {

	}

	protected String getJarFolder() {
		if (jarFolder == null) {
			File confFolder = new File(Configuration.getConfigFileName()).getParentFile();
			jarFolder = confFolder != null ? confFolder.getParent() : "";
			fr2afopConfigFileName = jarFolder + Const.FR2AFOP_CONF_FILE;
			fopConfigFileName = jarFolder + Const.FOP_CONF_FILE;
		}
		return jarFolder;
	}

	protected void printFromFile(final String reportFileName, Printer printer, String formName, Integer copies)
			throws Exception {
		// Get config file
		if (configuration == null) {
			r2afopConfigFile = new File(fr2afopConfigFileName);
			configuration = ru.aplix.converters.fr2afop.utils.Utils.fileToObject(r2afopConfigFile, ru.aplix.converters.fr2afop.fr.Configuration.class);
			prepareDatabase(configuration);
		}

		// Read report from file
		ReportReader reportReader = new XMLReportReader();
		Report report = reportReader.readFromStream(() -> new FileInputStream(reportFileName), null, configuration);

		beforeResolving(report);
		try {
			ValueResolver vr = new ValueResolver();
			vr.resolve(report, configuration);
		} finally {
			if (!afterResolving(report)) {
				return;
			}
		}

		// Render report
		if (ArrayUtils.contains(REPORT_TYPE_FRF, report.getFileVersion())) {
			printUsingApacheFop(report, printer, formName, copies);
		} else if (ArrayUtils.contains(REPORT_TYPE_ZEBRA, report.getFileVersion())) {
			printOnZebraPrepared(report, printer, copies);
		} else {
			throw new PackLineException(getResources().getString("error.report.invalid.type"));
		}
	}

	protected void beforeResolving(Report report) {

	}

	protected boolean afterResolving(Report report) throws PackLineException {
		return true;
	}

	private void printUsingApacheFop(Report report, final Printer printer, final String formName, final Integer copies) throws Exception {
		File xsltFileAfter = null;
		if (configuration.getReplacementFile().getAfter() != null) {
			xsltFileAfter = new File(configuration.getReplacementFile().getAfter());
			if (!xsltFileAfter.exists()) {
				xsltFileAfter = new File(r2afopConfigFile.getParent(), configuration.getReplacementFile().getAfter());
			}
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE)) {
			// Convert report to XMl
			ReportWriter reportWriter = new XMLReportWriter(xsltFileAfter);
			reportWriter.writeToStream(report, () -> baos);

			// Render report using Apache FOP
			ByteArrayOutputStream baos2 = null;
			try {
				RenderXMLToOutputImpl rxto = new RenderXMLToOutputImpl();
				rxto.setFopConfigFileName(fopConfigFileName);
				rxto.setInputStreamOpener(() -> new ByteArrayInputStream(baos.toByteArray()));

				// Select renderer
				if (PrintMode.JAVA2D.equals(printer.getPrintMode()) || PrintMode.JAVA2D_WO_COPIES.equals(printer.getPrintMode())) {
					rxto.setOutputFileName(printer.getName());
					rxto.setOutputFormat(MimeConstants.MIME_FOP_PRINT);
					rxto.setPrintAttributesResolver(printService -> createPrintAttributesFromList(printService, printer.getMediaAttributes(), formName,
							PrintMode.JAVA2D_WO_COPIES.equals(printer.getPrintMode()) ? 1 : (copies != null && copies > 0) ? copies : 1));
				} else if (PrintMode.POSTSCRIPT.equals(printer.getPrintMode())) {
					final ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
					baos2 = memoryBuffer;

					rxto.setOutputFormat(MimeConstants.MIME_POSTSCRIPT);
					rxto.setOutputStreamOpener(() -> memoryBuffer);
				} else if (PrintMode.PCL.equals(printer.getPrintMode())) {
					final ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
					baos2 = memoryBuffer;

					rxto.setOutputFormat(MimeConstants.MIME_PCL);
					rxto.setOutputStreamOpener(() -> memoryBuffer);
				} else if (PrintMode.PDF.equals(printer.getPrintMode())) {
					final ByteArrayOutputStream memoryBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
					baos2 = memoryBuffer;

					rxto.setOutputFormat(MimeConstants.MIME_PDF);
					rxto.setOutputStreamOpener(() -> memoryBuffer);
				} else {
					throw new PackLineException(String.format(getResources().getString("error.print.mode.invalid"), printer.getPrintMode()));
				}

				// Go!
				int c = PrintMode.JAVA2D_WO_COPIES.equals(printer.getPrintMode()) ? ((copies != null && copies > 0) ? copies : 1) : 1;
				for (int i = 1; i <= c; i++) {
					rxto.execute();
				}

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
		}
	}

	private void printOnZebraPrepared(Report report, Printer printer, Integer copies) throws Exception {
		File xsltFileAfter = null;
		if (configuration.getReplacementFile().getAfter() != null) {
			xsltFileAfter = new File(configuration.getReplacementFile().getAfter());
			if (!xsltFileAfter.exists()) {
				xsltFileAfter = new File(r2afopConfigFile.getParent(), configuration.getReplacementFile().getAfter());
			}
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE)) {
			ReportWriter reportWriter = new XMLReportWriter(xsltFileAfter);
			reportWriter.writeToStream(report, () -> baos);

			// Send xml to printer directly
			Utils.sendDataToSocket(printer.getIpAddress(), printer.getPort(), baos.toByteArray(), copies);
		}
	}

	protected void prepareDatabase(ru.aplix.converters.fr2afop.fr.Configuration configuration) {
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
			Arrays.sort(medias, (o1, o2) -> {
                int res = o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
                if (res != 0) {
                    return res;
                }
                res = Integer.compare(o1.getValue(), o2.getValue());
                if (res != 0) {
                    return res;
                }
                return o1.toString().compareTo(o2.toString());
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
}
