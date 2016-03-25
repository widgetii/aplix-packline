package ru.aplix.packline.utils;

import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;

import ru.aplix.packline.PackLineException;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;

public class CacheOrders {

	@Autowired
	ResourceBundleMessageSource messageSource;

	public ResourceBundle getResources() {
		return new MessageSourceResourceBundle(messageSource, Locale.getDefault());
	}

	@Cacheable(value = "CACHE_ORDERS", key = "#orderId")
	public Order findOrder(PackingLinePortType postServicePort, String orderId, boolean strict, boolean requestToServer) throws PackLineException {
		Order order = null;
		if (requestToServer) {
		    order = postServicePort.getOrder(orderId);
			if (strict && (order == null || order.getId() == null || order.getId().length() == 0)) {
				throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
			}
		}
		return order;
	}
}
