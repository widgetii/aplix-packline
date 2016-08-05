package ru.aplix.packline.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.SelectCarrierAction;

public class SelectCarrierController extends StandardController<SelectCarrierAction> {
    @FXML
	public ImageView logoPEK;
	@FXML
	public StackPane contentPane;
	@FXML
	public Label infoLabel;
	@FXML
	public InnerShadow x1;
	@FXML
	public InnerShadow x2;
	@FXML
	public ImageView logoDELLINE;
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
	@FXML
	private ImageView logoLogibox;
	@FXML
	private ImageView logoBoxberry;
	@FXML
	private ImageView logoHermes;
	@FXML
	private ImageView logoPonyexpress;
	@FXML
	private ImageView logoB2CPL;

	public void onImageClicked(MouseEvent mouseEvent) throws PackLineException {
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
		} else if (mouseEvent.getSource().equals(logoLogibox)) {
			value = "Logibox";
		} else if (mouseEvent.getSource().equals(logoBoxberry)) {
			value = "Boxberry";
		} else if (mouseEvent.getSource().equals(logoHermes)) {
			value = "Hermes";
		} else if (mouseEvent.getSource().equals(logoPonyexpress)) {
			value = "Ponyexpress";
		} else if (mouseEvent.getSource().equals(logoB2CPL)) {
			value = "B2CPL";
		} else if (mouseEvent.getSource().equals(logoPEK)) {
		    value = "PEK";
    	} else if (mouseEvent.getSource().equals(logoDELLINE)) {
			value = "DELLINE";
		}

		getAction().select(value);
		done();
	}
}
