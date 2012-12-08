package com.imgscalr.cdn;

import static com.imgscalr.cdn.Constants.MIME_MAP;
import static com.imgscalr.cdn.Constants.TMP_DIR;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

public class CDNRequest {
	public final String distroName;

	public final String fileStem = null;
	public final String fileExt = null;
	public final String fileName = null;
	public final String fileMimeType = null;

	public final String queryString;
	public final String encQueryString;

	public final Path imagePath;

	public CDNRequest(HttpServletRequest request) throws CDNResponse {
		distroName = parseDistroName(request);

		if (distroName == null)
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"Request did not include a distribution name. e.g. 'http://<distro_name>.cdn.imgscalr.com/...'");

		String pathInfo = request.getPathInfo();

		if (pathInfo == null || pathInfo.isEmpty())
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"Request did not include a valid, complete file name. e.g. 'http://<distro_name>.cdn.imgscalr.com/<file_name>'");

		try {
			imagePath = TMP_DIR.resolve(pathInfo);
		} catch (Exception e) {
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"File name specified contained invalid characters and could not be handled as a valid file request: '"
							+ pathInfo + "'");
		}

		// else {
		// // We are case-sensitive, so no normalization of the file name.
		// fileStem = fileNameSegments[0];
		// fileExt = fileNameSegments[1];
		// fileName = fileStem + '.' + fileExt;
		// }

		// fileMimeType = MIME_MAP.get(fileExt);
		//
		// if (fileMimeType == null)
		// throw new CDNResponse(
		// SC_BAD_REQUEST,
		// "Server was unable to determine MIME type based on the extension for the requested file: "
		// + fileName);

		queryString = request.getQueryString();

		// Prepare the URLEncoded version of the query string.
		if (queryString == null || queryString.isEmpty())
			encQueryString = null;
		else {
			try {
				encQueryString = URLEncoder.encode(queryString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new CDNResponse(SC_BAD_REQUEST,
						"Server was unable to process provided query string: '"
								+ queryString + "'");
			}
		}

		/*
		 * Cheap operations: create Path references to the two cached copies of
		 * the fully processed image or the original that was pulled from S3 to
		 * this scaling instance (NOTE: These might be the same path).
		 * 
		 * We will determine existence/accessibility of these resources later in
		 * the processing.
		 * 
		 * Format: [distro_name]-[file_stem](-[encoded_query_string]).[file_ext]
		 */
		try {
			// origImage = TMP_DIR.resolve(distroName + '-' + fileName);
			// cachedImage = TMP_DIR.resolve(distroName + '-' + fileStem
			// + (encQueryString == null ? "" : '-' + encQueryString)
			// + '.' + fileExt);
		} catch (Exception e) {
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"Server was unable to try and reference the cached copy of the image on the file system.");
		}

	}

	// @Override
	// public String toString() {
	// return CDNRequest.class.getName() + "[distroName=" + distroName
	// + ", fileName=" + fileName + ", queryString=" + queryString
	// + ", tmpFile=" + cachedImage + ", cached="
	// + Files.exists(cachedImage) + "]";
	// }

	private static String parseDistroName(HttpServletRequest request) {
		String result = null;

		if (request != null) {
			String serverName = request.getServerName();

			if (serverName != null && !serverName.isEmpty()) {
				int i = serverName.indexOf('.');

				if (i > -1) {
					// The first segment of the CNAME is our distribution name.
					result = serverName.substring(0, i);
				}
			}
		}

		return result;
	}

	private static String encodeQueryString(String queryString) {
		String result = null;

		if (queryString != null && !queryString.isEmpty()) {
			try {
				result = URLEncoder.encode(queryString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new CDNResponse(
						SC_BAD_REQUEST,
						"Server was unable to URL-encode provided query string (used to identify cached copy of image): '"
								+ queryString + "'");
			}
		}

		return result;
	}
}