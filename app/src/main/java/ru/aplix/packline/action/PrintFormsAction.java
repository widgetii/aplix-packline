package ru.aplix.packline.action;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.commons.lang.ArrayUtils;
import org.apache.fop.apps.MimeConstants;

import ru.aplix.converters.fr2afop.app.RenderXMLToOutputImpl;
import ru.aplix.converters.fr2afop.database.ValueResolver;
import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.dataset.Connection;
import ru.aplix.converters.fr2afop.fr.dataset.Database;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Parameter;
import ru.aplix.converters.fr2afop.reader.InputStreamOpener;
import ru.aplix.converters.fr2afop.reader.ReportReader;
import ru.aplix.converters.fr2afop.reader.XMLReportReader;
import ru.aplix.converters.fr2afop.writer.OutputStreamOpener;
import ru.aplix.converters.fr2afop.writer.ReportWriter;
import ru.aplix.converters.fr2afop.writer.XMLReportWriter;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.conf.PrintMode;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.controller.PrintFormsController;
import ru.aplix.packline.jdbc.PostDriver;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.Utils;

public class PrintFormsAction extends CommonAction<PrintFormsController> {

	private static final int[] REPORT_TYPE_ZEBRA = { 105 };
	private static final int[] REPORT_TYPE_FRF = { 21, 22, 23 };

	private static final int BUFFER_SIZE = 10240;

	private static final String FR2AFOP_CONF_FILE = "/conf/fr2afop.xconf";
	private static final String FOP_CONF_FILE = "/conf/fop.xconf";
	private static final String REPORT_FILE_TEMPLATE = "/reports/%s.xml";
	private static final String OUTPUT_FILE_TEMPLATE = "/reports/%s.pdf";

	private ru.aplix.converters.fr2afop.fr.Configuration configuration = null;
	private List<Parameter> containerIdParams;

	private String jarFolder = null;
	private File r2afopConfigFile;
	private String fr2afopConfigFileName;
	private String fopConfigFileName;

	public PrintFormsAction() {
		containerIdParams = new ArrayList<Parameter>();
	}

	@Override
	protected String getFormName() {
		return "printing";
	}

	public void printForms(String containerId, PrintForm printForm) throws Exception {
		if (printForm.getPrinter() == null) {
			throw new PackLineException(getResources().getString("error.printer.not.assigned"));
		}

		if (jarFolder == null) {
			File confFolder = new File(Configuration.getConfigFileName()).getParentFile();
			jarFolder = confFolder != null ? confFolder.getParent() : "";
			fr2afopConfigFileName = jarFolder + FR2AFOP_CONF_FILE;
			fopConfigFileName = jarFolder + FOP_CONF_FILE;
		}

		String reportFileName = jarFolder + String.format(REPORT_FILE_TEMPLATE, printForm.getFile());
		String outputFileName = jarFolder + String.format(OUTPUT_FILE_TEMPLATE, printForm.getFile());
		printForm(containerId, reportFileName, outputFileName, printForm.getPrinter());
	}

	public void printForm(String containerId, final String reportFileName, String outputFileName, Printer printer) throws Exception {
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
		for (Parameter parameter : containerIdParams) {
			parameter.setValue(containerId);
		}
		ValueResolver vr = new ValueResolver();
		vr.resolve(report, configuration);

		// Render report
		if (ArrayUtils.contains(REPORT_TYPE_FRF, report.getFileVersion())) {
			printUsingApacheFop(report, printer);
		} else if (ArrayUtils.contains(REPORT_TYPE_ZEBRA, report.getFileVersion())) {
			printOnZebraPrepared(report, printer);
		} else {
			throw new PackLineException(getResources().getString("error.report.invalid.type"));
		}
	}

	private void printUsingApacheFop(Report report, Printer printer) throws Exception {
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
				}

				// Go!
				rxto.execute();

				// If we rendered in memory buffer, then the contents
				// of this buffer should be sent to printer directly
				if (baos2 != null) {
					Utils.sendDataToSocket(printer.getIpAddress(), printer.getPort(), baos2.toByteArray());
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

	private void printOnZebraPrepared(Report report, Printer printer) throws Exception {
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
			Utils.sendDataToSocket(printer.getIpAddress(), printer.getPort(), baos.toByteArray());
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
			for (Parameter parameter : dataset.getParameters()) {
				if (Const.CONTAINER_ID_PARAM.equals(parameter.getName())) {
					containerIdParams.add(parameter);
				}
			}
		}
	}
}
