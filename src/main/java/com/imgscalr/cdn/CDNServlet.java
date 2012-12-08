package com.imgscalr.cdn;

import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class CDNServlet extends HttpServlet {
	private static Logger L = LoggerFactory.getLogger(CDNServlet.class);

	private static final long serialVersionUID = 1L;

	// TODO: Make this configurable via an init-param
	private static ExecutorService EXEC_SERVICE = Executors
			.newFixedThreadPool(8);

	public static void main(String[] args) {
		Path p = Paths.get("C:\\A\\B\\C\\D\\name.txt");
		try {
			// Files.createDirectories(p.getParent());

			System.out.println("File Name: " + p.getFileName());
			System.out.println("Root: " + p.getRoot());

			for (int i = 0, c = p.getNameCount(); i < c; i++)
				System.out.println("\t" + i + ": '" + p.getName(i) + "'");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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
	/**
	 * Overridden to avoid all the delegation calls inside of the default
	 * HttpService impl; just grab the request/response, attempt to cast them
	 * and process the only method this service supports (GET) otherwise throw
	 * an exception or return a valid HTTP error.
	 * 
	 * The information we want from the request is as follows:
	 * http(s)://<distro_name
	 * >.cdn.imgscalr.com/<file_stem>.<file_ext>(?<query_string_ops>)
	 */
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
			// Verify a supported HTTP method is being used.
			if (!"GET".equals(request.getMethod()))
				throw new CDNResponse(SC_METHOD_NOT_ALLOWED, "HTTP Method ["
						+ request.getMethod()
						+ "] is not supported by this server.");

			/*
			 * TODO: Debug
			 * 
			 * Context Path: /cdn Server Name: abc.cdn.imgscalr.com Servlet
			 * Path: Path Info: /bob/aws2.png Request URI: /cdn/bob/aws2.png
			 * Query String: crop=x20,y30,width140,height240&resize
			 * =methodQUALITY,mode=FIT_TO_WIDTH,w20,h40
			 */
			System.out.println("Context Path: " + request.getContextPath());
			System.out.println("Server Name: " + request.getServerName());
			System.out.println("Servlet Path: " + request.getServletPath());
			System.out.println("Path Info: " + request.getPathInfo());
			System.out.println("Request URI: " + request.getRequestURI());
			System.out.println("Query String: " + request.getQueryString());
			System.out.println("Enc Query String: "
					+ URLEncoder.encode(request.getQueryString(), "UTF-8"));

			processRequest(new CDNRequest(request));
		} catch (CDNResponse cr) {
			response.setStatus(cr.httpCode);

			// L.info("{}-End[elapsedTime={} ms, httpCode={}, message={}, cReq={}]",
			// CDNServlet.class.getName(),
			// (System.currentTimeMillis() - sTime), cRes.httpCode,
			// cRes.message, cRes.cReq);

			if (cr.httpCode == SC_OK) {
				// response.setHeader("imgscalr-elapsedTime",
				// Long.toString(System.currentTimeMillis() - sTime));
				response.setContentType(cr.cReq.fileMimeType);
				// response.setContentLength((int)
				// Files.size(cr.cReq.cachedImage));

				// Stream file contents back to client.
				// Files.copy(cr.cReq.cachedImage, response.getOutputStream());
			} else {
				// TODO: I don't think I need this anymore with NIO2 paths API
				// we won't create the actual file on-disk for writing until
				// we can write to it.
				System.out.println("@@@### FIRING CLEANUP!");

				/*
				 * Cleanup any temp files we created incase the request was not
				 * found in S3; we don't want to create a temp file locally for
				 * a file that doesn't exist in the origin.
				 */
				// EXEC_SERVICE.execute(new RequestCleanupTask(cRes.cReq));
				//
				// // Render error.
				// response.sendError(cRes.httpCode, cRes.message);
			}
		} finally {
			try {
				// Safely close the client OutputStream.
				response.getOutputStream().flush();
				response.getOutputStream().close();
			} catch (Exception e) {
				L.error("{}-Ex[Unable to safely flush/close client OutpuStream]");
			}
		}
	}

	private static void processRequest(CDNRequest request) throws CDNResponse {
		/*
		 * 1. If cachedImage exists, stream it back. 2. If it doesn't, check if
		 * origImage does, then process, then stream back. 3. If it doesn't,
		 * origin-pull the image, process, then stream back. 4. If the image
		 * doesn't exist at all, return an error.
		 */

		/*
		 * Initiate the origin-pull if the image isn't locally cached already,
		 * otherwise stream back the local file immediately.
		 */
		// if (!cReq.cached) {
		// response.setHeader("imgscalr-cache", "miss");
		// try {
		// EXEC_SERVICE.submit(new OriginPullTask(cReq)).get();
		// } catch (InterruptedException | ExecutionException e) {
		// e.printStackTrace();
		// }
		// } else
		// response.setHeader("imgscalr-cache", "hit");
		//
		// // Done processing image request, ready to render.
		// throw new CDNResponse(cReq);
	}
	
	class CDNRequest {
		public final String distroName;
		
		public CDNRequest
	}
}
