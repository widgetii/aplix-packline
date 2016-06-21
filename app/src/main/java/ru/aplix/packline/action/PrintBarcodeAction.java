package ru.aplix.packline.action;

import ru.aplix.converters.fr2afop.fr.Report;
import ru.aplix.converters.fr2afop.fr.dataset.Column;
import ru.aplix.converters.fr2afop.fr.dataset.Dataset;
import ru.aplix.converters.fr2afop.fr.dataset.Row;
import ru.aplix.packline.Const;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.controller.PrintBarcodeController;

import java.util.ArrayList;
import java.util.List;

public class PrintBarcodeAction extends BasePrintAction<PrintBarcodeController> {

	private int length;

	private static final String widthFormat = "PL-W %04d";

	private static final String heightFormat = "PL-H %04d";

	private static final String lengthFormat = "PL-L %04d";

	private static final String reportFileName = "barcode-line";

	@Override
	protected String getFormName() {
		return "print-barcode";
	}

	@Override
	protected void beforeResolving(Report report) {
		List<Dataset> datasets = new ArrayList<>();
		Dataset dataset = new Dataset();
		dataset.setName("GenerateLineDataSet");

		List<Row> rows = new ArrayList<>();
		for( int i = 1; i <= length; i++) {
			Row row = new Row();

			List<Column> columns = new ArrayList<>();
			Column column;

			column = new Column();
			column.setName("VarBarCodeIndex");
			column.setValue(String.valueOf(i));
			columns.add(column);

			column = new Column();
			column.setName("VarBarCodeWidth1");
			column.setValue(String.format(widthFormat, (i * 10) - 5));
			columns.add(column);

			column = new Column();
			column.setName("VarBarCodeHeight1");
			column.setValue(String.format(heightFormat, (i * 10) - 5));
			columns.add(column);

			column = new Column();
			column.setName("VarBarCodeLength1");
			column.setValue(String.format(lengthFormat, (i * 10) - 5));
			columns.add(column);

			column = new Column();
			column.setName("VarBarCodeWidth2");
			column.setValue(String.format(widthFormat, i * 10));
			columns.add(column);

			column = new Column();
			column.setName("VarBarCodeHeight2");
			column.setValue(String.format(heightFormat, i * 10));
			columns.add(column);

			column = new Column();
			column.setName("VarBarCodeLength2");
			column.setValue(String.format(lengthFormat, i * 10));
			columns.add(column);

			row.setColumns(columns);

			rows.add(row);
		}

		dataset.setRows(rows);
		datasets.add(dataset);
		report.setDatasets(datasets);
	}

	public boolean printBarcodeLine(int length, Printer printer, Integer copies) throws Exception {

		this.length = length;

		long t = System.currentTimeMillis();

		try {
			String reportFullFileName = getJarFolder() + String.format(Const.REPORT_FILE_TEMPLATE, reportFileName);
			printFromFile(reportFullFileName, printer, getResources().getString("print.barcode.formName"), copies);

			t = System.currentTimeMillis() - t;
			LOG.info(String.format("Printing time: %.1f sec", (float) t / 1000f));
			return true;

		} catch (Exception e) {
			LOG.error(null, e);

			throw e;
		}
	}
}
