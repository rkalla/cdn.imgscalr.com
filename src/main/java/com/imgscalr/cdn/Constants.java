package com.imgscalr.cdn;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.imgscalr.cdn.task.OriginPullTask;

public class Constants {
	private static final Logger L = LoggerFactory.getLogger(Constants.class);
	
	/**
	 * All internal origin-pulls are done over HTTP because the home AWS network
	 * is considered safe for inter-network communication (especially for file
	 * transfers).
	 * 
	 * It also allows us to avoid the setup cost of HTTPS connection allow the
	 * origin pulls to be as fast as possible.
	 */
	public static final String ORIGIN_HREF = "http://cdn.imgscalr.com.s3.amazonaws.com/";

	public static final Path TMP_DIR = Paths.get(System
			.getProperty("java.io.tmpdir"));

	public static final Map<String, String> MIME_MAP = new HashMap<String, String>(
			31);

	public static final AmazonS3 S3_CLIENT;

	static {
		MIME_MAP.put("gif", "image/gif");
		MIME_MAP.put("jpg", "image/jpeg");
		MIME_MAP.put("jpeg", "image/jpeg");
		MIME_MAP.put("jpe", "image/jpeg");
		MIME_MAP.put("jif", "image/jpeg");
		MIME_MAP.put("jfif", "image/jpeg");
		MIME_MAP.put("jfi", "image/jpeg");
		MIME_MAP.put("png", "image/png");
		MIME_MAP.put("tiff", "image/tiff");
		MIME_MAP.put("tif", "image/tiff");
		MIME_MAP.put("bmp", "image/bmp");

		try {
			S3_CLIENT = new AmazonS3Client(new PropertiesCredentials(
					OriginPullTask.class
							.getResourceAsStream("/aws-s3.properties")));
		} catch (Exception e) {
			String message = "Failed to init AmazonS3Client object using '/aws-s3.properties' file.";
			L.error(message, e);
			throw new RuntimeException(message, e);
		}
	}
}