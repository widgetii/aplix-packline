package ru.aplix.packline.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.PackTypeAction;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.model.PackingType;
import ru.aplix.packline.workflow.WorkflowContext;

public class PackTypeController extends StandardController<PackTypeAction> {

	@FXML
	private Label clientLabel;
	@FXML
	private Label deliveryLabel;
	@FXML
	private Label customerLabel;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		Order order = (Order) context.getAttribute(Const.ORDER);

		clientLabel.setText(order.getClient());
		deliveryLabel.setText(order.getDeliveryMethod());
		customerLabel.setText(order.getCustomer());
	}

	@Override
	public void terminate() {

	}

	public void boxClick(ActionEvent event) {
		selectPacketType(PackingType.BOX);
	}

	public void packetClick(ActionEvent event) {
		selectPacketType(PackingType.PACKET);
	}

	public void paperClick(ActionEvent event) {
		selectPacketType(PackingType.PAPER);
	}

	private void selectPacketType(PackingType value) {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		getAction().selectPacketType(value, order);
		done();
	}
}
