package ru.aplix.packline.action;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.Stickers;
import ru.aplix.packline.controller.GenStickCustomerController;
import ru.aplix.packline.post.BoxType;
import ru.aplix.packline.post.PackingLinePortType;

public class GenStickAction extends BaseStickAction<GenStickCustomerController, BoxType> {

	private String boxTypeId = null;

	@Override
	protected String getFormName() {
		return "sticking";
	}

	@Override
	protected Stickers getStickers() throws FileNotFoundException, MalformedURLException, JAXBException {
		return Configuration.getInstance().getStickers().getForContainers();
	}

	@Override
	protected String getReportName() {
		return "mcont";
	}

	@Override
	protected String getDatesetName() {
		return Const.MARKERS_FOR_CONTAINERS_DATASET;
	}

	@Override
	public BoxType processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		BoxType result = postServicePort.getBoxType(code);
		if (result == null || result.getId() == null || result.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		if (getLastTagList() != null) {
			setLastTagList(null);
		}

		return result;
	}

	public void generateAndPrint(int offset, int count, String boxTypeId) throws PackLineException {
		this.boxTypeId = boxTypeId;

		generateAndPrint(offset, count);
	}

	@Override
	protected void afterResolving(Report report) throws Exception {
		super.afterResolving(report);

		if (boxTypeId != null) {
			PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
			boolean res = postServicePort.addBoxContainers(boxTypeId, getLastTagList());
			if (!res) {
				throw new PackLineException(getResources().getString("error.post.add.containers"));
			}
		}
	}
}
