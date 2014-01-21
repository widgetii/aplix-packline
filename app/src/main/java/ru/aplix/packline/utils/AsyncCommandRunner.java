package ru.aplix.packline.utils;

import org.springframework.scheduling.annotation.Async;

public class AsyncCommandRunner {

	@Async
	public void exec(Runnable runnable) {
		runnable.run();
	}
}
