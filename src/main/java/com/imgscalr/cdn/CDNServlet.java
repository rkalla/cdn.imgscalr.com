package com.imgscalr.cdn;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
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
			throw new ServletException(
					"Non-HTTP request received; this service only supports HTTP-based operations.");
		if (!(sResponse instanceof HttpServletResponse))
			throw new ServletException(
					"Non-HTTP response received; this service only supports HTTP-based operations.");

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
			processRequest(request);
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

	private static void processRequest(HttpServletRequest servletRequest)
			throws CDNResponse {
		CDNRequest request = new CDNRequest(servletRequest);
		L.debug("CDN Request: {}", request);

		/*
		 * If there is no processedImage and no originalImage, then we need an
		 * origin-pull. This is the worst-case scenario.
		 */
		if (!Files.exists(request.processedImage)
				&& !Files.exists(request.originalImage)) {
			L.trace("originalImage and processedImage do not exist, initiating an origin-pull...");
			CDNResponse pullResponse = null;

			try {
				pullResponse = EXEC_SERVICE.submit(
						new OriginPullTask(request.originalImage,
								request.distroName, request.fileName)).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				pullResponse = new CDNResponse(SC_INTERNAL_SERVER_ERROR,
						"A server error occurred while performing the origin-pull operation.");
			}

			/*
			 * If the pullResponse is not null, that means a more detailed
			 * failure happened during the OriginPullTask (e.g. like the image
			 * not existing) or a more low-level system exception occurred that
			 * caused the operation to abort or fail; either way it is a
			 * internal server error back to the client and we need to throw it.
			 * 
			 * If there is no response, then the operation succeeded and we can
			 * process the image.
			 */
			if (pullResponse != null)
				throw pullResponse;
		}

		/*
		 * If there is no processedImage, then we need to process the
		 * originalImage to get on. The previous condition guarantees that the
		 * originalImage will exist by this point (either by already existing on
		 * the server, or we just origin-pulled it).
		 */
		if (!Files.exists(request.processedImage)) {
			L.trace("processedImage does not exist, creating it...");
			createProcessedImage(request.originalImage, request.queryString,
					request.processedImage);
		}

		/*
		 * At this point, whether we had to origin-pull the image or process the
		 * original, we are guaranteed to have a processedImage that is ready to
		 * render.
		 */
		if (Files.isReadable(request.processedImage))
			throw new CDNResponse(request.processedImage, request.mimeType);
		else
			throw new CDNResponse(
					SC_INTERNAL_SERVER_ERROR,
					"Server is unable to read the image from the local filesystem and render it to the client.");
	}

	private static void createProcessedImage(Path originalImage,
			String queryString, Path processedImage) throws CDNResponse {
		L.debug("processImage [originalImage={}, queryString={}]",
				originalImage, queryString);

		/*
		 * Do nothing and return immediately if there is no query string; then
		 * originalImage and processedImage point at the same file.
		 */
		if (queryString == null || queryString.isEmpty())
			return;

		// TODO: Implement original -> processed image processing (parsing the
		// query string).
	}
}