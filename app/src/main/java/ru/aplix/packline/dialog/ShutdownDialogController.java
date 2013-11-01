package ru.aplix.packline.dialog;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ShutdownDialogController {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Parent rootNode;

	private Window anotherOwner;

	public Window getAnotherOwner() {
		return anotherOwner;
	}

	public void setAnotherOwner(Window value) {
		this.anotherOwner = value;
	}

	private Window getOwner() {
		Window owner = rootNode.getScene().getWindow();
		if (anotherOwner != null) {
			if (owner instanceof Stage) {
				((Stage) owner).close();
			}
			owner = anotherOwner;
		}
		return owner;
	}

	public void cancelClick(ActionEvent event) {
		Window owner = rootNode.getScene().getWindow();
		if (owner instanceof Stage) {
			((Stage) owner).close();
		}
	}

	public void logoffClick(ActionEvent event) {
		ConfirmationDialog cd = new ConfirmationDialog(getOwner(), "system.logoff", "confirmation.logoff", new ConfirmationListener() {

			@Override
			public void onAccept() {
				shutdown(CMD_LOGOFF, 0);
			}

			@Override
			public void onDecline() {
			}
		});
		cd.centerOnScreen();
		cd.show();
	}

	public void restartClick(ActionEvent event) {
		ConfirmationDialog cd = new ConfirmationDialog(getOwner(), "system.restart", "confirmation.restart", new ConfirmationListener() {

			@Override
			public void onAccept() {
				shutdown(CMD_RESTART, 0);
			}

			@Override
			public void onDecline() {
			}
		});
		cd.centerOnScreen();
		cd.show();
	}

	public void poweroffClick(ActionEvent event) {
		ConfirmationDialog cd = new ConfirmationDialog(getOwner(), "system.poweroff", "confirmation.poweroff", new ConfirmationListener() {

			@Override
			public void onAccept() {
				shutdown(CMD_POWEROFF, 0);
			}

			@Override
			public void onDecline() {
			}
		});
		cd.centerOnScreen();
		cd.show();
	}

	private static final int CMD_LOGOFF = 0x01;
	private static final int CMD_RESTART = 0x02;
	private static final int CMD_POWEROFF = 0x03;

	public boolean shutdown(int cmd, int timeInSeconds) {
		String shutdownCommand = null;
		if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_UNIX) {
			if (cmd == CMD_LOGOFF) {
				shutdownCommand = "logout";
			} else {
				long timeInMinutes = timeInSeconds * DateUtils.MILLIS_PER_SECOND / DateUtils.MILLIS_PER_MINUTE;
				String t = (timeInMinutes == 0) ? "now" : String.valueOf(timeInMinutes);
				String r = (cmd == CMD_RESTART) ? "r" : "h";
				shutdownCommand = String.format("shutdown -%s %s", r, t);
			}
		} else if (SystemUtils.IS_OS_WINDOWS) {
			if (cmd == CMD_LOGOFF) {
				shutdownCommand = "shutdown.exe -l";
			} else {
				String t = String.valueOf(timeInSeconds);
				String r = (cmd == CMD_RESTART) ? "r" : "s";
				shutdownCommand = String.format("shutdown.exe -%s -t %s", r, t);
			}
		} else
			return false;

		try {
			Runtime.getRuntime().exec(shutdownCommand);
		} catch (IOException e) {
			LOG.error(null, e);
		}
		return true;
	}
}
