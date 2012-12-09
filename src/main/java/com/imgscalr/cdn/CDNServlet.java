package com.imgscalr.cdn;

import static com.imgscalr.cdn.Constants.TMP_DIR;
import static com.imgscalr.cdn.util.FileUtil.determineMimeType;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imgscalr.cdn.task.OriginPullTask;

public class CDNServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger L = LoggerFactory.getLogger(CDNServlet.class);
	private static final ExecutorService EXEC_SERVICE = Executors
			.newCachedThreadPool();

	@Override
	public void service(final ServletRequest sRequest,
			final ServletResponse sResponse) throws ServletException,
			IOException {
		if (!(sRequest instanceof HttpServletRequest))
			throw new ServletException("non-HTTP request");
		if (!(sResponse instanceof HttpServletResponse))
			throw new ServletException("non-HTTP response");

		long sTime = System.currentTimeMillis();
		final HttpServletRequest request = (HttpServletRequest) sRequest;
		final HttpServletResponse response = (HttpServletResponse) sResponse;

		try {
			// Verify HTTP method.
			if (!"GET".equals(request.getMethod()))
				throw new CDNResponse(SC_METHOD_NOT_ALLOWED, "HTTP Method '"
						+ request.getMethod()
						+ "' is not supported by this service.");

			L.debug("HTTP GET Start [serverName={}, pathInfo={}, queryString='{}']",
					request.getServerName(), request.getPathInfo(),
					request.getQueryString());

			// Continue processing the request.
			processRequest(request, response);
		} catch (CDNResponse ex) {
			try {
				L.debug("Response Complete [time(ms)={}, response={}]",
						(System.currentTimeMillis() - sTime), ex);
				sTime = System.currentTimeMillis();

				// Render the response according to the HTTP status code.
				switch (ex.httpCode) {
				case 200:
					response.setStatus(SC_OK);
					response.setContentType(ex.mimeType);

					int size = (int) Files.size(ex.image);
					response.setContentLength(size);

					WritableByteChannel bc = Channels.newChannel(response
							.getOutputStream());
					FileChannel fc = FileChannel.open(ex.image,
							StandardOpenOption.READ);

					/*
					 * Let the OS stream the image back to the client in the
					 * most optimized way possible (transferTo) -- also let any
					 * exceptions bubble up to the outside catch as it likely
					 * symbolizes the client broke the connection before it
					 * finished.
					 */
					fc.transferTo(0, size, bc);

					// Cleanup
					fc.close();
					bc.close();

					// Calculate rate
					long eTime = System.currentTimeMillis() - sTime;
					double bpms = (double) size / (double) eTime;
					L.trace("Client Send Complete [bytes={}, time(ms)={}, rate(KB/sec)={}]",
							size, eTime, (float) (bpms * 1000));
					break;

				default:
					response.sendError(ex.httpCode, ex.message);
					break;
				}
			} catch (Exception e) {
				// Client aborted connection, do nothing.
			}
		}

		/*
		 * Whether success or failure, try and close out response stream
		 * successfully
		 */
		try {
			response.flushBuffer();
		} catch (Exception e) {
			// no-op, likely client aborted connection.
		}

		L.debug("HTTP GET End");
	}

	private static void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws CDNResponse {
		/*
		 * DISTRO NAME
		 */
		final String distroName;
		final int idx = request.getServerName().indexOf('.');

		if (idx > -1)
			distroName = request.getServerName().substring(0, idx);
		else
			throw new CDNResponse(
					SC_BAD_REQUEST,
					"Request is missing a valid distro name, e.g. http://<distro_name>.cdn.imgscalr.com/...");

		L.debug("distroName='{}'", distroName);

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
				L.debug("queryString='{}'\tencodedQueryString='{}'",
						encodedQueryString);
			} catch (UnsupportedEncodingException ex) {
				throw new CDNResponse(SC_BAD_REQUEST,
						"Server is unable to process the provided Query String: '"
								+ queryString + "'");
			}
		}

		/*
		 * FILE NAME
		 */
		final String pathInfo = request.getPathInfo();

		if (pathInfo == null || pathInfo.isEmpty())
			throw new CDNResponse(SC_BAD_REQUEST,
					"Request is missing a valid file name reference.");

		final int extIdx = pathInfo.lastIndexOf('.');

		if (extIdx == -1 || extIdx + 1 == pathInfo.length())
			throw new CDNResponse(SC_BAD_REQUEST,
					"Request is missing a valid, complete file name reference: '"
							+ pathInfo + "'");

		final String fileStem = pathInfo.substring(0, extIdx);
		final String fileExt = pathInfo.substring(extIdx + 1);

		L.debug("pathInfo='{}'\tfileStem='{}'\tfileExt='{}'", pathInfo,
				fileStem, fileExt);

		/*
		 * Create references to the two different types of files we will have on
		 * this server: the fully processed result which includes the encoded
		 * query string OR the original unprocessed one from S3 that we can
		 * process here locally for a new result. It is possible these are the
		 * same file if there is no query string provided (just a raw image
		 * reference).
		 */
		final Path cachedProcImage = Paths.get(TMP_DIR.toString(), fileStem
				+ (encodedQueryString == null ? "" : "-" + encodedQueryString)
				+ '.' + fileExt);
		final Path cachedOrigImage = Paths.get(TMP_DIR.toString(), pathInfo);
		final String mimeType = determineMimeType(cachedOrigImage);

		L.debug("originalImage={}\tprocessedImage={}\tmimeType={}",
				cachedOrigImage, cachedProcImage, mimeType);

		// Render immediately if cached processed image already exists.
		if (Files.exists(cachedProcImage)) {
			if (Files.isReadable(cachedProcImage))
				throw new CDNResponse(cachedProcImage, mimeType);
			else
				throw new CDNResponse(SC_INTERNAL_SERVER_ERROR,
						"Server is unable to read the image from the local filesystem.");
		}

		L.trace("processedImage not available, checking for originalImage...");

		// Process then render if the original is available.
		if (Files.exists(cachedOrigImage)) {
			L.trace("originalImage found!");
			Path image = processImage(cachedOrigImage, queryString);

			if (Files.isReadable(image))
				throw new CDNResponse(image, mimeType);
			else
				throw new CDNResponse(SC_INTERNAL_SERVER_ERROR,
						"Server is unable to read the image from the local filesystem.");
		} else
			L.trace("originalImage NOT found, beginning origin pull...");

		/*
		 * If we got this far then it means the cached copy wasn't on the
		 * server, the original wasn't on the server either and we need to do an
		 * origin-pull.
		 * 
		 * One potential optimization that we COULD have made here is to
		 * implement a dual-write buffering output stream that pulls from the
		 * origin and writes to both a local file AND the output stream back to
		 * the client at the same time.
		 * 
		 * This was decided against for the time being because it is not clear
		 * how much performance this will buy us in response time AND would
		 * require circumvention of the entire exception-based-flow-control
		 * design of this class.
		 * 
		 * If it is decided at a later time that this optimization IS worth it,
		 * then it can all be implemented down here independently of the
		 * flow-control and a special exception thrown to exit the service
		 * method cleanly.
		 */
		CDNResponse pullResponse = null;

		try {
			long sTime = System.currentTimeMillis();
			pullResponse = EXEC_SERVICE.submit(
					new OriginPullTask(cachedOrigImage, distroName, pathInfo))
					.get();
			L.trace("Origin pull completed in {} ms",
					(System.currentTimeMillis() - sTime));
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			pullResponse = new CDNResponse(SC_INTERNAL_SERVER_ERROR,
					"A server error occurred while performing the origin-pull operation.");
		}

		/*
		 * If the pullResponse is not null, that means a more detailed failure
		 * happened during the OriginPullTask (e.g. like the image not existing)
		 * or a more low-level system exception occurred that caused the
		 * operation to abort or fail; either way it is a internal server error
		 * back to the client and we need to throw it.
		 * 
		 * If there is no response, then the operation succeeded and we can
		 * process the image.
		 */
		if (pullResponse != null)
			throw pullResponse;

		// TODO: Now we can process and stream result.
	}

	private static Path processImage(Path image, String queryString)
			throws CDNResponse {
		L.debug("processImage [image={}, queryString={}]", image, queryString);

		if (queryString == null || queryString.isEmpty())
			return image;

		// TODO: impl TEMP
		return image;
	}
}