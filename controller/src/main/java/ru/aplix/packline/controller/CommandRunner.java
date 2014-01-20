package ru.aplix.packline.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CommandRunner {

	private static final Logger logger = LoggerFactory.getLogger(CommandRunner.class);

	@Async(value = "commandExecutor")
	public void exec(String command) {
		try {
			logger.debug("Executing command: " + command);
			Runtime.getRuntime().exec(command);
		} catch (Exception e) {
			logger.error(null, e);
		}
	}
}
