package ru.aplix.packline.dialog;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ManualInputController {

	@FXML
	private TextField editText;

	private ManualInputListener listener;

	public ManualInputListener getListener() {
		return listener;
	}

	public void setListener(ManualInputListener listener) {
		this.listener = listener;
	}

	private void close() {
		Window owner = editText.getScene().getWindow();
		if (owner instanceof Stage) {
			((Stage) owner).close();
		}
	}

	private TextField getFocusedEdit() {
		if (editText.isFocused()) {
			return editText;
		} else {
			editText.requestFocus();
			return null;
		}
	}

	private void addSymbol(String s) {
		TextField tf = getFocusedEdit();
		if (tf != null) {
			String text = tf.getText();
			text = (text == null ? "" : text) + s;
			tf.setText(text);
			tf.positionCaret(text.length());
		}
	}

	public void cancelClick(ActionEvent event) {
		close();
	}

	public void numericKeybordEnterClick(ActionEvent event) {
		close();

		if (listener != null) {
			listener.onTextInput(editText.getText());
		}
	}

	public void numericKeybordBackClick(ActionEvent event) {
		TextField tf = getFocusedEdit();
		if (tf != null) {
			String text = tf.getText();
			if (text != null && text.length() > 0) {
				text = text.substring(0, Math.max(0, text.length() - 1));
				tf.setText(text);
				tf.positionCaret(text.length());
			}
		}
	}

	public void numericKeybordClearClick(ActionEvent event) {
		TextField tf = getFocusedEdit();
		if (tf != null) {
			tf.setText(null);
		}
	}

	public void numericKeybordDotClick(ActionEvent event) {
		addSymbol(".");
	}

	public void numericKeybord0Click(ActionEvent event) {
		addSymbol("0");
	}

	public void numericKeybord1Click(ActionEvent event) {
		addSymbol("1");
	}

	public void numericKeybord2Click(ActionEvent event) {
		addSymbol("2");
	}

	public void numericKeybord3Click(ActionEvent event) {
		addSymbol("3");
	}

	public void numericKeybord4Click(ActionEvent event) {
		addSymbol("4");
	}

	public void numericKeybord5Click(ActionEvent event) {
		addSymbol("5");
	}

	public void numericKeybord6Click(ActionEvent event) {
		addSymbol("6");
	}

	public void numericKeybord7Click(ActionEvent event) {
		addSymbol("7");
	}

	public void numericKeybord8Click(ActionEvent event) {
		addSymbol("8");
	}

	public void numericKeybord9Click(ActionEvent event) {
		addSymbol("9");
	}
}
