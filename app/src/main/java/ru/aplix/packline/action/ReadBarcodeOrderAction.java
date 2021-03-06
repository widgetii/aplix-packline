package ru.aplix.packline.action;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.controller.ReadBarcodeOrderController;
import ru.aplix.packline.post.ActionType;
import ru.aplix.packline.post.CheckAddressResult;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PickupRequest;
import ru.aplix.packline.post.PickupRequestList;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.post.RouteList;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.utils.CacheOrders;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowAction;

public class ReadBarcodeOrderAction extends NotificationAction<ReadBarcodeOrderController> {

	private WorkflowAction acceptanceAction;
	private WorkflowAction packingAction;
	private WorkflowAction markingAction;
	private WorkflowAction orderActAction;
	private WorkflowAction resetWorkAction;
	private WorkflowAction pickupRequestAction;
	private WorkflowAction newMarkerAction;

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

	public WorkflowAction getPickupRequestAction() {
		return pickupRequestAction;
	}

	public void setPickupRequestAction(WorkflowAction pickupRequestAction) {
		this.pickupRequestAction = pickupRequestAction;
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

	public void carryOutRouteList() throws PackLineException {
		RouteList routeList = (RouteList) getContext().getAttribute(Const.ROUTE_LIST);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.carryOutRouteList(routeList.getId())) {
			throw new PackLineException(getResources().getString("error.post.routeList.carryout"));
		}

		getContext().setAttribute(Const.ROUTE_LIST, null);
		setNextAction(this);
	}

	public void saveRouteList() {
		getContext().setAttribute(Const.ROUTE_LIST, null);
		setNextAction(this);
	}

	public Tag processBarcode(String code)
			throws PackLineException, FileNotFoundException, MalformedURLException, JAXBException, DatatypeConfigurationException {
		Post post;
		Registry registry;
		Order order;
		PickupRequest pickupRequest = null;
		Tag result;
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		CacheOrders cacheOrders = (CacheOrders) applicationContext.getBean(Const.CACHE_ORDERS_BEAN_NAME);

		TagType tagType = postServicePort.findTag(code);
		if (TagType.INCOMING.equals(tagType)) {
			if (!Configuration.getInstance().getRoles().getAcceptance()) {
				throw new PackLineException(getResources().getString("error.roles.acceptance"));
			}

			Incoming incoming = findAndValidateTag(postServicePort, TagType.INCOMING, code, Incoming.class, false);
			order = cacheOrders.findOrder(postServicePort, incoming.getOrderId(), false, true);
			registry = findRegistry(postServicePort, incoming.getId());
			checkRegistry(registry, order);

			for (final Incoming inc : registry.getIncoming()) {
				cacheOrders.findOrder(postServicePort, inc.getOrderId(), false, true);
			}

			RouteList routeList = (RouteList) getContext().getAttribute(Const.ROUTE_LIST);
			if (routeList == null) {
				PickupRequestList list = postServicePort.getPickupRequests(registry.getCustomer().getId(), Utils.now());
				pickupRequest = list != null && !list.getItems().isEmpty() ? list.getItems().get(0) : null;
			}

			if (isIncomingRegistered(registry, incoming.getId())) {
				setNextAction(getOrderActAction());
			} else {
				if (registry.getPickupRequest() == null && routeList != null) {
					setNextAction(getPickupRequestAction());
				} else {
					setNextAction(getAcceptanceAction());
				}
			}
			result = incoming;
			post = null;
		} else if (TagType.POST.equals(tagType)) {
			post = findAndValidateTag(postServicePort, TagType.POST, code, Post.class, false);

			order = cacheOrders.findOrder(postServicePort, post.getOrderId(), true, true);

			if (post.getContainer() != null) {
				setNextAction(getNewMarkerAction());
			}
			else {
				if (Configuration.getInstance().getRoles().getPacking()) {
					setNextAction(getPackingAction());
				}
				else {
					setNextAction(getNewMarkerAction());
				}
			}
			result = post;
			registry = null;
		} else if (TagType.CONTAINER.equals(tagType)) {
			if (!Configuration.getInstance().getRoles().getLabeling()) {
				throw new PackLineException(getResources().getString("error.roles.labeling"));
			}

			Container container = findAndValidateTag(postServicePort, TagType.CONTAINER, code, Container.class, false);
			checkContainer(container);
			post = findAndValidateTag(postServicePort, TagType.POST, container.getPostId(), Post.class, true);
			order = cacheOrders.findOrder(postServicePort, post.getOrderId(), true, true);

			setNextAction(getMarkingAction());
			notifyAboutOutgoingParcel(container.getId());
			result = container;
			registry = null;
		} else if (TagType.ROUTELIST.equals(tagType)) {
			if (!Configuration.getInstance().getRoles().getAcceptance()) {
				throw new PackLineException(getResources().getString("error.roles.acceptance"));
			}

			RouteList routeList = findAndValidateTag(postServicePort, TagType.ROUTELIST, code, RouteList.class, false);
			checkRouteList(routeList);

			setNextAction(this);
			result = routeList;
			order = null;
			registry = null;
			post = null;
		} else {
			order = null;
			result = null;
			registry = null;
			post = null;
		}

		getContext().setAttribute(Const.REGISTRY, registry);
		getContext().setAttribute(Const.ORDER, order);
		getContext().setAttribute(Const.POST, post);
		getContext().setAttribute(Const.PICKUPREQUEST, pickupRequest);
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
			throw new PackLineException(
					String.format(getResources().getString("error.post.invalid.tag.class"), tagId, tagClass.getSimpleName(), tag.getClass().getSimpleName()));
		} else {
			return (T) tag;
		}
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
		if (ActionType.ADD.equals(registry.getActionType())) {
			Incoming existing = (Incoming) CollectionUtils.find(registry.getIncoming(), item -> incomingId.equals(((Tag) item).getId()));
			return (existing != null);
		} else {
			Incoming existing = (Incoming) CollectionUtils.find(registry.getIncoming(), o -> {
                Incoming item = (Incoming) o;
                boolean result = incomingId.equals(item.getId());
                if (!result && item.getBarcodes() != null) {
                    result = ArrayUtils.contains(item.getBarcodes().toArray(), incomingId);
                }
                return result;
            });
			if (existing == null) {
				throw new PackLineException(getResources().getString("error.post.incoming.other.registy"));
			}

			return false;
		}
	}

	private void checkRegistry(Registry registry, Order order) throws PackLineException {
		if (registry.isCarriedOutAndClosed()) {
			throw new PackLineException(getResources().getString("error.post.order.already.closed"));
		}
		if (registry.getCustomer() == null
				|| (order != null && (order.getCustomer() == null || !registry.getCustomer().getId().equals(order.getCustomer().getId())))) {
			throw new PackLineException(getResources().getString("error.post.incoming.incorrect.customer"));
		}
	}

	private void checkContainer(Container container) throws PackLineException {
		if (container.getPostId() == null || container.getPostId().length() == 0) {
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

	private void checkRouteList(RouteList routeList) throws PackLineException {
		// @formatter:off
		/*
		 * if (routeList.isCarriedOutAndClosed()) { throw new PackLineException(getResources().getString("error.post.routeList.already.closed")); }
		 */
		// @formatter:on
	}

	public void setNewMarkerAction(WorkflowAction newMarkerAction) {
		this.newMarkerAction = newMarkerAction;
	}

	public WorkflowAction getNewMarkerAction() {
		return newMarkerAction;
	}
}
