package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.util.StringUtils;
import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.Variable;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Parameter;
import ru.aplix.converters.fr2afop.fr.type.VariableType;
import ru.aplix.packline.Const;
import ru.aplix.packline.ContainerProblemException;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.GetLabelResponse2;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.StandardWorkflowController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class BasePrintFormsAction<Controller extends StandardWorkflowController<?>> extends BasePrintAction {

	private Parameter containerIdParam = null;
	private Parameter postIdParam = null;
	private List<Parameter> queryIdParams = null;

	private String queryId;
	private Boolean attachDocuments = false;

	private String containerId;
	private String postId;

	public BasePrintFormsAction() {
		queryIdParams = new ArrayList<Parameter>();
	}

	public void prepare() {
		// updateQueryId
		queryId = RandomStringUtils.randomAlphanumeric(15);
	}

	public Boolean printForms(String containerId, String postId, PrintForm printForm) throws PackLineException {
		attachDocuments = false;

		if (printForm.getPrinter() == null) {
			throw new PackLineException(String.format(getResources().getString("error.printer.not.assigned"), printForm.getName()));
		}

		try {
			if (printForm.getFile() == null) {
				downloadAndPrintForm(containerId, printForm.getName(), printForm.getPrinter());
			} else {
				String reportFileName = getJarFolder() + String.format(Const.REPORT_FILE_TEMPLATE, printForm.getFile());
				printFormFromFile(containerId, postId, reportFileName, printForm.getPrinter(), printForm.getName(), printForm.getCopies());
			}

			if (printForm.getPostPrintDelay() != null) {
				Thread.sleep(printForm.getPostPrintDelay());
			}
		} catch (PackLineException ple) {
			throw ple;
		} catch (Throwable e) {
			PackLineException ple = new PackLineException(String.format(getResources().getString("error.printing"), printForm.getPrinter().getName()), e);
			throw ple;
		}
		return attachDocuments;
	}

	private void printFormFromFile(String containerId, String postId, final String reportFileName, Printer printer, String formName, Integer copies)
			throws Exception {

		this.containerId = containerId;
		this.postId = postId;
		printFromFile(reportFileName, printer, formName, copies);
	}

	@Override
	protected void beforeResolving(Report report) {
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
	}

	@Override
	protected boolean afterResolving(Report report) throws PackLineException {
		// Check if the report determined to cancel printing
		return checkPrintingStatus(report);
	}

	@Override
	protected void prepareDatabase(ru.aplix.converters.fr2afop.fr.Configuration configuration) {
		super.prepareDatabase(configuration);

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

	private boolean checkPrintingStatus(Report report) throws PackLineException {
		attachDocuments = false;

		// Cancel printing
		Variable variable = (Variable) CollectionUtils.find(report.getVariables(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return Const.CANCEL_PRINTING_VARIABLE.equals(((Variable) item).getName());
			}
		});

		if (variable != null && variable.getValue() != null && Boolean.valueOf(variable.getValue())) {
			return false;
		}

		// Check container problem
		variable = (Variable) CollectionUtils.find(report.getVariables(), new Predicate() {
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
			} else if ("AttachDocuments".equalsIgnoreCase(variable.getValue())) {
					attachDocuments = true;
			}
			else throw new ContainerProblemException(String.format(getResources().getString("error.post.container.problem.other"), variable.getValue()),
				variable.getValue());
		}

		// Check container tracking Id
		Container container = (Container) getContext().getAttribute(Const.TAG);
		if (container.getTrackingId() == null || container.getTrackingId().length() == 0) {
			variable = (Variable) CollectionUtils.find(report.getVariables(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					Variable v = (Variable) item;
					return VariableType.DB_FIELD.equals(v.getType()) && Const.CONTAINER_TRACKING_ID_VARIABLE.equals(v.getContent());
				}
			});

			if (variable != null && variable.getValue() != null && variable.getValue().length() > 0) {
				container.setTrackingId(variable.getValue());
			}
		}

		return true;
	}

	private void downloadAndPrintForm(String containerId, String formName, Printer printer) throws Exception {
		// Download label from server
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		GetLabelResponse2 response = postServicePort.getLabel2(containerId);
		if (response == null) {
			throw new PackLineException(String.format(getResources().getString("error.post.label.empty"), formName, containerId));
		}
		if (!StringUtils.isEmpty(response.getError())) {
			throw new PackLineException(response.getError());
		}
		if (response.getFileContents() == null || response.getFileContents().length == 0) {
			throw new PackLineException(String.format(getResources().getString("error.post.label.empty"), formName, containerId));
		}

		// Save it in temp file
		File tempFile = File.createTempFile(containerId, ".pdf");
		LOG.debug(String.format("Saving label temporary to '%s'...", tempFile.getAbsolutePath()));
		tempFile.deleteOnExit();
		try (OutputStream os = new FileOutputStream(tempFile)) {
			os.write(response.getFileContents());
			os.flush();
		}

		// Invoke pdf printer
		String command = String.format("\"%s%s%s\" -p \"%s\" \"%s\"", getJarFolder(), File.separatorChar, Const.PDF_PRINTER_FILE, printer.getName(),
				tempFile.getAbsolutePath());
		LOG.debug("Executing command: " + command);
		Runtime.getRuntime().exec(command);
	}
}
