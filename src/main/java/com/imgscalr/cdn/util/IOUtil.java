package com.imgscalr.cdn.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {
	public static int copy(File from, OutputStream to) throws IOException {
		int total = 0;

		try (InputStream inStream = new FileInputStream(from)) {
			total = copy(inStream, to);
		}

		return total;
	}

	public static int copy(InputStream from, File to) throws IOException {
		int total = 0;

		try (OutputStream toStream = new FileOutputStream(to)) {
			total = copy(from, toStream);
		}

		return total;
	}

	public static int copy(InputStream from, OutputStream to)
			throws IOException {
		int read = 0;
		int total = 0;
		byte[] buffer = new byte[8192];

		while ((read = from.read(buffer)) != -1) {
			// Stream bytes to output.
			to.write(buffer, 0, read);

			// Update total written.
			total += read;
		}

		return total;
	}
}