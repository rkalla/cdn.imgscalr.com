package com.imgscalr.cdn;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CDNServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final File TMP_DIR = new File(
			System.getProperty("java.io.tmpdir"));

	@Override
	public void service(final ServletRequest request,
			final ServletResponse response) throws ServletException,
			IOException {
		if (!(request instanceof HttpServletRequest))
			throw new ServletException("non-HTTP request");
		if (!(response instanceof HttpServletResponse))
			throw new ServletException("non-HTTP response");

		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse res = (HttpServletResponse) response;

		if (!"GET".equals(req.getMethod()))
			res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"HTTP Method [" + req.getMethod()
							+ "] not supported by this server.");

//		String bucketName = RequestUtil.parseBucketName(req.getServerName());
//		String fileName = RequestUtil.parseFileName(req.getRequestURI());
		String queryString = req.getQueryString();

		System.out.println("Req Host: " + req.getServerName());
		System.out.println("Req URI: " + req.getRequestURI());
		System.out.println("Req PI : " + req.getPathInfo());
		System.out.println("Req QS:  " + req.getQueryString());

		res.getWriter().write(
				"URI: " + req.getRequestURI() + "?" + req.getQueryString());
		res.getWriter().flush();
	}
}
