package ru.aplix.packline.action;

import ru.aplix.packline.controller.PhotoController;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.model.Packing;
import ru.aplix.packline.model.PackingType;
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

	public void selectPacketType(PackingType value, Order order) {
		Packing packing = new Packing();
		packing.setPackingType(value);
		order.setPacking(packing);

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
