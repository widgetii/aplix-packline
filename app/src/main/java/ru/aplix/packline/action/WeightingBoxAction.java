package ru.aplix.packline.action;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.WeightingRestriction;
import ru.aplix.packline.controller.WeightingBoxController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.workflow.WorkflowAction;

public class WeightingBoxAction extends CommonAction<WeightingBoxController> {

	private WorkflowAction printingAction;
	private WorkflowAction overweightAction;

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

	@Override
	protected String getFormName() {
		return "weighting-box";
	}

	public void processMeasure(Float value) throws PackLineException, FileNotFoundException, MalformedURLException, JAXBException {
		Container container = (Container) getContext().getAttribute(Const.TAG);
		container.setTotalWeight(value);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.updateContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.update"));
		}

		final Post post = (Post) getContext().getAttribute(Const.POST);
		WeightingRestriction wr = (WeightingRestriction) CollectionUtils.find(Configuration.getInstance().getWeightingRestrictions(), new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				return post.getPostType().equals(((WeightingRestriction) object).getPostType());
			}
		});

		if (wr != null && container.getTotalWeight() > wr.getMaxWeight()) {
			setNextAction(getOverweightAction());
		} else {
			setNextAction(getPrintingAction());
		}
	}
}
