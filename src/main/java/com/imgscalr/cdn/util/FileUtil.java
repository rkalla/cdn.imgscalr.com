package com.imgscalr.cdn.util;

import java.util.HashMap;
import java.util.Map;

public class FileUtil {
	private static final Map<String, String> MIME_MAP = new HashMap<String, String>(
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

	public static String getMimeType(String ext) {
		String result = null;

		if (ext != null && !ext.isEmpty()) {
			result = MIME_MAP.get(ext.toLowerCase());
		}

		return result;
	}
}
