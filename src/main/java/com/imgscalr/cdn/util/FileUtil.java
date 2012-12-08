package com.imgscalr.cdn.util;

import java.nio.file.Path;
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

	public static String determineMimeType(Path file) {
		String result = null;

		/*
		 * Ensure path exists; we don't care if the file exists or is readable
		 * or not, we presume the caller has vetted the file up to this point
		 * before calling this method.
		 */
		if (file != null) {
			Path fileNamePath = file.getFileName();

			// Ensure the last segment of file path exists.
			if (fileNamePath != null) {
				String fileName = fileNamePath.toString();
				int idx = fileName.lastIndexOf('.');

				// Ensure there is an extension separator and chars after it.
				if (idx > -1 && idx < fileName.length() - 1)
					result = MIME_MAP.get(fileName.substring(idx + 1)
							.toLowerCase());
			}
		}

		return result;
	}
}
