package com.imgscalr.cdn;

import static com.imgscalr.cdn.Constants.*;
import static com.imgscalr.cdn.util.RequestUtil.parseDistroName;
import static com.imgscalr.cdn.util.RequestUtil.parseFileName;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;

public class CDNServletRequest {
	public final String fileStem;
	public final String fileExt;
	public final String fileName;
	public final String mimeType;
	public final String distroName;
	public final String queryString;

	public final boolean cached;
	public final File tmpFile;

	public CDNServletRequest(HttpServletRequest req) throws CDNServletResponse {
		String[] fileSegments = parseFileName(req);

		if (fileSegments == null)
			throw new CDNServletResponse(SC_BAD_REQUEST,
					"Request did not include a valid, complete file name. e.g. 'myfile.jpg'");
		else {
			fileStem = fileSegments[0];
			fileExt = fileSegments[1];
			fileName = fileStem + '.' + fileExt;
		}

		mimeType = MIME_MAP.get(fileExt);

		if (mimeType == null)
			throw new CDNServletResponse(
					SC_BAD_REQUEST,
					"Server was unable to determine MIME type based on the extension for the requested file: "
							+ fileName);

		distroName = parseDistroName(req);

		if (distroName == null)
			throw new CDNServletResponse(
					SC_BAD_REQUEST,
					"Request did not include a distribution name. e.g. http://[DISTRO].cdn.imgscalr.com/...");

		queryString = req.getQueryString();

		try {
			/*
			 * Try and create the FD pointing at the temporary file that either
			 * already exists or we need to download from S3 into this target.
			 * 
			 * Format:
			 * [DISTRO_NAME]-[FILE_NAME_ROOT](-[QUERY_STRING_ENCODED]).[FILE_EXT
			 * ]
			 */
			tmpFile = new File(TMP_DIR, distroName
					+ '-'
					+ fileStem
					+ (queryString == null ? "" : '-' + URLEncoder.encode(
							queryString, "UTF-8")) + '.' + fileExt);
		} catch (UnsupportedEncodingException e) {
			throw new CDNServletResponse(
					SC_BAD_REQUEST,
					"Server was unable to URL-encode the provided query string ["
							+ queryString
							+ "]. Please ensure provided query string is valid.");
		}

		if (tmpFile.exists()) {
			if (tmpFile.canRead())
				cached = true;
			else
				throw new CDNServletResponse(SC_INTERNAL_SERVER_ERROR,
						"Server was unable to read edge-cached image from the local filesystem.");
		} else {
			cached = false;

			try {
				if (!tmpFile.createNewFile())
					throw new CDNServletResponse(SC_INTERNAL_SERVER_ERROR,
							"Server was unable to create edge-cache of file on the local filesystem.");

				/*
				 * If we got here, then the temporary file was successfully
				 * created and can be written to. All processing can proceed
				 * successfully.
				 */
			} catch (IOException e) {
				throw new CDNServletResponse(
						SC_BAD_REQUEST,
						"Server was unable to create edge-cache of requested file '"
								+ fileName
								+ "' on local filesystem due to an invalid file name.");
			}
		}
	}

	@Override
	public String toString() {
		return CDNServletRequest.class.getName() + "[distroName=" + distroName
				+ ", fileName=" + fileName + ", queryString=" + queryString
				+ ", tmpFile=" + tmpFile.getAbsolutePath() + ", cached="
				+ cached + "]";
	}
}