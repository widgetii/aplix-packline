package ru.aplix.packline.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintMode;
import ru.aplix.packline.conf.Printer;
import ru.aplix.packline.controller.OrderActController;
import ru.aplix.packline.post.ActionType;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.workflow.WorkflowAction;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.Barcode39;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

public class OrderActAction extends BasePrintFormsAction<OrderActController> {

	private WorkflowAction idleAction;
	private WorkflowAction deleteActAction;
	private WorkflowAction closeActAction;

	public WorkflowAction getIdleAction() {
		return idleAction;
	}

	public void setIdleAction(WorkflowAction idleAction) {
		this.idleAction = idleAction;
	}

	public WorkflowAction getDeleteActAction() {
		return deleteActAction;
	}

	public void setDeleteActAction(WorkflowAction deleteActAction) {
		this.deleteActAction = deleteActAction;
	}

	public WorkflowAction getCloseActAction() {
		return closeActAction;
	}

	public void setCloseActAction(WorkflowAction closeActAction) {
		this.closeActAction = closeActAction;
	}

	@Override
	protected String getFormName() {
		return "order-act";
	}

	public boolean processBarcode(String code) throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (!TagType.INCOMING.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.post.not.incoming"));
		}
		final Incoming incoming = postServicePort.findIncoming(code);
		if (incoming == null || incoming.getId() == null || incoming.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		Order order = postServicePort.getOrder(incoming.getOrderId());
		if (order != null && (order.getId() == null || order.getId().length() == 0)) {
			throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
		}
		if (registry.getCustomer() == null || order.getCustomer() == null || !registry.getCustomer().getId().equals(order.getCustomer().getId())) {
			throw new PackLineException(getResources().getString("error.post.incoming.incorrect.customer"));
		}

		Incoming existing;
		switch (registry.getActionType()) {
		case ADD:
			// Now we should add a new incoming to our registry.
			// Check that it hasn't been added yet.
			existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return incoming.getId().equals(((Tag) item).getId());
				}
			});
			if (existing != null) {
				throw new PackLineException(getResources().getString("error.post.incoming.registered"));
			}

			break;
		case DELETE:
			// Now we should delete selected incoming from our registry.
			// Check that incoming is present in registry first.
			existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object o) {
					Incoming item = (Incoming) o;
					boolean result = incoming.getId().equals(item.getId());
					if (!result && item.getBarcodes() != null) {
						result = ArrayUtils.contains(item.getBarcodes().toArray(), incoming.getId());
					}
					return result;
				}
			});
			if (existing == null) {
				throw new PackLineException(getResources().getString("error.post.incoming.other.registy"));
			}
			break;
		}

		return true;
	}

	public ActionType carryOutRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		setNextAction(getCloseActAction());

		if (!postServicePort.carryOutRegistry(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.carryout"));
		}

		return registry.getActionType();
	}

	public void printAct(boolean print) throws Exception {
		if (print)
			printAct();
		else
			setNextAction(getIdleAction());
	}

	private void printAct() throws Exception {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		File tempFile = File.createTempFile("incoming_" + registry.getId(), ".pdf");
		tempFile.deleteOnExit();

		FileOutputStream os = new FileOutputStream(tempFile);

		try {
			generateActReport(registry, os);
			os.flush();
		} finally {
			os.close();
		}

		Printer printer = Configuration.getInstance().getHardwareConfiguration().lookupPrinter(PrintMode.PDF);
		if (printer == null)
			throw new PackLineException(getResources().getString("error.printer.pdf.not.assigned"));

		// Invoke pdf printer
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("\"%s%s\" -p \"%s\" \"%s\"", getJarFolder(), Const.PDF_PRINTER_FILE, printer.getName(), tempFile.getAbsolutePath()));

		String command = sb.toString();
		LOG.debug("Executing command: " + command);
		Runtime.getRuntime().exec(command);
	}

	private void generateActReport(Registry registry, OutputStream os) throws DocumentException, IOException {
		LOG.debug("generateActReport");

		URL imageUrl = null;
		String img = String.format("%s%s", getJarFolder(), Const.APLIX_LOGO);

		File f = new File(img);
		if (f.exists() && !f.isDirectory())
			try {
				imageUrl = new URL(String.format("file:///%s", img));
			} catch (Exception e) {
				LOG.error(String.format("Can't find logo image at %s", img), e);
			}

		final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, getLocale());
		// final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

		// set fonts
		String fpath = String.format("%s%s", getJarFolder(), Const.FONTS);
		FontFactory.registerDirectory(fpath);

		final com.itextpdf.text.Font cellFont = FontFactory.getFont("Arial", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 11);
		final com.itextpdf.text.Font headerFont = FontFactory.getFont("Arial Bold", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 11);

		final float defaultSpacing = 5f;

		String agreementNumber = "_________";
		String agreementDate = "__ __________ ____";

		String identifier = "PR-" + registry.getId();

		Document document = new Document(PageSize.A4, 36, 36, 54, 54);
		PdfWriter pdfWriter = PdfWriter.getInstance(document, os);
		document.addTitle(getText("acceptance.report.title"));
		document.addAuthor(getText("acceptance.report.author"));
		document.addSubject(getText("acceptance.report.subject", new Object[] { identifier }));

		pdfWriter.setPageEvent(new PdfPageEventHelper() {
			@Override
			public void onEndPage(PdfWriter writer, Document document) {
				String text = getText("acceptance.report.page", new Object[] { "" + document.getPageNumber() });
				Rectangle rect = document.getPageSize();
				ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER | Element.ALIGN_MIDDLE, new Phrase(text, cellFont),
						(rect.getLeft() + rect.getRight()) / 2, rect.getBottom() + 27, 0);
			}
		});

		document.open();
		try {
			// Add number and logo
			PdfPTable table = new PdfPTable(new float[] { 0.5f, 0.5f });
			table.setWidthPercentage(100);
			table.setSpacingAfter(defaultSpacing);

			PdfPCell cell = new PdfPCell();
			cell.setHorizontalAlignment(Element.ALIGN_LEFT);
			cell.setBorder(PdfPCell.NO_BORDER);
			if (imageUrl != null) {
				try {
					Image image = Image.getInstance(imageUrl);
					image.setWidthPercentage(40f);
					cell.addElement(image);
				} catch (Exception e) {
					LOG.error(String.format("Can't use logo image"), e);
				}
			}
			table.addCell(cell);

			try {
				Barcode39 code39 = new Barcode39();
				code39.setCode(identifier);
				code39.setX(1f);
				code39.setStartStopText(false);
				Image barcodeimage = code39.createImageWithBarcode(pdfWriter.getDirectContent(), null, null);
				cell = new PdfPCell(barcodeimage);
			} catch (Exception e) {
				cell = new PdfPCell();
			}
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			document.add(table);

			// Add header
			table = new PdfPTable(new float[] { 0.25f, 0.5f, 0.25f });
			table.setWidthPercentage(100);
			table.setSpacingAfter(defaultSpacing);

			cell = new PdfPCell(new Paragraph(getText("acceptance.report.city"), cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_LEFT);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.header", new Object[] { agreementNumber, agreementDate }), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(dateFormat.format(new Date()), cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);

			document.add(table);

			// Add statement
			Paragraph paragraph = new Paragraph(getText("acceptance.report.statement.noagreement", new Object[] { registry.getCustomer().getName(),
					getText("acceptance.report.agent.aplix") }), cellFont);

			paragraph.setSpacingBefore(3 * defaultSpacing);
			paragraph.setSpacingAfter(defaultSpacing);
			paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
			document.add(paragraph);

			// Add table
			table = new PdfPTable(new float[] { 0.1f, 0.3f, 0.3f, 0.3f });
			table.setSpacingBefore(3 * defaultSpacing);
			table.setWidthPercentage(100);
			table.setSpacingAfter(defaultSpacing);

			cell = new PdfPCell(new Paragraph(getText("acceptance.report.order"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.number"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.count"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.weight"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);

			int i = 1;
			int totalPlaces = 0;
			Float totalWeight = 0f;

			for (Incoming incoming : registry.getIncoming()) {
				cell = new PdfPCell(new Paragraph("" + i, cellFont));
				cell.setPaddingBottom(defaultSpacing);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				table.addCell(cell);

				String number;
				int places;

				number = incoming.getId();
				places = 1;

				Float weight = incoming.getWeight();
				totalWeight = totalWeight + weight;
				totalPlaces += places;

				cell = new PdfPCell(new Paragraph(number, cellFont));
				cell.setPaddingBottom(defaultSpacing);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				table.addCell(cell);

				cell = new PdfPCell(new Paragraph("" + places, cellFont));
				cell.setPaddingBottom(defaultSpacing);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				table.addCell(cell);

				cell = new PdfPCell(new Paragraph(weight != null ? String.format(getLocale(), "%.3f", weight) : "", cellFont));
				cell.setPaddingBottom(defaultSpacing);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				table.addCell(cell);

				i++;
			}

			cell = new PdfPCell(new Paragraph(getText("acceptance.report.total"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setColspan(2);
			table.addCell(cell);

			cell = new PdfPCell(new Paragraph("" + totalPlaces, cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);

			cell = new PdfPCell(new Paragraph(totalWeight != null ? String.format(getLocale(), "%.3f", totalWeight) : "", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);

			document.add(table);

			// Add summary
			String totalCount = String.format(getLocale(), "%d %s", totalPlaces, getText("acceptance.report.count.ending" + getNumEndingIndex(totalPlaces)));

			paragraph = new Paragraph(getText("acceptance.report.summary", new Object[] { totalCount }), cellFont);
			paragraph.setSpacingBefore(defaultSpacing);
			paragraph.setSpacingAfter(defaultSpacing);
			paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
			document.add(paragraph);

			// Add signers
			table = new PdfPTable(new float[] { 0.47f, 0.06f, 0.47f });
			table.setSpacingBefore(3 * defaultSpacing);
			table.setWidthPercentage(100);
			table.setSpacingAfter(defaultSpacing);
			table.setKeepTogether(true);

			cell = new PdfPCell(new Paragraph(getText("acceptance.report.principal"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell();
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.agent"), headerFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.give", new Object[] { registry.getCustomer().getName() }), cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell();
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.take", new Object[] { getText("acceptance.report.agent.aplix") }), cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph("_____________________/______________/", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell();
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph("_____________________/______________/", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph("___________________________________", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell();
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph("___________________________________", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph("/" + getText("acceptance.report.position") + "/", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_TOP);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell();
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph("/" + getText("acceptance.report.position") + "/", cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_TOP);
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.stamp"), cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			cell.setFixedHeight(50f);
			table.addCell(cell);
			cell = new PdfPCell();
			cell.setBorder(PdfPCell.NO_BORDER);
			table.addCell(cell);
			cell = new PdfPCell(new Paragraph(getText("acceptance.report.stamp"), cellFont));
			cell.setPaddingBottom(defaultSpacing);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
			cell.setBorder(PdfPCell.NO_BORDER);
			cell.setFixedHeight(50f);
			table.addCell(cell);

			document.add(table);
		} finally {
			document.close();
		}
	}

	private String getText(String text) {
		String s;
		try {
			s = getResources().getString(text);
		} catch (Exception e) {
			s = text;
		}
		return s;
	}

	private String getText(String text, Object[] args) {
		String s = "";
		try {
			s = String.format(getText(text), args);
		} catch (Exception e) {
			LOG.error("String format exception!" + e);
		}
		return s;
	}

	private Locale getLocale() {
		return Locale.getDefault();
	}

	public static int getNumEndingIndex(int a) {
		int r = a % 100;
		if (r >= 11 && r <= 19) {
			return 3;
		} else {
			r = a % 10;
			if (r == 1) {
				return 1;
			} else if (r >= 2 && r <= 4) {
				return 2;
			} else {
				return 3;
			}
		}
	}

	public void saveAct() {
		setNextAction(getIdleAction());
	}

	public void deleteRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		setNextAction(getDeleteActAction());

		if (!postServicePort.deleteRegistry(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.delete"));
		}
	}

	public void deleteIncoming(final Incoming incoming) throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteIncomingFromRegistry(registry.getId(), incoming)) {
			throw new PackLineException(getResources().getString("error.post.registry.incoming.delete"));
		}

		// Delete incoming from registry
		registry.getIncoming().remove(incoming);

		// Delete incoming from order as well
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (order != null) {
			Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return incoming.getId().equals(((Incoming) object).getId());
				}
			});
			if (existing != null) {
				order.getIncoming().remove(existing);
			}
		}
	}
}
