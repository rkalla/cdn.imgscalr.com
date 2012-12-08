package com.imgscalr.cdn;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.nio.file.Path;

public class CDNResponse extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public final int httpCode;
	public final String message;
	public final Path image;
	public final String mimeType;

	public CDNResponse(int httpCode, String message) {
		this.httpCode = httpCode;
		this.message = message;
		this.image = null;
		this.mimeType = null;
	}

	public CDNResponse(Path image, String mimeType) {
		this.httpCode = SC_OK;
		this.message = null;
		this.image = image;
		this.mimeType = mimeType;
	}

	@Override
	public String toString() {
		return CDNResponse.class.getName()
				+ '['
				+ (httpCode == SC_OK ? "image=" + image + ", mimeType="
						+ mimeType : "httpCode=" + httpCode + ", message='"
						+ message + "'") + ']';
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