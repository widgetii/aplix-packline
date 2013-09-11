package ru.aplix.packline.controller;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.WeightingOrderAction;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.workflow.WorkflowContext;

public class WeightingOrderController extends StandardController<WeightingOrderAction> {

	@FXML
	private Label clientLabel;
	@FXML
	private Label deliveryLabel;
	@FXML
	private Label customerLabel;
	@FXML
	private Label weightLabel;
	
	private float measure = 0f;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		final Random random = new Random();

		Timeline timeline = new Timeline();
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(0.5), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				measure = 10f + random.nextFloat() * 1f;
				weightLabel.setText(String.format("%.3f", measure));
			}
		}));
		timeline.playFromStart();
	}

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

	public void nextClick(ActionEvent event) {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		getAction().processMeasure(measure, order);
		done();
	}
}
