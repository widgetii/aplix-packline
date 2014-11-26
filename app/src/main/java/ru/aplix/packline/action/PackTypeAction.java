package ru.aplix.packline.action;

import javax.xml.datatype.DatatypeConfigurationException;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.PackTypeController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowAction;

public class PackTypeAction extends NotificationAction<PackTypeController> {

	private WorkflowAction barcodeAction;
	private WorkflowAction dimentionAction;
	private WorkflowAction closeAction;

	public WorkflowAction getBarcodeAction() {
		return barcodeAction;
	}

	public void setBarcodeAction(WorkflowAction barcodeAction) {
		this.barcodeAction = barcodeAction;
	}

	public WorkflowAction getDimentionAction() {
		return dimentionAction;
	}

	public void setDimentionAction(WorkflowAction dimentionAction) {
		this.dimentionAction = dimentionAction;
	}

	public WorkflowAction getCloseAction() {
		return closeAction;
	}

	public void setCloseAction(WorkflowAction closeAction) {
		this.closeAction = closeAction;
	}

	@Override
	protected String getFormName() {
		return "pack-type";
	}

	public void selectPacketType(PackingType value) throws DatatypeConfigurationException {
		Container container = new Container();

		Post post = (Post) getContext().getAttribute(Const.TAG);
		post.setContainer(container);

		container.setPackingType(value);
		container.setPostId(post.getId());
		container.setDate(Utils.now());

		switch (value) {
		case BOX:
			setNextAction(getBarcodeAction());
			break;
		case PACKET:
		case PAPER:
			setNextAction(getDimentionAction());
			break;
		}
	}

	public int processBarcode(String code) throws PackLineException, DatatypeConfigurationException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (!TagType.CONTAINER.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.post.not.box.container"));
		}
		Container emptyBox = postServicePort.findContainer(code);
		if (emptyBox == null || emptyBox.getId() == null || emptyBox.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		if (!PackingType.BOX.equals(emptyBox.getPackingType())) {
			throw new PackLineException(getResources().getString("error.post.not.box.container"));
		}
		if (Utils.isPackingSizeEmpty(emptyBox.getPackingSize())) {
			throw new PackLineException(String.format(getResources().getString("error.post.box.invalid.size"), code));
		}
		if (emptyBox.getPostId() != null && emptyBox.getPostId().length() > 0) {
			throw new PackLineException(getResources().getString("error.post.container.incorrect.post"));
		}
		if (emptyBox.isShipped()) {
			throw new PackLineException(getResources().getString("error.post.container.shipped"));
		}

		Container container = new Container();

		Post post = (Post) getContext().getAttribute(Const.TAG);
		post.setContainer(container);

		container.setId(code);
		container.setPostId(post.getId());
		container.setPackingType(emptyBox.getPackingType());
		container.setPackingSize(emptyBox.getPackingSize());
		container.setBoxTypeId(emptyBox.getBoxTypeId());
		container.setDate(Utils.now());

		if (!postServicePort.addContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.add"));
		}

		notifyAboutIncomingParcel(container.getId());

		setNextAction(getCloseAction());
		return postServicePort.getBoxCount(container.getBoxTypeId());
	}
}
