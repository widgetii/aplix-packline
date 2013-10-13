package ru.aplix.packline.action;

import java.util.List;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.GenStickController;
import ru.aplix.packline.post.BoxType;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Tag;

public class GenStickAction extends CommonAction<GenStickController> {

	@Override
	protected String getFormName() {
		return "sticking";
	}

	public BoxType processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		BoxType result = postServicePort.getBoxType(code);
		return result;
	}

	public void generateAndPrint(int count, String boxTypeId) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		List<Tag> tags = postServicePort.generateTagsForContainers(count);
		if (tags == null) {
			throw new PackLineException(getResources().getString("error.post.generate.tags"));
		}

		if (boxTypeId != null) {
			boolean res = postServicePort.addBoxContainers(boxTypeId, tags);
			if (!res) {
				throw new PackLineException(getResources().getString("error.post.add.containers"));
			}
		}

		for (int i = 0; i < count; i++) {
			// TODO: print stickers
		}
	}
}
