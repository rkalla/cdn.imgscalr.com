package com.imgscalr.cdn;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Constants {
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
	}
}