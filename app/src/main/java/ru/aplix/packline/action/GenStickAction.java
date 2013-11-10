package ru.aplix.packline.action;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

import javax.xml.bind.JAXBException;

import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.dataset.Column;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Parameter;
import ru.aplix.converters.fr2afop.fr.dataset.Row;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.Stickers;
import ru.aplix.packline.controller.GenStickCustomerController;
import ru.aplix.packline.post.BoxType;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagList;

public class GenStickAction extends BaseStickAction<GenStickCustomerController, BoxType> {

	private int count;
	private Parameter countParam = null;
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
	public BoxType processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		BoxType result = postServicePort.getBoxType(code);
		if (result == null || result.getId() == null || result.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		return result;
	}

	public void generateAndPrint(int count, String boxTypeId) throws PackLineException {
		this.count = count;
		this.boxTypeId = boxTypeId;

		generateAndPrint();
	}

	@Override
	protected void beforeResolving(Report report) throws Exception {
		super.beforeResolving(report);
		
		if (countParam != null) {
			countParam.setValue("" + count);
		}
	}

	@Override
	protected void afterResolving(Report report) throws Exception {
		super.afterResolving(report);
		
		if (boxTypeId != null) {
			TagList tagList = new TagList();
			for (Dataset dataset : report.getDatasets()) {
				for (Row row : dataset.getRows()) {
					for (Column column : row.getColumns()) {
						Tag tag = new Tag();
						tag.setId(column.getValue());
						tagList.getItems().add(tag);
					}
				}
			}

			PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
			boolean res = postServicePort.addBoxContainers(boxTypeId, tagList);
			if (!res) {
				throw new PackLineException(getResources().getString("error.post.add.containers"));
			}
		}
	}

	@Override
	protected void prepareDatabase(ru.aplix.converters.fr2afop.fr.Configuration configuration) {
		super.prepareDatabase(configuration);

		// Get reference to container Id parameter
		for (Dataset dataset : configuration.getDatasets()) {
			if ("MarkersForContainersDataSet".equals(dataset.getName())) {
				for (Parameter parameter : dataset.getParameters()) {
					if (Const.COUNT_PARAM.equals(parameter.getName())) {
						countParam = parameter;
					}
				}
			}
		}
	}
}
