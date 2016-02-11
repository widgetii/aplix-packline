package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.SelectCarrierController;
import ru.aplix.packline.post.PostType;

public class SelectCarrierAction extends CommonAction<SelectCarrierController> {

	@Override
	protected String getFormName() {
		return "return-registry-carrier";
	}

	public void select(String value) throws PackLineException {
		getContext().setAttribute(Const.SELECTED_CARRIER_POSTTYPE, getCarrier(value));
		getContext().setAttribute(Const.SELECTED_CARRIER, value);
	}

	private PostType getCarrier(String selectedCarrier) throws PackLineException {
		if ("Aplix".equals(selectedCarrier)) {
			return PostType.PACKAGE;
		} else if ("CDEK".equals(selectedCarrier)) {
			return PostType.CDEK;
		} else if ("DHL".equals(selectedCarrier)) {
			return PostType.DHL;
		} else if ("DPD".equals(selectedCarrier)) {
			return PostType.DPD;
		} else if ("EMS".equals(selectedCarrier)) {
			return PostType.EMS;
		} else if ("IML".equals(selectedCarrier)) {
			return PostType.IML;
		} else if ("PickPoint".equals(selectedCarrier)) {
			return PostType.PICKPOINT;
		} else if ("QIWI".equals(selectedCarrier)) {
			return PostType.QIWIPOST;
		} else if ("RussianPost".equals(selectedCarrier)) {
			return PostType.PARCEL;
		} else if ("SPSR".equals(selectedCarrier)) {
			return PostType.SPSR;
		} else if ("Logibox".equals(selectedCarrier)) {
			return PostType.LOGIBOX;
		} else if ("Boxberry".equals(selectedCarrier)) {
			return PostType.BOXBERRY;
		} else if ("Hermes".equals(selectedCarrier)) {
			return PostType.HERMES;
		} else if ("Ponyexpress".equals(selectedCarrier)) {
			return PostType.PONYEXPRESS;
		} else if ("B2CPL".equals(selectedCarrier)) {
			return PostType.B_2_CPL;
		} else {
			throw new PackLineException(String.format(getResources().getString("error.unknown.carrier"), selectedCarrier));
		}
	}
}
