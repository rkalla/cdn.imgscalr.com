package com.imgscalr.cdn;

import static com.imgscalr.cdn.Constants.*;
import static javax.servlet.http.HttpServletResponse.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NewCDNServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void service(final ServletRequest sRequest,
			final ServletResponse sResponse) throws ServletException,
			IOException {
		if (!(sRequest instanceof HttpServletRequest))
			throw new ServletException("non-HTTP request");
		if (!(sResponse instanceof HttpServletResponse))
			throw new ServletException("non-HTTP response");

		final HttpServletRequest request = (HttpServletRequest) sRequest;
		final HttpServletResponse response = (HttpServletResponse) sResponse;

		try {
			checkMethod(request, response);
			processRequest(request, response);
		} catch (CDNResponseException ex) {
			try {
				// Render the response according to the HTTP status code.
				switch (ex.httpCode) {
				case 200:
					break;

				default:
					response.sendError(ex.httpCode, ex.message);
					break;
				}

				// Response was rendered, flush buffer and complete response.
				response.flushBuffer();
			} catch (Exception e) {
				// Client aborted connection, do nothing.
			}
		} catch (IOException ex) {
			// Client aborted connection, do nothing.
		}

	}

	private static void checkMethod(HttpServletRequest request,
			HttpServletResponse response) throws IOException,
			CDNResponseException {
		if (!"GET".equals(request.getMethod()))
			throw new CDNResponseException(SC_METHOD_NOT_ALLOWED,
					"HTTP Method '" + request.getMethod()
							+ "' is not supported by this service.");
	}

	private static void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws CDNResponseException {
		/*
		 * DISTRO NAME
		 */
		final String distroName;
		final int idx = request.getServerName().indexOf('.');

		if (idx > -1)
			distroName = request.getServerName().substring(0, idx);
		else
			throw new CDNResponseException(
					SC_BAD_REQUEST,
					"Request is missing a valid distro name, e.g. http://<distro_name>.cdn.imgscalr.com/...");

		/*
		 * QUERY STRING
		 */
		final String queryString = request.getQueryString();
		final String encodedQueryString;

		if (queryString == null || queryString.isEmpty())
			encodedQueryString = null;
		else {
			try {
				encodedQueryString = URLEncoder.encode(queryString, "UTF-8");
			} catch (UnsupportedEncodingException ex) {
				throw new CDNResponseException(SC_BAD_REQUEST,
						"Server is unable to process the provided Query String: '"
								+ queryString + "'");
			}
		}

		/*
		 * FILE NAME
		 */
		final String pathInfo = request.getPathInfo();

		if (pathInfo == null || pathInfo.isEmpty())
			throw new CDNResponseException(SC_BAD_REQUEST,
					"Request is missing a valid file name reference.");

		final int extIdx = pathInfo.lastIndexOf('.');

		if (extIdx == -1 || extIdx + 1 == pathInfo.length())
			throw new CDNResponseException(SC_BAD_REQUEST,
					"Request is missing a valid, complete file name reference: '"
							+ pathInfo + "'");

		final String fileStem = pathInfo.substring(0, extIdx);
		final String fileExt = pathInfo.substring(extIdx + 1);

		/*
		 * Create references to the two different types of files we will have on
		 * this server: the fully processed result which includes the encoded
		 * query string OR the original unprocessed one from S3 that we can
		 * process here locally for a new result. It is possible these are the
		 * same file if there is no query string provided (just a raw image
		 * reference).
		 */
		final Path cachedProcImage = TMP_DIR.resolve(fileStem
				+ (encodedQueryString == null ? "" : "-" + encodedQueryString)
				+ '.' + fileExt);
		final Path cachedOrigImage = TMP_DIR.resolve(pathInfo);

		// Render immediately if cached processed image already exists.
		if (Files.exists(cachedProcImage)) {
			if (Files.isReadable(cachedProcImage))
				throw new CDNResponseException(cachedProcImage);
			else
				throw new CDNResponseException(SC_INTERNAL_SERVER_ERROR,
						"Server is unable to read the image from the local filesystem.");
		}

		// Process then render if the original is available.
		if (Files.exists(cachedOrigImage)) {
			Path image = processImage(cachedOrigImage);

			if (Files.isReadable(image))
				throw new CDNResponseException(image);
			else
				throw new CDNResponseException(SC_INTERNAL_SERVER_ERROR,
						"Server is unable to read the image from the local filesystem.");
		} else {
			// Download, then process, then render.
		}

		/*
		 * TODO: This is a cascading operation that reaches out farther and
		 * farther if needed. This should be designed in a fluid way, maybe
		 * calling out to a separate method?
		 */
	}

	private static Path processImage(Path origImage)
			throws CDNResponseException {
		// TODO: impl
		return null;
	}
}