package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReadBarcodeOrderController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.post.RouteList;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.workflow.WorkflowAction;

public class ReadBarcodeOrderAction extends CommonAction<ReadBarcodeOrderController> {

	private WorkflowAction acceptanceAction;
	private WorkflowAction packingAction;
	private WorkflowAction markingAction;
	private WorkflowAction orderActAction;
	private WorkflowAction resetWorkAction;

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

	public WorkflowAction getResetWorkAction() {
		return resetWorkAction;
	}

	public void setResetWorkAction(WorkflowAction resetWorkAction) {
		this.resetWorkAction = resetWorkAction;
	}

	@Override
	protected String getFormName() {
		return "barcode-order";
	}

	public Operator checkOperatorWorkComplete(String code) {
		Operator operator = (Operator) getContext().getAttribute(Const.OPERATOR);
		if (operator != null && code != null && code.equals(operator.getId())) {
			PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
			postServicePort.setOperatorActivity(false);

			setNextAction(getResetWorkAction());
			return operator;
		}
		return null;
	}

	public Tag processBarcode(String code) throws PackLineException {
		Registry registry;
		Order order;
		Tag result;
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (TagType.INCOMING.equals(tagType)) {
			Incoming incoming = findAndValidateTag(postServicePort, TagType.INCOMING, code, Incoming.class, false);
			order = findOrder(postServicePort, incoming.getOrderId());
			registry = findRegistry(postServicePort, incoming.getId());
			checkRegistry(registry, order);

			if (isIncomingRegistered(registry, incoming.getId())) {
				setNextAction(getOrderActAction());
			} else {
				setNextAction(getAcceptanceAction());
			}
			result = incoming;
		} else if (TagType.POST.equals(tagType)) {
			Post post = findAndValidateTag(postServicePort, TagType.POST, code, Post.class, false);
			checkPost(post);
			order = findOrder(postServicePort, post.getOrderId());

			setNextAction(getPackingAction());
			result = post;
			registry = null;
		} else if (TagType.CONTAINER.equals(tagType)) {
			Container container = findAndValidateTag(postServicePort, TagType.CONTAINER, code, Container.class, false);
			checkContainer(container);
			Post post = findAndValidateTag(postServicePort, TagType.POST, container.getPostId(), Post.class, true);
			order = findOrder(postServicePort, post.getOrderId());

			setNextAction(getMarkingAction());
			result = container;
			registry = null;
		} else if (TagType.ROUTELIST.equals(tagType)) {
			RouteList routeList = findAndValidateTag(postServicePort, TagType.ROUTELIST, code, RouteList.class, false);

			setNextAction(this);
			result = routeList;
			order = null;
			registry = null;
		} else {
			order = null;
			result = null;
			registry = null;
		}

		getContext().setAttribute(Const.REGISTRY, registry);
		getContext().setAttribute(Const.ORDER, order);
		if (result instanceof RouteList) {
			getContext().setAttribute(Const.ROUTE_LIST, result);
			getContext().setAttribute(Const.TAG, null);
		} else {
			getContext().setAttribute(Const.TAG, result);
		}
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
		case ROUTELIST:
			tag = postServicePort.findRouteList(tagId);
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
		if (tag == null || tag.getId() == null || tag.getId().length() == 0) {
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

	private Order findOrder(PackingLinePortType postServicePort, String orderId) throws PackLineException {
		Order order = postServicePort.getOrder(orderId);
		if (order == null || order.getId() == null || order.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
		}
		return order;
	}

	private Registry findRegistry(PackingLinePortType postServicePort, String incomingId) throws PackLineException {
		RouteList routeList = (RouteList) getContext().getAttribute(Const.ROUTE_LIST);
		Registry registry = postServicePort.findRegistry(routeList != null ? routeList.getId() : "", incomingId);
		if (registry == null || registry.getId() == null || registry.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.post.registry.notfound"));
		}
		return registry;
	}

	private boolean isIncomingRegistered(Registry registry, final String incomingId) throws PackLineException {
		Incoming existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incomingId.equals(((Tag) item).getId());
			}
		});
		return (existing != null);
	}

	private void checkRegistry(Registry registry, Order order) throws PackLineException {
		if (registry.isCarriedOutAndClosed()) {
			throw new PackLineException(getResources().getString("error.post.order.already.closed"));
		}
		if (registry.getCustomer() == null || order.getCustomer() == null || !registry.getCustomer().getId().equals(order.getCustomer().getId())) {
			throw new PackLineException(getResources().getString("error.post.incoming.incorrect.customer"));
		}
	}

	private void checkPost(Post post) throws PackLineException {
		if (post.getContainer() != null) {
			throw new PackLineException(getResources().getString("error.post.already.packed"));
		}
	}

	private void checkContainer(Container container) throws PackLineException {
		if (container.getPostId() == null || container.getPostId().length() == 0) {
			throw new PackLineException(getResources().getString("error.post.container.empty"));
		}
		if (container.isShipped()) {
			throw new PackLineException(getResources().getString("error.post.container.shipped"));
		}
	}
}
