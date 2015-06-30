package ru.aplix.packline.action;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.controller.RemarkingController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;

public class RemarkingAction extends BasePrintFormsAction<RemarkingController> {

	@Override
	protected String getFormName() {
		return "remarking";
	}

	public boolean processBarcode(String code) throws Exception {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Container container = postServicePort.searchContainer(code);
		if (container == null || container.getId() == null || container.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		Post post = postServicePort.findPost(container.getPostId());
		if (post == null || post.getId() == null || post.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.post.container.incorrect.post"));
		}

		getContext().setAttribute(Const.TAG, container);
		getContext().setAttribute(Const.POST, post);

		long t = System.currentTimeMillis();
		boolean printedAtLeastOne = false;
		Set<Throwable> exceptions = new TreeSet<Throwable>(createSetComparator());
		try {
			List<PrintForm> forms = Configuration.getInstance().getPrintForms();
			for (PrintForm form : forms) {
				boolean postTypeRestriction = (form.getPostTypes().size() == 0 || form.getPostTypes().contains(post.getPostType()));
				boolean paymentMethodRestriction = (form.getPaymentFlags().size() == 0 || form.getPaymentFlags().contains(post.getPaymentFlags()));

				if (postTypeRestriction && paymentMethodRestriction && form.getEnabled()) {
					printForms(container.getId(), post.getId(), form);

					printedAtLeastOne = true;
				}
			}
		} catch (Throwable e) {
			LOG.error(null, e);
			exceptions.add(e);
		}

		// Log printing time
		t = System.currentTimeMillis() - t;
		if (printedAtLeastOne) {
			LOG.info(String.format("Printing time: %.1f sec", (float) t / 1000f));
		}

		// Throw single exception
		if (exceptions.size() == 1) {
			Throwable item = exceptions.iterator().next();
			if (item instanceof Exception) {
				throw (Exception) item;
			} else {
				throw new PackLineException(item);
			}
		} else if (exceptions.size() > 1) {
			StringBuffer sb = new StringBuffer();
			for (Throwable e : exceptions) {
				sb.append(e.getMessage());
				sb.append("\n");
			}
			throw new PackLineException(sb.toString());
		}

		return true;
	}

	private Comparator<Throwable> createSetComparator() {
		return new Comparator<Throwable>() {
			@Override
			public int compare(Throwable o1, Throwable o2) {
				int res = o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
				if (res != 0) {
					return res;
				}

				if (o1.getMessage() == null && o2.getMessage() == null) {
					return 0;
				} else if (o1.getMessage() == null) {
					return 1;
				} else {
					return o1.getMessage().compareTo(o2.getMessage());
				}
			}
		};
	}
}
