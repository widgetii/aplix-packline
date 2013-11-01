package ru.aplix.packline.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.PackTypeAction;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.workflow.WorkflowContext;

public class PackTypeController extends StandardController<PackTypeAction> {

	private final Log LOG = LogFactory.getLog(getClass());

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
		if (order != null) {
			clientLabel.setText(order.getClientName());
			deliveryLabel.setText(order.getDeliveryMethod());
			customerLabel.setText(order.getCustomerName());
		}
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
		try {
			getAction().selectPacketType(value);
		} catch (DatatypeConfigurationException e) {
			LOG.error(null, e);
		}
		done();
	}
}
