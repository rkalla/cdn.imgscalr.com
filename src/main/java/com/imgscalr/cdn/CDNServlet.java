package com.imgscalr.cdn;

import static com.imgscalr.cdn.util.IOUtil.copy;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
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
import com.imgscalr.cdn.task.RequestCleanupTask;

public class CDNServlet extends HttpServlet {
	private static Logger L = LoggerFactory.getLogger(CDNServlet.class);

	private static final long serialVersionUID = 1L;

	// TODO: Make this configurable via an init-param
	private static ExecutorService EXEC_SERVICE = Executors
			.newFixedThreadPool(8);

	/*
	 * Examples
	 * 
	 * http://nike.cdn.imgscalr.com/jordon12.jpg
	 * 
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?width=400
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?height=300
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?width=400&height=300
	 * http://nike
	 * .cdn.imgscalr.com/jordon12.jpg?width=400&height=300&quality=ultra
	 * &fit=height
	 * 
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?effect=antialias
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?effect=grayscale
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?effect=brighter
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?effect=brighter
	 * 
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?crop=200x200
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?crop=4x4,200x200
	 * 
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?pad=4
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?pad=4,FFBB00
	 * http://nike.cdn.imgscalr.com/jordon12.jpg?pad=4,255,225,225,100
	 */
	@Override
	public void service(final ServletRequest request,
			final ServletResponse response) throws ServletException,
			IOException {
		if (!(request instanceof HttpServletRequest))
			throw new ServletException("non-HTTP request");
		if (!(response instanceof HttpServletResponse))
			throw new ServletException("non-HTTP response");

		final long sTime = System.currentTimeMillis();
		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse res = (HttpServletResponse) response;

		try {
			L.info("{}-Begin[startTime={}]", CDNServlet.class.getName(), sTime);

			// Verify a supported HTTP method is being used.
			if (!"GET".equals(req.getMethod()))
				throw new CDNServletResponse(SC_METHOD_NOT_ALLOWED,
						"HTTP Method [" + req.getMethod()
								+ "] is not supported by this server.");

			// Parse request into usable data, detecting any errors.
			CDNServletRequest cReq = new CDNServletRequest(req);

			/*
			 * Initiate the origin-pull if the image isn't locally cached
			 * already, otherwise stream back the local file immediately.
			 */
			if (!cReq.cached) {
				res.setHeader("imgscalr-cache", "miss");
				EXEC_SERVICE.execute(new OriginPullTask(cReq));
			} else
				res.setHeader("imgscalr-cache", "hit");

			// Done processing image request, ready to render.
			throw new CDNServletResponse(cReq);
		} catch (CDNServletResponse cRes) {
			L.info("{}-End[elapsedTime={} ms, httpCode={}, message={}, cReq={}]",
					CDNServlet.class.getName(),
					(System.currentTimeMillis() - sTime), cRes.httpCode,
					cRes.message, cRes.cReq);

			if (cRes.httpCode == SC_OK) {
				// Set the remainder of headers.
				res.setStatus(SC_OK);
				res.setHeader("imgscalr-elapsedTime",
						Long.toString(System.currentTimeMillis() - sTime));
				res.setContentType(cRes.cReq.mimeType);
				res.setContentLength((int) cRes.cReq.tmpFile.length());

				// Stream file contents back to client.
				copy(cRes.cReq.tmpFile, res.getOutputStream());
			} else {
				/*
				 * Cleanup any temp files we created incase the request was not
				 * found in S3; we don't want to create a temp file locally for
				 * a file that doesn't exist in the origin.
				 */
				EXEC_SERVICE.execute(new RequestCleanupTask(cRes.cReq));

				// Render error.
				res.sendError(cRes.httpCode, cRes.message);
			}
		} finally {
			try {
				// Safely close the client OutputStream.
				res.getOutputStream().flush();
				res.getOutputStream().close();
			} catch (Exception e) {
				L.error("{}-Ex[Unable to safely flush/close client OutpuStream]");
			}
		}
	}
}
