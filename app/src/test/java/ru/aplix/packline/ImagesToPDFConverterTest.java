package ru.aplix.packline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ru.aplix.packline.utils.ImagesToPDFConverter;

public class ImagesToPDFConverterTest extends TestCase {

	public void test() throws Exception {
		List<File> imageFiles = new ArrayList<File>();
		imageFiles.add(new File(getClass().getResource("/resources/images/img-barcode-box.png").getFile()));
		imageFiles.add(new File(getClass().getResource("/resources/images/img-barcode-order.png").getFile()));

		File pdfFile = File.createTempFile("sample", ".pdf");
		try {

			ImagesToPDFConverter itpc = new ImagesToPDFConverter();
			itpc.convert(imageFiles, pdfFile.getAbsolutePath());

			assertTrue(pdfFile.exists());
		} finally {
			assertTrue(pdfFile.delete());
		}
	}
}
