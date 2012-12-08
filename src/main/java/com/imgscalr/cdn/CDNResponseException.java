package com.imgscalr.cdn;

import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

public class CDNResponseException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public final int httpCode;
	public final String message;
	public final Path image;

	public CDNResponseException(int httpCode, String message) {
		this.httpCode = httpCode;
		this.message = message;
		this.image = null;
	}

	public CDNResponseException(Path image) {
		this.httpCode = HttpServletResponse.SC_OK;
		this.message = null;
		this.image = image;
	}

	/**
	 * Overridden to do nothing and return <code>null</code> for performance
	 * reasons since this class is used for flow-control.
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return null;
	}
}