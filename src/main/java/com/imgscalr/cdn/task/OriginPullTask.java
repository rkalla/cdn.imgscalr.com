package com.imgscalr.cdn.task;

import static com.imgscalr.cdn.Constants.ORIGIN_HREF;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imgscalr.cdn.CDNResponse;

public class OriginPullTask implements Callable<CDNResponse> {
	private static final Logger L = LoggerFactory
			.getLogger(OriginPullTask.class);

	private Path targetFile;

	private String distroName;
	private String originPath;

	public OriginPullTask(Path targetFile, String distroName, String originPath) {
		this.targetFile = targetFile;
		this.distroName = distroName;
		this.originPath = originPath;
	}

	/**
	 * Executes the origin-pull of the image from the S3 repository. Returns an
	 * {@link CDNResponse} instance ONLY if there was an error during processing
	 * so a more meaningful exception can be sent back to the client (instead of
	 * trying to handle the {@link ExecutionException} on the caller's side and
	 * pull out the real cause.
	 * 
	 * If the origin pull is successful then <code>null</code> is returned so
	 * the caller can continue processing the image before sending back to the
	 * caller.
	 */
	@Override
	public CDNResponse call() throws Exception {
		long sTime = System.currentTimeMillis();
		String downloadURL = ORIGIN_HREF + distroName + originPath;
		L.trace("Attempting to download [{}] to local file [{}]...",
				downloadURL, targetFile);

		CDNResponse response = null;
		InputStream originStream = null;
		URL originURL = new URL(downloadURL);

		try {
			originStream = originURL.openStream();
		} catch (IOException e) {
			L.trace("\tRequested file ({}) does not exist.", downloadURL);
			response = new CDNResponse(SC_NOT_FOUND, "Requested image '"
					+ originPath + "' does not exist in the '" + distroName
					+ "' distribution.");
		}

		/*
		 * If no response was created yet, then the file exists and the
		 * InputStream to it was created successfully, so now attempt to stream
		 * it back to our target file.
		 */
		if (response == null) {
			try {
				ReadableByteChannel bc = Channels.newChannel(originStream);
				FileChannel fc = FileChannel.open(targetFile,
						StandardOpenOption.CREATE, StandardOpenOption.WRITE);

				/*
				 * Very efficient transfer directly from the open HTTP
				 * connection to the file we have open for writing.
				 */
				long size = fc.transferFrom(bc, 0, Integer.MAX_VALUE);

				// Cleanup.
				fc.close();
				bc.close();
				originStream.close();

				// Calculate rate
				long eTime = System.currentTimeMillis() - sTime;
				double bpms = (double) size / (double) eTime;
				L.trace("Origin Pull Complete [bytes={}, time={} ms, rate={} KB/sec]",
						size, eTime, (float) (bpms * 1000));
			} catch (Exception e) {
				e.printStackTrace();
				response = new CDNResponse(SC_INTERNAL_SERVER_ERROR,
						"A server error occurred while trying to origin-pull the requested image '"
								+ originPath + "' from the '" + distroName
								+ "' distribution.");
			}
		}

		return response;
	}
}