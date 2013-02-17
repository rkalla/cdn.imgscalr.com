package com.imgscalr.cdn;

import static com.imgscalr.cdn.Constants.TMP_DIR;
import static com.imgscalr.cdn.util.FileUtil.determineMimeType;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

public class CDNRequest {
	public final String distroName;

	public final String queryString;
	public final String encodedQueryString;

	public final String fileName;
	public final String fileStem;
	public final String fileExt;

	public final Path originalImage;
	public final Path processedImage;

	public final String mimeType;

	public CDNRequest(HttpServletRequest request)
			throws IllegalArgumentException, CDNResponse {
		if (request == null)
			throw new IllegalArgumentException("request cannot be null");

		/*
		 * Parse: Distro Name
		 */
		final int idx = request.getServerName().indexOf('.');

		if (idx > -1)
			distroName = request.getServerName().substring(0, idx);
		else
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"Request is missing a valid distro name, e.g. http://<distro_name>.cdn.imgscalr.com/...");

		/*
		 * Parse: Query String
		 */
		queryString = request.getQueryString();

		if (queryString == null || queryString.isEmpty())
			encodedQueryString = null;
		else {
			try {
				encodedQueryString = URLEncoder.encode(queryString, "UTF-8");
			} catch (UnsupportedEncodingException ex) {
				throw new CDNResponse(SC_BAD_REQUEST,
						"Server is unable to process the query string: '"
								+ queryString + "'");
			}
		}

		/*
		 * Parse: File Name (stem and extension)
		 */
		fileName = request.getPathInfo();

		if (fileName == null || fileName.isEmpty())
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"Request is missing a valid file name, e.g. http://mydistro.cdn.imgscalr.com/<file name>");

		final int extIdx = fileName.lastIndexOf('.');

		if (extIdx == -1 || extIdx + 1 == fileName.length())
			throw new CDNResponse(SC_BAD_REQUEST,
					"Request is missing a valid (complete) file name reference: '"
							+ fileName + "'");

		fileStem = fileName.substring(0, extIdx);
		fileExt = fileName.substring(extIdx + 1);

		/*
		 * Created references to the two potential types of files we will have
		 * on a CDN edge server: the fully processed image if we have requested
		 * it before or the original version of the image pulled from the origin
		 * if we haven't processed it yet (or if a different set of query string
		 * args is passed and we are created a slightly different version of the
		 * same original image).
		 * 
		 * Additionally, we might not have either image on the server yet (which
		 * is fine, it will cause an origin pull).
		 * 
		 * If there is no query string, then these values are the same and this
		 * is just a raw image reference.
		 */
		originalImage = Paths.get(TMP_DIR, fileName);
		processedImage = Paths.get(TMP_DIR, fileStem
				+ (encodedQueryString == null ? "" : "-" + encodedQueryString)
				+ '.' + fileExt);

		mimeType = determineMimeType(originalImage);
	}

	@Override
	public String toString() {
		return CDNRequest.class.getName() + "[distroName=" + distroName
				+ ", queryString='" + queryString + "', fileName=" + fileName
				+ ", originalImage=" + originalImage + ", processedImage="
				+ processedImage + "]";
	}
}