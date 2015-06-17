package com.example;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Ping N web sites in parallel. The ping simply does a GET, and looks at the
 * first header line. This example could be applied to many sorts of similar
 * tasks.
 * <P>
 * No time-out is used here. As usual, be wary of warm-up of the just-in-time
 * compiler. You might want to use -Xint.
 * <p>
 * From http://www.javapractices.com/topic/TopicAction.do?Id=247
 * <p>
 * @author <a href="mailto:dan@danbecker.info">Dan Becker </a>.
 */
public final class ParallelSite {
	/** Maximum thread for parallel execution. */
	public static final int MAX_THREADS = 4;

	/** Pool of sites */
	public static final List<String> URLs = Arrays.asList(
		"http://www.youtube.com/", "http://www.google.com/",
		"http://www.date4j.net", "http://www.web4j.com",
		"http://www.ebay.com", "http://www.paypal.com",
		"http://www.apache.org", "http://www.github.com"
	);
	
	/** Run this tool. */
	public static final void main(String... aArgs) {
		ParallelSite checker = new ParallelSite();
		try {
			log("Parallel, report each as it completes:");
			checker.pingAndReportEachWhenKnown();

			log("Parallel, report all at end:");
			checker.pingAndReportAllAtEnd();

			log("Sequential, report each as it completes:");
			checker.pingAndReportSequentially();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException ex) {
			log("Problem executing worker: " + ex.getCause());
		} catch (MalformedURLException ex) {
			log("Bad URL: " + ex.getCause());
		}
		log("Done.");
	}

	/**
	 * Check N sites, in parallel, using up to MAX_THREADS
	 * <p> 
	 * Uses fixed thread executor service and completion service to
	 * report the result of each 'ping' as it comes in.
	 */
	void pingAndReportEachWhenKnown() throws InterruptedException,
			ExecutionException {
		long start = System.currentTimeMillis();
		int numThreads = URLs.size() > MAX_THREADS ? MAX_THREADS : URLs.size(); 
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CompletionService<PingResult> completionService = new ExecutorCompletionService<>(
				executor);
		for (String url : URLs) {
			Task task = new Task(url);
			completionService.submit(task);
		}
		for (String url : URLs) {
			Future<PingResult> future = completionService.take();
			log(future.get());
		}
		executor.shutdown(); // always reclaim resources
		long duration = System.currentTimeMillis() - start;
		log("Duration: " + duration + " mS");
	}

	/**
	 * Check N sites, in parallel, using up to MAX_THREADS. 
	 * <p>
	 * Uses fixed thread executor service and get.
	 * Report the result only when all have completed.
	 */
	void pingAndReportAllAtEnd() throws InterruptedException, ExecutionException {
		long start = System.currentTimeMillis();
		Collection<Callable<PingResult>> tasks = new ArrayList<>();
		for (String url : URLs) {
			tasks.add(new Task(url));
		}
		int numThreads = URLs.size() > MAX_THREADS ? MAX_THREADS : URLs.size();
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		List<Future<PingResult>> results = executor.invokeAll(tasks);
		for (Future<PingResult> result : results) {
			PingResult pingResult = result.get();
			log(pingResult);
		}
		executor.shutdown(); // always reclaim resources
		long duration = System.currentTimeMillis() - start;
		log("Duration: " + duration + " mS");
	}

	/**
	 * Check N sites, but sequentially, not in parallel. 
	 * Does not use multiple threads at all.
	 */
	void pingAndReportSequentially() throws MalformedURLException {
		long start = System.currentTimeMillis();
		for (String url : URLs) {
			PingResult pingResult = pingAndReportStatus(url);
			log(pingResult);
		}
		long duration = System.currentTimeMillis() - start;
		log("Duration: " + duration + " mS");
	}

	// PRIVATE
	private static void log(Object aMsg) {
		System.out.println(String.valueOf(aMsg));
	}

	/** Try to ping a URL. Return true only if successful. */
	private final class Task implements Callable<PingResult> {
		Task(String aURL) {
			fURL = aURL;
		}

		/** Access a URL, and see if you get a healthy response. */
		@Override
		public PingResult call() throws Exception {
			return pingAndReportStatus(fURL);
		}

		private final String fURL;
	}

	
	/** Open connection to given URL. Return Result. */
	private PingResult pingAndReportStatus(String aURL)
			throws MalformedURLException {
		PingResult result = new PingResult();
		result.url = aURL;
		long start = System.currentTimeMillis();
		URL url = new URL(aURL);
		try {
			URLConnection connection = url.openConnection();
			int FIRST_LINE = 0;
			String firstLine = connection.getHeaderField(FIRST_LINE);
			result.success = true;
			long end = System.currentTimeMillis();
			result.timing = end - start;
		} catch (IOException ex) {
			// ignore - fails
		}
		return result;
	}

	/** Simple struct to hold all the date related to a ping. */
	private static final class PingResult {
		String url;
		Boolean success;
		Long timing;

		@Override
		public String toString() {
			return "   Result:" + success + " " + timing + " msecs " + url;
		}
	}
}