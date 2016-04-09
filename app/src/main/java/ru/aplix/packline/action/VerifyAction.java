package ru.aplix.packline.action;

import java.util.Objects;
import java.util.Optional;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.VerifyController;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.Product;
import ru.aplix.packline.workflow.WorkflowAction;

public class VerifyAction extends CommonAction<VerifyController> {

	private WorkflowAction idleAction;
	private WorkflowAction verifyCloseAction;
	private WorkflowAction cancelAction;

	public WorkflowAction getIdleAction() {
		return idleAction;
	}

	public void setIdleAction(WorkflowAction idleAction) {
		this.idleAction = idleAction;
	}

	public WorkflowAction getVerifyCloseAction() {
		return verifyCloseAction;
	}

	public void setVerifyCloseAction(WorkflowAction verifyCloseAction) {
		this.verifyCloseAction = verifyCloseAction;
	}

	public WorkflowAction getCancelAction() {
		return cancelAction;
	}

	public void setCancelAction(WorkflowAction cancelAction) {
		this.cancelAction = cancelAction;
	}

	@Override
	protected String getFormName() {
		return "verify";
	}

	/**
	 *
	 * @param code
	 *            - Product bar code
	 * @return Product
	 * @throws PackLineException
	 */
	public Product processBarcode(String code) throws PackLineException {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (order == null || (order.getId() == null || order.getId().length() == 0)) {
			throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
		}

		Optional<Product> optProduct = order.getProducts().stream().filter(p -> {
			Optional<String> opt = p.getBarcodes().stream().filter(b -> Objects.equals(b, code)).findFirst();
			return opt.isPresent();
		}).findFirst();

		return optProduct.isPresent() ? optProduct.get() : null;
	}
}
