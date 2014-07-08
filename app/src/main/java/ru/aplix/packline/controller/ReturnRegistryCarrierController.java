package ru.aplix.packline.controller;

import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import ru.aplix.packline.action.ReturnRegistryCarrierAction;

public class ReturnRegistryCarrierController extends StandardController<ReturnRegistryCarrierAction> {

	@FXML
	private ImageView logoAplix;
	@FXML
	private ImageView logoCDEK;
	@FXML
	private ImageView logoDHL;
	@FXML
	private ImageView logoDPD;
	@FXML
	private ImageView logoEMS;
	@FXML
	private ImageView logoIML;
	@FXML
	private ImageView logoPickPoint;
	@FXML
	private ImageView logoQIWI;
	@FXML
	private ImageView logoRussianPost;
	@FXML
	private ImageView logoSPSR;

	public void onImageClicked(MouseEvent mouseEvent) {
		String value = null;
		if (mouseEvent.getSource().equals(logoAplix)) {
			value = "Aplix";
		} else if (mouseEvent.getSource().equals(logoCDEK)) {
			value = "CDEK";
		} else if (mouseEvent.getSource().equals(logoDHL)) {
			value = "DHL";
		} else if (mouseEvent.getSource().equals(logoDPD)) {
			value = "DPD";
		} else if (mouseEvent.getSource().equals(logoEMS)) {
			value = "EMS";
		} else if (mouseEvent.getSource().equals(logoIML)) {
			value = "IML";
		} else if (mouseEvent.getSource().equals(logoPickPoint)) {
			value = "PickPoint";
		} else if (mouseEvent.getSource().equals(logoQIWI)) {
			value = "QIWI";
		} else if (mouseEvent.getSource().equals(logoRussianPost)) {
			value = "RussianPost";
		} else if (mouseEvent.getSource().equals(logoSPSR)) {
			value = "SPSR";
		}

		getAction().select(value);
		done();
	}
}