package ru.aplix.packline.action;

import java.util.List;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReadBarcodeBoxController;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.StringList;

public class ReadBarcodeBoxAction extends CommonAction<ReadBarcodeBoxController> {

	@Override
	protected String getFormName() {
		return "custom-box";
	}

	public void process(List<String> codes) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		StringList containerIds = new StringList();
		containerIds.getItems().addAll(codes);
		String result = postServicePort.accountContainerCost(containerIds);
		if (result != null && result.length() > 0) {
			throw new PackLineException(result);
		}
	}
}
