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

import com.imgscalr.cdn.CDNResponse;

public class OriginPullTask implements Callable<CDNResponse> {
	private Path targetFile;

	private String distroName;
	private String originPath;

	public OriginPullTask(Path targetFile, String distroName, String originPath) {
		System.out.println("OPull [targetFile=" + targetFile + ", distroName="
				+ distroName + ", originPath=" + originPath + "]");

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
		System.out.println("Attemping to download [" + ORIGIN_HREF + originPath
				+ "]");

		CDNResponse response = null;
		InputStream originStream = null;
		URL originURL = new URL(ORIGIN_HREF + distroName + originPath);

		try {
			originStream = originURL.openStream();
		} catch (IOException e) {
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
				fc.transferFrom(fc, 0, Integer.MAX_VALUE);

				// Cleanup.
				fc.close();
				bc.close();
				originStream.close();
			} catch (Exception e) {
				response = new CDNResponse(SC_INTERNAL_SERVER_ERROR,
						"A server error occurred while trying to origin-pull the requested image '"
								+ originPath + "' from the '" + distroName
								+ "' distribution.");
			}
		}

		return response;
	}
}