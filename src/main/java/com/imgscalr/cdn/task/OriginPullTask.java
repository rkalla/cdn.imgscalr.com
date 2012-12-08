package com.imgscalr.cdn.task;

import static com.imgscalr.cdn.Constants.ORIGIN_HREF;
import static javax.servlet.http.HttpServletResponse.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import com.imgscalr.cdn.CDNResponse;

public class OriginPullTask implements Callable<CDNResponse> {
	private Path targetFile;
	private String mimeType;

	private String distroName;
	private String originPath;

	public OriginPullTask(Path targetFile, String mimeType, String distroName,
			String originPath) {
		System.out.println("OPull [targetFile=" + targetFile + ", mimeType="
				+ mimeType + ", distroName=" + distroName + ", originPath="
				+ originPath + "]");

		this.targetFile = targetFile;
		this.mimeType = mimeType;
		this.distroName = distroName;
		this.originPath = originPath;
	}

	/*
	 * TODO: Does it make sense for this method to return anything? It is just
	 * writing to the target file that was passed in. Maybe a boolean to
	 * indicate success or failure? It should probably throw a flow-control
	 * exception if the image doesn't exist or something or maybe it should
	 * return the exception from the call with the correct HTTP status and
	 * message?
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

				response = new CDNResponse(targetFile, mimeType);
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