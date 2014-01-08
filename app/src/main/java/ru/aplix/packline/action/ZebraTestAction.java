package ru.aplix.packline.action;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.controller.ZebraTestController;

public class ZebraTestAction extends CommonAction<ZebraTestController> {

	private String jarFolder = null;

	@Override
	protected String getFormName() {
		return "zebra-test";
	}

	protected String getReportName() {
		return "zebra-test";
	}

	public void test() throws PackLineException, FileNotFoundException, MalformedURLException, JAXBException {
		Printer printer = Configuration.getInstance().getZebraTest().getPrinter();
		if (printer == null) {
			throw new PackLineException(getResources().getString("error.printer.not.assigned"));
		}

		try {
			if (jarFolder == null) {
				File confFolder = new File(Configuration.getConfigFileName()).getParentFile();
				jarFolder = confFolder != null ? confFolder.getParent() : "";
			}

			InputStream is = new FileInputStream(jarFolder + String.format(Const.REPORT_FILE_TEMPLATE, getReportName()));
			
			Socket socket = new Socket(printer.getIpAddress(), printer.getPort());
			try {
				BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
				try {
					IOUtils.copy(is, bos);
				} finally {
					bos.close();
				}
			} finally {
				socket.close();
			}
		} catch (Throwable e) {
			PackLineException ple = new PackLineException(String.format(getResources().getString("error.printing"), printer.getName()), e);
			throw ple;
		}
	}
}
