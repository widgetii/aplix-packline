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
import ru.aplix.packline.workflow.WorkflowAction;

public class ReadBarcodeOrderAction extends CommonAction<ReadBarcodeOrderController> {

	private WorkflowAction acceptanceAction;
	private WorkflowAction packingAction;
	private WorkflowAction markingAction;

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

	@Override
	protected String getFormName() {
		return "barcode-order";
	}

	public Tag processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Order order;
		Tag result = postServicePort.findTag(code);
		if (result == null || !code.equals(result.getId())) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		if (result instanceof Incoming) {
			Incoming incoming = (Incoming) result;
			order = findAndValidateTag(postServicePort, incoming.getOrderId(), Order.class);
			checkIncomingRegistered(order, incoming.getId());
			setNextAction(getAcceptanceAction());
		} else if (result instanceof Post) {
			Post post = (Post) result;
			order = findAndValidateTag(postServicePort, post.getOrderId(), Order.class);
			checkPostPacked(post);
			setNextAction(getPackingAction());
		} else if (result instanceof Container) {
			Container container = (Container) result;
			Post post = findAndValidateTag(postServicePort, container.getPostId(), Post.class);
			order = findAndValidateTag(postServicePort, post.getOrderId(), Order.class);

			setNextAction(getMarkingAction());
		} else {
			order = null;
			result = null;
		}

		getContext().setAttribute(Const.ORDER, order);
		getContext().setAttribute(Const.TAG, result);
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> T findAndValidateTag(PackingLinePortType postServicePort, String tagId, Class<T> tagClass) throws PackLineException {
		Tag tag = postServicePort.findTag(tagId);
		if (tag == null) {
			throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
		} else if (!(tag.getClass().equals(tagClass))) {
			throw new PackLineException(String.format(getResources().getString("error.post.invalid.tag.class"), tagId, tagClass.getSimpleName(), tag.getClass()
					.getSimpleName()));
		} else {
			return (T) tag;
		}
	}

	private void checkIncomingRegistered(Order order, final String incomingId) throws PackLineException {
		Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incomingId.equals(((Tag) item).getId());
			}
		});
		if (existing != null) {
			throw new PackLineException(getResources().getString("error.post.incoming.registered"));
		}
	}

	private void checkPostPacked(Post post) throws PackLineException {
		if (post.getContainer() != null) {
			throw new PackLineException(getResources().getString("error.post.already.packed"));
		}
	}
}
