package ru.aplix.packline.controller;

import java.util.List;

import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.PrintFormsBeforeWeightingAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;
import ru.aplix.packline.conf.WhenPrint;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class PrintFormsBeforeWeightingController extends StandardController<PrintFormsBeforeWeightingAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	private Scales<?> scales = null;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);
		getAction().prepare();

		scales = (Scales<?>) context.getAttribute(Const.SCALES);
		if (scales != null) {
			// Run printing task to print form before weighting.
			// We do not care about this task further.
			new Thread(new PrintTask()).start();
		}

		throw new SkipActionException();
	}

	/**
	 * 
	 */
	private class PrintTask extends Task<Void> {

		private Container container;
		private Post post;

		public PrintTask() {
			super();

			container = (Container) getContext().getAttribute(Const.TAG);
			post = (Post) getContext().getAttribute(Const.POST);
		}

		@Override
		public Void call() throws Exception {
			long t = System.currentTimeMillis();
			boolean printedAtLeastOne = false;

			// Enumerate all forms
			List<PrintForm> forms = Configuration.getInstance().getPrintForms();
			for (PrintForm form : forms) {
				if (!WhenPrint.BEFORE_WEIGHTING.equals(form.getWhenPrint())) {
					continue;
				}

				boolean postTypeRestriction = (form.getPostTypes().size() == 0 || form.getPostTypes().contains(post.getPostType()));
				boolean paymentMethodRestriction = (form.getPaymentFlags().size() == 0 || form.getPaymentFlags().contains(post.getPaymentFlags()));

				if (postTypeRestriction && paymentMethodRestriction) {
					try {
						getAction().printForms(container.getId(), post.getId(), form);
						printedAtLeastOne = true;
					} catch (Throwable e) {
						LOG.error(null, e);
					}
				}
			}

			// Log printing time
			t = System.currentTimeMillis() - t;
			if (printedAtLeastOne) {
				LOG.info(String.format("Printing time: %.1f sec", (float) t / 1000f));
			}

			return null;
		}
	};
}
