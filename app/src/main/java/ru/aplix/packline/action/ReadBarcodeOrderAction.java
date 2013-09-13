package ru.aplix.packline.action;

import ru.aplix.packline.controller.ReadBarcodeOrderController;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.model.Packing;
import ru.aplix.packline.model.PackingSize;
import ru.aplix.packline.model.PackingType;
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

	public Order processBarcode(String code) {
		// TODO: place processing code here
		if ("008002".equals(code)) {
			Order order = new Order();
			order.setId(1L);
			order.setCode(code);
			order.setClient("Аллагулов Азат Халитович");
			order.setDeliveryMethod("Почтовая посылка Почта России (140.7 руб., 7 дня)");
			order.setDeliveryAddress("456010, Челябинская обл., Ашинский р-н, Аша г., Ленина ул., дом № 45А, кв. 29");
			order.setCustomer("ИП Митрюшкин Сергей Сергеевич");

			setNextAction(getAcceptanceAction());
			return order;
		} else if ("010932".equals(code)) {
			Order order = new Order();
			order.setId(2L);
			order.setCode(code);
			order.setClient("Клименко Ольга Васильевна");
			order.setDeliveryMethod("Почтовая посылка Почта России (154.4 руб., 8 дня)");
			order.setDeliveryAddress("627354, Тюменская обл., Аромашевский р-н, Слободчики с., Молодежная ул.\t(Слободчики с.), дом № 18, кв. 1");
			order.setCustomer("ОАО \"Звезда\"");

			setNextAction(getPackingAction());
			return order;
		} else if ("70001792".equals(code)) {
			Order order = new Order();
			order.setId(3L);
			order.setCode(code);
			order.setClient("Репников Борис Владимирович");
			order.setDeliveryMethod("Почтовая посылка Почта России (195.1 руб., 3 дня)");
			order.setDeliveryAddress("400005, Волгоградская обл., Волгоград г., Коммунистическая ул., дом № 19");
			order.setCustomer("ООО \"Аскари\", г. Москва");

			Packing packing = new Packing();
			packing.setPackingId(1L);
			packing.setPackingCode("1234567890");
			packing.setPackingType(PackingType.BOX);
			packing.setPackingSize(new PackingSize(100f, 90f, 20f));
			order.setPacking(packing);

			setNextAction(getMarkingAction());
			return order;
		} else
			return null;
	}
}
