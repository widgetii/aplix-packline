package ru.aplix.packline.action;

import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Parameter;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.Stickers;
import ru.aplix.packline.controller.AskGenStickCustomerController;
import ru.aplix.packline.post.Customer;
import ru.aplix.packline.post.PackingLinePortType;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class AskGenStickCustomerAction extends BaseStickAction<AskGenStickCustomerController, Customer> {

	private String customerCode;
	private Parameter customerCodeParam = null;

	@Override
	protected String getFormName() {
		return "ask-sticking-customer";
	}

	@Override
	protected Stickers getStickers() throws FileNotFoundException, MalformedURLException, JAXBException {
		return Configuration.getInstance().getStickers().getForCustomers();
	}

	@Override
	protected String getReportName() {
		return "mcust";
	}

	@Override
	protected String getDatesetName() {
		return Const.MARKERS_FOR_CUSTOMERS_DATASET;
	}

	@Override
	public Customer processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Customer result = postServicePort.getCustomer(code);
		if (result == null || result.getId() == null || result.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		if (getLastTagList() != null) {
			setLastTagList(null);
		}
		
		return result;
	}

	public void generateAndPrint(int offset, int count, String customerCode) throws PackLineException {
		this.customerCode = customerCode;

		if (customerCode == null) {
			throw new PackLineException(getResources().getString("error.customer.not.selected"));
		}

		generateAndPrint(offset, count);
	}

	@Override
	protected void beforeResolving(Report report) throws Exception {
		super.beforeResolving(report);

		if (customerCodeParam != null) {
			customerCodeParam.setValue(customerCode);
		}
	}

	@Override
	protected void prepareDatabase(ru.aplix.converters.fr2afop.fr.Configuration configuration) {
		super.prepareDatabase(configuration);

		// Get reference to container Id parameter
		for (Dataset dataset : configuration.getDatasets()) {
			if (getDatesetName().equals(dataset.getName())) {
				for (Parameter parameter : dataset.getParameters()) {
					if (Const.CUSTOMER_CODE_PARAM.equals(parameter.getName())) {
						customerCodeParam = parameter;
					}
				}
			}
		}
	}
}
