package ru.aplix.packline.action;

import javax.xml.datatype.DatatypeConfigurationException;

import ru.aplix.packline.Const;
import ru.aplix.packline.controller.PhotoController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowAction;

public class PackTypeAction extends CommonAction<PhotoController> {

	private WorkflowAction barcodeAction;
	private WorkflowAction dimentionAction;

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
}
