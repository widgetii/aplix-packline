package ru.aplix.packline.action;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.controller.ReturnRegistryScanController;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PostType;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.utils.ImagesToPDFConverter;
import ru.aplix.packline.workflow.WorkflowAction;

public class ReturnRegistryScanAction extends CommonAction<ReturnRegistryScanController> {

	private WorkflowAction viewAction;
	private WorkflowAction backAction;

	public WorkflowAction getViewAction() {
		return viewAction;
	}

	public void setViewAction(WorkflowAction viewAction) {
		this.viewAction = viewAction;
	}

	public WorkflowAction getBackAction() {
		return backAction;
	}

	public void setBackAction(WorkflowAction backAction) {
		this.backAction = backAction;
	}

	@Override
	protected String getFormName() {
		return "return-registry-scan";
	}

	public void processBarcode(String code, List<File> images) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Registry registry = postServicePort.findRegistry2(code, getCarrier());
		if (registry == null || registry.getId() == null || registry.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.post.registry.notfound"));
		}
		if (registry.isCarriedOutAndClosed()) {
			throw new PackLineException(getResources().getString("error.post.order.already.closed"));
		}

		uploadRegistry(code, images);

		getContext().setAttribute(Const.REGISTRY, registry);

		if (registry.getIncoming() != null && registry.getIncoming().size() > 0) {
			setNextAction(getViewAction());
		} else {
			setNextAction(getBackAction());
		}
	}

	private void uploadRegistry(String code, List<File> images) throws PackLineException {
		if (images.size() == 0) {
			return;
		}

		try {
			File pdfFile = File.createTempFile(code, ".pdf");
			try {
				ImagesToPDFConverter itpc = new ImagesToPDFConverter();
				itpc.convert(images, pdfFile.getAbsolutePath());

				File destFile = new File(Configuration.getInstance().getPostService().getRemoteStoragePath(), pdfFile.getName());
				if (!destFile.equals(pdfFile)) {
					FileUtils.copyFile(pdfFile, destFile);

					PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
					String result = postServicePort.fileUpload(code, destFile.getAbsolutePath());
					if (result != null && result.length() > 0) {
						throw new PackLineException(result);
					}
				}
			} finally {
				pdfFile.delete();
			}
		} catch (Exception e) {
			throw new PackLineException(getResources().getString("error.file.upload"), e);
		}
	}

	private PostType getCarrier() throws PackLineException {
		String selectedCarrier = (String) getContext().getAttribute(Const.SELECTED_CARRIER);
		if ("Aplix".equals(selectedCarrier)) {
			return PostType.PACKAGE;
		} else if ("CDEK".equals(selectedCarrier)) {
			return PostType.CDEK;
		} else if ("DHL".equals(selectedCarrier)) {
			return PostType.DHL;
		} else if ("DPD".equals(selectedCarrier)) {
			return PostType.DPD;
		} else if ("EMS".equals(selectedCarrier)) {
			return PostType.EMS;
		} else if ("IML".equals(selectedCarrier)) {
			return PostType.IML;
		} else if ("PickPoint".equals(selectedCarrier)) {
			return PostType.PICKPOINT;
		} else if ("QIWI".equals(selectedCarrier)) {
			return PostType.QIWIPOST;
		} else if ("RussianPost".equals(selectedCarrier)) {
			return PostType.PARCEL;
		} else if ("SPSR".equals(selectedCarrier)) {
			return PostType.SPSR;
		} else {
			throw new PackLineException(String.format(getResources().getString("error.unknown.carrier"), selectedCarrier));
		}
	}
}
