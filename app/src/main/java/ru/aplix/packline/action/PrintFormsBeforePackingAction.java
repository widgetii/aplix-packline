package ru.aplix.packline.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.springframework.util.StringUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintMode;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.controller.PrintFormsBeforePackingController;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.PrintDocument;
import ru.aplix.packline.post.PrintingDocumentsResponse;

public class PrintFormsBeforePackingAction extends BasePrintFormsAction<PrintFormsBeforePackingController> {

	@Override
	protected String getFormName() {
		return "printing-before-packing";
	}

	public void downloadAndPrintDocuments(int[] printed) throws Exception {
		Post post = (Post) getContext().getAttribute(Const.POST);

		// Download label from server
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		PrintingDocumentsResponse response = postServicePort.getDocumentsForPrinting(post.getId());
		if (response == null) {
			throw new PackLineException(getResources().getString("error.post.printing.documents"));
		}
		if (!StringUtils.isEmpty(response.getError())) {
			throw new PackLineException(response.getError());
		}
		if (response.getItems() == null || response.getItems().size() == 0) {
			return;
		}

		Printer printer = Configuration.getInstance().getHardwareConfiguration().lookupPrinter(PrintMode.PDF);
		if (printer == null) {
			throw new PackLineException(getResources().getString("error.printer.pdf.not.assigned"));
		}

		for (int index = 0; index < response.getItems().size(); index++) {
			PrintDocument pd = response.getItems().get(index);

			// Save it in temp file
			File tempFile = File.createTempFile(post.getId() + "_" + (index + 1), ".pdf");
			LOG.debug(String.format("Saving label temporary to '%s'...", tempFile.getAbsolutePath()));
			tempFile.deleteOnExit();
			OutputStream os = new FileOutputStream(tempFile);
			try {
				os.write(pd.getFileContents());
				os.flush();
			} finally {
				os.close();
			}

			// Invoke pdf printer
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("\"%s%s%s\" -p \"%s\" \"%s\"", getJarFolder(), File.separatorChar, Const.PDF_PRINTER_FILE, printer.getName(),
					tempFile.getAbsolutePath()));

			for (int i = 2; i <= Math.max(pd.getCopies(), 1); i++) {
				sb.append(String.format(" \"%s\"", tempFile.getAbsolutePath()));
			}

			String command = sb.toString();
			LOG.debug("Executing command: " + command);
			Runtime.getRuntime().exec(command);

			printed[0]++;
		}
	}
}
