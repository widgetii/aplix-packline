package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReadBarcodeOrderController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.workflow.WorkflowAction;

public class ReadBarcodeOrderAction extends CommonAction<ReadBarcodeOrderController> {

	private WorkflowAction acceptanceAction;
	private WorkflowAction packingAction;
	private WorkflowAction markingAction;
	private WorkflowAction orderActAction;

	public WorkflowAction getAcceptanceAction() {
		return acceptanceAction;
	}

	public void setAcceptanceAction(WorkflowAction acceptanceAction) {
		this.acceptanceAction = acceptanceAction;
	}

	public WorkflowAction getPackingAction() {
		return packingAction;
	}

	public void setPackingAction(WorkflowAction packingAction) {
		this.packingAction = packingAction;
	}

	public WorkflowAction getMarkingAction() {
		return markingAction;
	}

	public void setMarkingAction(WorkflowAction markingAction) {
		this.markingAction = markingAction;
	}

	public WorkflowAction getOrderActAction() {
		return orderActAction;
	}

	public void setOrderActAction(WorkflowAction orderActAction) {
		this.orderActAction = orderActAction;
	}

	@Override
	protected String getFormName() {
		return "barcode-order";
	}

	public Tag processBarcode(String code) throws PackLineException {
		Order order;
		Tag result;
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (TagType.INCOMING.equals(tagType)) {
			Incoming incoming = findAndValidateTag(postServicePort, TagType.INCOMING, code, Incoming.class, false);
			order = findAndValidateTag(postServicePort, TagType.ORDER, incoming.getOrderId(), Order.class, true);

			checkOrder(order);

			if (isIncomingRegistered(order, incoming.getId())) {
				setNextAction(getOrderActAction());
			} else {
				setNextAction(getAcceptanceAction());
			}
			result = incoming;
		} else if (TagType.POST.equals(tagType)) {
			Post post = findAndValidateTag(postServicePort, TagType.POST, code, Post.class, false);
			checkPost(post);
			order = findAndValidateTag(postServicePort, TagType.ORDER, post.getOrderId(), Order.class, true);

			setNextAction(getPackingAction());
			result = post;
		} else if (TagType.CONTAINER.equals(tagType)) {
			Container container = findAndValidateTag(postServicePort, TagType.CONTAINER, code, Container.class, false);
			checkContainer(container);
			Post post = findAndValidateTag(postServicePort, TagType.POST, container.getPostId(), Post.class, true);
			order = findAndValidateTag(postServicePort, TagType.ORDER, post.getOrderId(), Order.class, true);

			setNextAction(getMarkingAction());
			result = container;
		} else {
			order = null;
			result = null;
		}

		getContext().setAttribute(Const.ORDER, order);
		getContext().setAttribute(Const.TAG, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> T findAndValidateTag(PackingLinePortType postServicePort, TagType tagType, String tagId, Class<T> tagClass, boolean nested)
			throws PackLineException {
		Tag tag;
		switch (tagType) {
		case INCOMING:
			tag = postServicePort.findIncoming(tagId);
			break;
		case ORDER:
			tag = postServicePort.findOrder(tagId);
			break;
		case POST:
			tag = postServicePort.findPost(tagId);
			break;
		case CONTAINER:
			tag = postServicePort.findContainer(tagId);
			break;
		default:
			throw new IllegalArgumentException();
		}
		if (tag == null || !tagId.equals(tag.getId())) {
			if (nested) {
				throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
			} else {
				throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
			}
		}
		if (!(tag.getClass().equals(tagClass))) {
			throw new PackLineException(String.format(getResources().getString("error.post.invalid.tag.class"), tagId, tagClass.getSimpleName(), tag.getClass()
					.getSimpleName()));
		} else {
			return (T) tag;
		}
	}

	private boolean isIncomingRegistered(Order order, final String incomingId) throws PackLineException {
		Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incomingId.equals(((Tag) item).getId());
			}
		});
		return (existing != null);
	}

	private void checkOrder(Order order) throws PackLineException {
		if (order.isCarriedOutAndClosed()) {
			throw new PackLineException(getResources().getString("error.post.order.already.closed"));
		}
	}

	private void checkPost(Post post) throws PackLineException {
		if (post.getContainer() != null) {
			throw new PackLineException(getResources().getString("error.post.already.packed"));
		}
	}

	private void checkContainer(Container container) throws PackLineException {
		if (container.getPostId() == null) {
			throw new PackLineException(getResources().getString("error.post.container.empty"));
		}
	}
}
