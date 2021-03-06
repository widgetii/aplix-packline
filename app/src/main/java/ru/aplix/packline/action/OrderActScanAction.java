package ru.aplix.packline.action;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.controller.OrderActScanController;
import ru.aplix.packline.post.DocumentType;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.ImagesToPDFConverter;

public class OrderActScanAction extends CommonAction<OrderActScanController> {

	@Override
	protected String getFormName() {
		return "order-act-scan";
	}

	public void processBarcode(String code, List<File> images) throws PackLineException {
		uploadRegistry(code, images);
	}

	private void uploadRegistry(String code, List<File> images) throws PackLineException {
		if (images.size() == 0) {
			return;
		}

		try {
			File pdfFile = File.createTempFile(code, ".pdf");
			try {
				ImagesToPDFConverter itpc = new ImagesToPDFConverter();
				itpc.convert(images, pdfFile.getAbsolutePath());

				File destFile = new File(Configuration.getInstance().getPostService().getRemoteStoragePath(), pdfFile.getName());
				if (!destFile.equals(pdfFile)) {
					FileUtils.copyFile(pdfFile, destFile);

					PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
					String result = postServicePort.fileUpload(code, destFile.getAbsolutePath(), DocumentType.RETURN_REGISTRY);
					if (result != null && result.length() > 0) {
						throw new PackLineException(result);
					}
				}
			} finally {
				pdfFile.delete();
			}
		} catch (Exception e) {
			throw new PackLineException(getResources().getString("error.file.upload"), e);
		}
	}

}
