package ru.aplix.packline.action;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.RandomStringUtils;

import ru.aplix.converters.fr2afop.database.ValueResolver;
import ru.aplix.converters.fr2afop.fr.Page;
import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.Variable;
import ru.aplix.converters.fr2afop.fr.dataset.Column;
import ru.aplix.converters.fr2afop.fr.dataset.Connection;
import ru.aplix.converters.fr2afop.fr.dataset.Database;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Parameter;
import ru.aplix.converters.fr2afop.fr.dataset.Row;
import ru.aplix.converters.fr2afop.fr.type.BarCodeType;
import ru.aplix.converters.fr2afop.fr.view.BarCodeView;
import ru.aplix.converters.fr2afop.fr.view.View;
import ru.aplix.converters.fr2afop.reader.InputStreamOpener;
import ru.aplix.converters.fr2afop.reader.ReportReader;
import ru.aplix.converters.fr2afop.reader.XMLReportReader;
import ru.aplix.converters.fr2afop.writer.OutputStreamOpener;
import ru.aplix.converters.fr2afop.writer.ReportWriter;
import ru.aplix.converters.fr2afop.writer.XMLReportWriter;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.conf.Stickers;
import ru.aplix.packline.jdbc.PostDriver;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagList;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.StandardWorkflowController;

public abstract class BaseStickAction<Controller extends StandardWorkflowController<?>, T> extends CommonAction<Controller> {

	private static final int BUFFER_SIZE = 10240;

	private ru.aplix.converters.fr2afop.fr.Configuration configuration = null;

	private String jarFolder = null;
	private File r2afopConfigFile;
	private String fr2afopConfigFileName;

	private Parameter queryIdParam = null;
	private Parameter countParam = null;
	private String queryId;
	private Boolean lastGenerateSucceded = null;
	private TagList lastTagList;
	private int offset;
	private int count;

	protected abstract Stickers getStickers() throws Exception;

	protected abstract String getReportName();

	protected abstract String getDatesetName();

	public abstract T processBarcode(String code) throws PackLineException;

	public int findPrintedTag(final String code) {
		int result = -1;
		if (lastTagList != null && code != null) {
			int i = 0;
			List<Tag> list = lastTagList.getItems();
			while (result == -1 && i < list.size()) {
				Tag item = list.get(i);
				if (code.equals(item.getId())) {
					result = i;
				}
				i++;
			}
		}
		return result;
	}

	protected void setLastTagList(TagList value) {
		lastTagList = value;
	}

	protected TagList getLastTagList() {
		return lastTagList;
	}

	protected void beforeResolving(Report report) throws Exception {
		if (queryIdParam != null) {
			queryIdParam.setValue(queryId);
		}
		if (countParam != null) {
			countParam.setValue("" + count);
		}
	}

	protected void afterResolving(Report report) throws Exception {
		// Set variables values
		setVariableValue(report, Const.PRINT_MODE_VARIABLE, getStickers().getPrinter().getPrintMode());
		setBarCodeViewValue(report, getStickers().getBarCodeType());

		if (offset < 0) {
			// Store last tag list
			lastTagList = new TagList();
			for (Dataset dataset : report.getDatasets()) {
				for (Row row : dataset.getRows()) {
					for (Column column : row.getColumns()) {
						Tag tag = new Tag();
						tag.setId(column.getValue());
						lastTagList.getItems().add(tag);
					}
				}
			}
		} else {
			// Remove the beginning of list
			for (Dataset dataset : report.getDatasets()) {
				List<Row> newRows = dataset.getRows().subList(offset + 1, dataset.getRows().size());
				dataset.setRows(newRows);
			}
		}
	}

	protected void generateAndPrint(int offset, int count) throws PackLineException {
		this.offset = offset;
		this.count = offset < 0 ? count : lastTagList != null ? lastTagList.getItems().size() : count;

		try {
			// updateQueryId
			if ((lastGenerateSucceded == null || lastGenerateSucceded.booleanValue()) && offset < 0) {
				queryId = RandomStringUtils.randomAlphanumeric(15);
				lastGenerateSucceded = false;
			}

			// Resolve configuration
			Printer printer = getStickers().getPrinter();
			if (printer == null) {
				throw new PackLineException(getResources().getString("error.printer.not.assigned"));
			}

			if (jarFolder == null) {
				File confFolder = new File(Configuration.getConfigFileName()).getParentFile();
				jarFolder = confFolder != null ? confFolder.getParent() : "";
				fr2afopConfigFileName = jarFolder + Const.FR2AFOP_CONF_FILE;
			}

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
					return new FileInputStream(jarFolder + String.format(Const.REPORT_FILE_TEMPLATE, getReportName()));
				}
			}, null, configuration);

			// Select data from dataset
			beforeResolving(report);
			try {
				ValueResolver vr = new ValueResolver();
				vr.resolve(report, configuration);
			} finally {
				afterResolving(report);
			}

			// Load XSL transform
			printPrepared(report, printer);

			lastGenerateSucceded = true;
		} catch (Throwable e) {
			if (e instanceof PackLineException) {
				throw (PackLineException) e;
			} else {
				PackLineException ple = new PackLineException(getResources().getString("error.stickers"), e);
				throw ple;
			}
		}
	}

	protected void setVariableValue(Report report, final String name, Object value) {
		Variable variable = (Variable) CollectionUtils.find(report.getVariables(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return name.equals(((Variable) item).getName());
			}
		});
		if (variable != null) {
			variable.setValue("" + value);
		}
	}

	protected void setBarCodeViewValue(Report report, BarCodeType value) {
		for (Page page : report.getPages()) {
			for (View view : page.getViews()) {
				if (view instanceof BarCodeView) {
					((BarCodeView) view).setBarCodeType(value);
				}
			}
		}
	}

	protected void printPrepared(Report report, Printer printer) throws Exception {
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
			Utils.sendDataToSocket(printer.getIpAddress(), printer.getPort(), baos.toByteArray(), null);
		} finally {
			baos.close();
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

		// Get reference to container Id parameter
		for (Dataset dataset : configuration.getDatasets()) {
			if (getDatesetName().equals(dataset.getName())) {
				for (Parameter parameter : dataset.getParameters()) {
					if (Const.QUERY_ID_PARAM.equals(parameter.getName())) {
						queryIdParam = parameter;
					}
					if (Const.COUNT_PARAM.equals(parameter.getName())) {
						countParam = parameter;
					}
				}
			}
		}
	}
}
