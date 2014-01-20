package ru.aplix.packline.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PostManager {

	private static final Logger logger = LoggerFactory.getLogger(PostManager.class);

	private static final int DEFAULT_STALE_TIMEOUT = 1000 * 60 * 10;

	@Autowired
	private CommandRunner commandRunner;

	private String cmdStartPackLine;
	private String cmdStopPackLine;
	private int staleTimeout;

	private volatile boolean running;
	private ReentrantLock connectionLock;
	private List<Parcel> parcels;

	public PostManager() {
		parcels = new ArrayList<Parcel>();
		connectionLock = new ReentrantLock();

		running = false;

		cmdStartPackLine = CustomResource.getString("cmdStartPackLine", null);
		cmdStopPackLine = CustomResource.getString("cmdStopPackLine", null);
		staleTimeout = CustomResource.getInteger("ParcelStaleTimeout", DEFAULT_STALE_TIMEOUT);

		logger.info(String.format("Initializing Packline Controller\n\tcmdStartPackLine = '%s'\n\tcmdStopPackLine = '%s'\n\tstaleTimeout = '%d'",
				cmdStartPackLine, cmdStopPackLine, staleTimeout));
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		connectionLock.lock();
		try {
			if (!running) {
				running = true;
				commandRunner.exec(cmdStartPackLine);
			}
		} finally {
			connectionLock.unlock();
		}
	}

	public void stop() {
		connectionLock.lock();
		try {
			if (running) {
				running = false;
				commandRunner.exec(cmdStopPackLine);
			}
		} finally {
			connectionLock.unlock();
		}
	}

	public void addParcelToQueue(String parcelId) {
		if (parcelId == null || parcelId.length() == 0) {
			return;
		}

		connectionLock.lock();
		try {
			// Add parcel to list
			parcels.add(new Parcel(parcelId));
			logger.debug(String.format("Added parcel id = %s, total parcels %d", parcelId, parcels.size()));

			// Start the line, because at least one parcel is presented
			start();
		} finally {
			connectionLock.unlock();
		}
	}

	public void removeParcelFromQueue(String parcelId) {
		if (parcelId == null || parcelId.length() == 0) {
			return;
		}

		connectionLock.lock();
		try {
			// Remove parcel from list
			parcels.remove(new Parcel(parcelId));
			logger.debug(String.format("Removed parcel id = %s, total parcels %d", parcelId, parcels.size()));

			// If there are no more parcels then stop the line
			if (parcels.size() == 0) {
				logger.debug("No more parcels");
				stop();
			}
		} finally {
			connectionLock.unlock();
		}
	}

	public int getNumberOfParcels() {
		connectionLock.lock();
		try {
			return parcels.size();
		} finally {
			connectionLock.unlock();
		}
	}

	@Scheduled(fixedDelay = 1000)
	public void clearStaleParcels() {
		connectionLock.lock();
		try {
			if (parcels.size() > 0) {
				long now = System.currentTimeMillis() - staleTimeout;

				// Remove stale parcels
				Iterator<Parcel> iterator = parcels.iterator();
				while (iterator.hasNext()) {
					Parcel parcel = iterator.next();
					if (parcel.getTimestamp() < now) {
						iterator.remove();
						logger.debug(String.format("Removed stale parcel id = %s", parcel.getId()));
					}
				}

				// If there are no more parcels then stop the line
				if (parcels.size() == 0) {
					logger.debug("No more parcels");
					stop();
				}
			}
		} finally {
			connectionLock.unlock();
		}
	}
}
