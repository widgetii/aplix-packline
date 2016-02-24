package ru.aplix.packline.action;

import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.input.InputEvent;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.TrolleyController;
import ru.aplix.packline.post.CheckAddressResult;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowAction;

public class TrolleyAction extends CommonAction<TrolleyController> {

	private WorkflowAction photoAction;
	private WorkflowAction acceptanceAction;
	private WorkflowAction dimentionAction;
	private WorkflowAction expressMarkingAction;

	public WorkflowAction getPhotoAction() {
		return photoAction;
	}

	public void setPhotoAction(WorkflowAction photoAction) {
		this.photoAction = photoAction;
	}

	public WorkflowAction getAcceptanceAction() {
		return acceptanceAction;
	}

	public void setAcceptanceAction(WorkflowAction acceptanceAction) {
		this.acceptanceAction = acceptanceAction;
	}

	public WorkflowAction getDimentionAction() {
		return dimentionAction;
	}

	public void setDimentionAction(WorkflowAction dimentionAction) {
		this.dimentionAction = dimentionAction;
	}

	public WorkflowAction getExpressMarkingAction() {
		return expressMarkingAction;
	}

	public void setExpressMarkingAction(WorkflowAction expressMarkingAction) {
		this.expressMarkingAction = expressMarkingAction;
	}

	@Override
	protected String getFormName() {
		return "trolley";
	}

	@Override
	protected void onFormLoaded(Parent rootNode, ResourceBundle resources) {
		super.onFormLoaded(rootNode, resources);

		// Add user activity event filter
		rootNode.addEventFilter(InputEvent.ANY, new EventHandler<InputEvent>() {
			@Override
			public void handle(InputEvent inputEvent) {
				getController().stopAutoFirer();
			}
		});
	}

	public TrolleyType getTrolleyMessage() {
		int current = 0;
		int total = Integer.MAX_VALUE;

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (order != null) {
			current = order.getIncoming().size() + 1;
			total = Math.max(1, order.getTotalIncomings());
		}

		if (order != null && order.isSkipPacking()) {
			return TrolleyType.SKIP;
		} else if (total == 1) {
			return TrolleyType.PACK;
		} else if (current < total) {
			return TrolleyType.KEEP;
		} else {
			return TrolleyType.JOIN;
		}
	}

	public void process() throws PackLineException, DatatypeConfigurationException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		final Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		incoming.setDate(Utils.now());

		switch (registry.getActionType()) {
		case ADD:
			if (registry.getIncoming().indexOf(incoming) == -1) {
				int res = postServicePort.addIncomingToRegistry(registry.getId(), incoming);
				if (res <= -1) {
					throw new PackLineException(getResources().getString("error.post.incoming.registry.add"));
				}
				registry.getIncoming().add(incoming);
			}
			if (order != null && order.getIncoming().indexOf(incoming) == -1) {
				order.getIncoming().add(incoming);
			}
			break;
		case DELETE:
			// Delete incoming from registry
			Incoming item = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object o) {
					Incoming item = (Incoming) o;
					boolean result = incoming.getId().equals(item.getId());
					if (!result && item.getBarcodes() != null) {
						result = ArrayUtils.contains(item.getBarcodes().toArray(), incoming.getId());
					}
					return result;
				}
			});
			if (item != null) {
				if (!postServicePort.deleteIncomingFromRegistry(registry.getId(), incoming)) {
					throw new PackLineException(getResources().getString("error.post.incoming.registry.delete"));
				}

				registry.getIncoming().remove(item);
			}
			if (order != null) {
				item = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
					@Override
					public boolean evaluate(Object o) {
						Incoming item = (Incoming) o;
						boolean result = incoming.getId().equals(item.getId());
						if (!result && item.getBarcodes() != null) {
							result = ArrayUtils.contains(item.getBarcodes().toArray(), incoming.getId());
						}
						return result;
					}
				});
				if (item != null) {
					order.getIncoming().remove(item);
				}
			}
			break;
		}

		if (order != null && order.isSkipPacking()) {
			Post post = postServicePort.createPostFromIncoming(incoming);
			if (post != null) {
				checkContainer(post.getContainer());

				getContext().setAttribute(Const.POST, post);
				getContext().setAttribute(Const.TAG, post.getContainer());
				getContext().setAttribute(Const.PREDEFINED_CONTAINER_WEIGHT, Boolean.TRUE);

				if (!PackingType.BOX.equals(post.getContainer().getPackingType())) {
					setNextAction(getDimentionAction());
				} else {
					setNextAction(getExpressMarkingAction());
				}
				return;
			}
		}

		setNextAction(getAcceptanceAction());
	}

	private void checkContainer(Container container) throws PackLineException {
		if (container == null || container.getPostId() == null || container.getPostId().length() == 0) {
			throw new PackLineException(getResources().getString("error.post.container.empty"));
		}
		if (container.isShipped()) {
			throw new PackLineException(getResources().getString("error.post.container.shipped"));
		}

		// Check address
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		CheckAddressResult checkResult = postServicePort.checkAddress(container.getId());
		if (!checkResult.isResult()) {
			if (checkResult.getMsg() != null && checkResult.getMsg().length() > 0) {
				throw new PackLineException(checkResult.getMsg());
			} else {
				throw new PackLineException(getResources().getString("error.post.container.check.address"));
			}
		}
		if (!StringUtils.isEmpty(checkResult.getMsg())) {
			getContext().setAttribute(Const.WARNING_MESSAGE, checkResult.getMsg());
			getContext().setAttribute(Const.WARNING_CODE, "ReceiverAddress");
		} else {
			getContext().setAttribute(Const.WARNING_MESSAGE, null);
			getContext().setAttribute(Const.WARNING_CODE, null);
		}
	}

	/**
	 *
	 */
	public static enum TrolleyType {
		PACK, KEEP, JOIN, SKIP
	}
}
