package ru.aplix.packline.action;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.conf.WeightingRestriction;
import ru.aplix.packline.controller.WeightingBoxController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.UpdateContainerResponse2;
import ru.aplix.packline.workflow.WorkflowAction;

public class WeightingBoxAction extends CommonAction<WeightingBoxController> {

	private WorkflowAction printingAction;
	private WorkflowAction overweightAction;
	private WorkflowAction underweightAction;
	private WorkflowAction nextParcelAction;

	private boolean containerHasUnrecoverableProblem;

	public WorkflowAction getPrintingAction() {
		return printingAction;
	}

	public void setPrintingAction(WorkflowAction printingAction) {
		this.printingAction = printingAction;
	}

	public WorkflowAction getOverweightAction() {
		return overweightAction;
	}

	public void setOverweightAction(WorkflowAction overweightAction) {
		this.overweightAction = overweightAction;
	}

	public WorkflowAction getUnderweightAction() {
		return underweightAction;
	}

	public void setUnderweightAction(WorkflowAction underweightAction) {
		this.underweightAction = underweightAction;
	}

	public WorkflowAction getNextParcelAction() {
		return nextParcelAction;
	}

	public void setNextParcelAction(WorkflowAction nextParcelAction) {
		this.nextParcelAction = nextParcelAction;
	}

	@Override
	protected String getFormName() {
		return "weighting-box";
	}

	public void reset() {
		containerHasUnrecoverableProblem = false;
	}

	public void processMeasure(Float value) throws PackLineException, FileNotFoundException, MalformedURLException, JAXBException {
		if (containerHasUnrecoverableProblem) {
			setNextAction(getNextParcelAction());
			return;
		}

		value = Math.max(value, 0);
		final Post post = (Post) getContext().getAttribute(Const.POST);
		Order order = (Order) getContext().getAttribute(Const.ORDER);

		// Add print form weight to the measured value
		List<PrintForm> forms = Configuration.getInstance().getPrintForms();
		for (PrintForm form : forms) {
			boolean postTypeRestriction = (form.getPostTypes().size() == 0 || form.getPostTypes().contains(post.getPostType()));
			boolean paymentMethodRestriction = (form.getPaymentFlags().size() == 0 || form.getPaymentFlags().contains(post.getPaymentFlags()));

			if (postTypeRestriction && paymentMethodRestriction && form.getWeight() != null) {
				value += form.getWeight();
			}
		}

		// Update container weight
		Container container = (Container) getContext().getAttribute(Const.TAG);
		container.setTotalWeight(value);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		UpdateContainerResponse2 response = postServicePort.updateContainer2(container);
		if (response == null) {
			throw new PackLineException(getResources().getString("error.post.container.update"));
		} else if (response.getError() != null && response.getError().length() > 0) {
			containerHasUnrecoverableProblem = true;
			throw new PackLineException(response.getError());
		} else if (response.getContainer() == null) {
			throw new PackLineException(getResources().getString("error.post.container.update"));
		}

		// Update container in our context
		container = response.getContainer();
		getContext().setAttribute(Const.TAG, container);

		// Check weighting restriction
		WeightingRestriction wr = (WeightingRestriction) CollectionUtils.find(Configuration.getInstance().getWeighting().getWeightingRestrictions(),
				new Predicate() {
					@Override
					public boolean evaluate(Object object) {
						if (post.getPostType() != null) {
							return post.getPostType().equals(((WeightingRestriction) object).getPostType());
						}
						return (((WeightingRestriction) object).getPostType() == null);
					}
				});

		if (wr != null && container.getTotalWeight() > wr.getMaxWeight()) {
			setNextAction(getOverweightAction());
		} else if (order.getIncoming() != null && (order.getIncoming().size() == 1)
				&& (container.getTotalWeight() - order.getIncoming().get(0).getWeight() < -1e-3)) {
			setNextAction(getUnderweightAction());
		} else {
			setNextAction(getPrintingAction());
		}
	}
}
