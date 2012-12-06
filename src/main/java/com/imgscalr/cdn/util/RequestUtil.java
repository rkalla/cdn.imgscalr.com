package com.imgscalr.cdn.util;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {
	public static String parseDistroName(HttpServletRequest req) {
		String result = null;

		if (req != null) {
			String serverName = req.getServerName();

			if (serverName != null && !serverName.isEmpty()) {
				int i = serverName.indexOf('.');

				if (i > -1)
					result = serverName.substring(0, i);
			}
		}

		return result;
	}

	public static String[] parseFileName(HttpServletRequest req) {
		String result[] = null;

		if (req != null) {
			String requestURI = req.getRequestURI();

			if (requestURI != null && requestURI.length() > 0) {
				// Find last path file separator to start from.
				int sIdx = requestURI.lastIndexOf('/') + 1;

				// Ensure there is a filename after the last '/' char.
				if (sIdx > 0 && sIdx < requestURI.length()) {
					// Find the file extension.
					int eIdx = requestURI.indexOf('.', sIdx);

					// Ensure there is a file extension.
					if (eIdx > -1 && eIdx < requestURI.length() - 1) {
						// Only create the result now we confirmed all exists.
						result = new String[2];

						// Parse the file name (minus extension).
						result[0] = requestURI.substring(sIdx, eIdx);
						// Parse the remainder of the file name as the
						// extension.
						result[1] = requestURI.substring(eIdx + 1,
								requestURI.length());
					}
				}
			}
		}

		return result;
	}
}