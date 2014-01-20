package ru.aplix.packline.controller;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * CustomResource class.
 */
public final class CustomResource {

	private static InitialContext iniCtx = null;

	private CustomResource() {
	}

	/**
	 * Retrieve named properties from initial context.
	 * 
	 * @param name
	 *            named object name
	 * @return object found or null
	 */
	public static Properties getProperties(String name) {
		Object o = retrieveNamedObject(name);
		if (o != null && o instanceof Properties) {
			return (Properties) o;
		} else {
			return null;
		}
	}

	/**
	 * Retrieve named string from initial context.
	 * 
	 * @param name
	 *            named object name
	 * @return object found or null
	 */
	public static String getString(String name, String defaultValue) {
		Object o = retrieveNamedObject(name);
		if (o != null && o instanceof String) {
			return (String) o;
		} else {
			return defaultValue;
		}
	}

	/**
	 * Retrieve named integer from initial context.
	 * 
	 * @param name
	 *            named object name
	 * @return object found or null
	 */
	public static Integer getInteger(String name, Integer defaultValue) {
		Object o = retrieveNamedObject(name);
		if (o != null && o instanceof Integer) {
			return (Integer) o;
		} else {
			return defaultValue;
		}
	}

	/**
	 * Retrieve named object from initial context.
	 * 
	 * @param name
	 *            named object name
	 * @return object found or null
	 */
	public static Object retrieveNamedObject(String name) {
		try {
			if (iniCtx == null) {
				iniCtx = new InitialContext();
			}

			// Retrieve global environment entry (from JBoss, WebLogic, ...)
			Object o = lookup(iniCtx, name);
			if (o == null) {
				o = lookup(iniCtx, String.format("java:/%s", name));
			}
			if (o == null) {
				// If not set globally, retrieve local env. entry (from web.xml)
				o = lookup(iniCtx, String.format("java:comp/env/%s", name));
			}

			return o;
		} catch (Exception e) {
			// If not set either globally or locally,
			return null;
		}
	}

	private static Object lookup(InitialContext initialContext, String name) {
		try {
			return initialContext.lookup(name);
		} catch (NamingException ne) {
			return null;
		}
	}
}
