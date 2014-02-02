package ru.aplix.packline.jws;

import java.io.File;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public abstract class BaseClass {

	static {
		ConsoleAppender ca = new ConsoleAppender();
		ca.setName("console");
		ca.setLayout(new PatternLayout("%m%n"));
		ca.setTarget("System.out");
		ca.setThreshold(Level.INFO);
		ca.activateOptions();
		Logger.getRootLogger().addAppender(ca);

		RollingFileAppender fa = new RollingFileAppender();
		fa.setName("file");
		fa.setLayout(new PatternLayout("%d{ABSOLUTE} %5p %c{1}:%L - %m%n"));
		fa.setFile(System.getProperty("java.io.tmpdir", "") + File.separator + "packline-jws.log");
		fa.setMaximumFileSize(0x20000);
		fa.setAppend(true);
		fa.setThreshold(Level.INFO);
		fa.activateOptions();
		Logger.getRootLogger().addAppender(fa);
	}
}
