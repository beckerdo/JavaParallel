package com.example;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
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
import java.util.concurrent.TimeUnit;
import java.util.Random;

/**
 * Ping N web sites in parallel. The ping simply does a GET.
 * <p>
 * All sites are HTTP. Respone codes are checked.
 * Response code 500 will cause pingAndReportEachWhenKnownTerminateOnFail to
 * call ExecutorService.shutdownNow. 
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
	public static final int MAX_THREADS = 8;

	/** Pool of sites */
	public static final List<String> URLs = Arrays.asList(
		"http://www.youtube.com/", "http://www.google.com/",
		"http://www.date4j.net", "http://www.web4j.com",
		"http://www.ebay.com", "http://www.paypal.com",
		"http://www.apache.org", "http://www.github.com",
		"http://www.facebook.com", "http://www.danbecker.info",
		"http://www.slashdot.org", "http://www.cnn.com",
		"http://www.boingboing.net/", "http://www.tomshardware.com/",
		"http://www.scientificamerican.com/", "http://www.nytimes.com/"
	);
	public static final Random random = new Random();
	
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

			log("Parallel, terminate when one breaks:");
			checker.pingAndReportEachWhenKnownTerminateOnFail();
		} catch (InterruptedException ex) {
			log("Interruption occured: " + ex.getCause());
			// Thread.currentThread().interrupt();
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
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		CompletionService<PingResult> completionService = 
			new ExecutorCompletionService<>(executorService);
		for (String url : URLs) {
			Task task = new Task(url);
			completionService.submit(task);
		}
		for (String url : URLs) {
			Future<PingResult> future = completionService.take();
			log(future.get());
		}
		executorService.shutdown(); // always reclaim resources
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
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		List<Future<PingResult>> results = executorService.invokeAll(tasks);
		for (Future<PingResult> result : results) {
			PingResult pingResult = result.get();
			log(pingResult);
		}
		executorService.shutdown(); // always reclaim resources
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

	/**
	 * Check N sites, in parallel, using up to MAX_THREADS
	 * <p> 
	 * Uses fixed thread executor service and completion service to
	 * report the result of each 'ping' as it comes in.
	 * <p>
	 * A random URL failure is injected (mis-spelled URL).
	 * The task will fail when this is encountered.
	 */
	void pingAndReportEachWhenKnownTerminateOnFail() throws InterruptedException,
			ExecutionException {
		long start = System.currentTimeMillis();
		int numThreads = URLs.size() > MAX_THREADS ? MAX_THREADS : URLs.size(); 
		ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
		CompletionService<PingResult> completionService = 
			new ExecutorCompletionService<>(executorService);
		// int brokenURL = random.nextInt( URLs.size( ) / 2 );
		for (int i = 0; i < URLs.size(); i++) {
			String urlString = URLs.get(i);
			if ( i == 0 ) {
				// Make one URL broken
				urlString = breakString( 10, 3, urlString );
			}
			Task task = new Task(urlString);
			completionService.submit(task);
		}
		for (String url : URLs) {
			Future<PingResult> future = completionService.take();
			PingResult result = future.get();
			log(result);
			if ( !result.success ) {
				// List<Runnable> waiting = executor.shutdownNow(); // Interrupts all threads. Leaves a zombie.
				// log( "Shutdown called. Waiting task count=" + waiting.size());
				// executor.shutdown(); // Waits for threads to complete
				log( "Shutdown called." );
				shutdownAndAwaitTermination( executorService, 4 );
			}
		}
		long duration = System.currentTimeMillis() - start;
		log("Duration: " + duration + " mS");
	}

	/** The following method shuts down an ExecutorService in two phases, 
	 * first by calling shutdown to reject incoming tasks, 
	 * and then calling shutdownNow, if necessary, to cancel any lingering tasks: 
	 * @param pool
	 */
	public void shutdownAndAwaitTermination(ExecutorService pool, long delaySecs) {
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(delaySecs, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(delaySecs, TimeUnit.SECONDS))
					log("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
	
	// PRIVATE
	// Insert N random breaks in String.
	private String breakString( int prefix, int N, String original ) {
		char [] chars = original.toCharArray();
		for ( int j = 0; j < N; j++ ) {
			int breakChar = prefix + random.nextInt( chars.length - prefix );
			chars[ breakChar ] = 'Z';
		}
		return new String( chars );
	}
	
	private static void log(Object aMsg) {
		System.out.println(String.valueOf(aMsg));
	}

	/** Try to ping a URL. Return true if successful. */
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
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			// log( "   response code=" + responseCode );
			// int FIRST_LINE = 0;
			// log( "   content length=" + connection.getContentLength());
			// String firstLine = connection.getHeaderField(FIRST_LINE);
			if ( responseCode < 500 ) {
				result.success = true;
			} else {
				result.success = false;
			}
		} catch (Exception ex) {
			// ignore - fails
			result.success = false;
			// log( "   Exception: " + url + " " + ex);		
		}
		long end = System.currentTimeMillis();
		result.timing = end - start;
		return result;
	}

	/** Simple struct to hold all the data related to a ping. */
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