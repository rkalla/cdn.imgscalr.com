package com.imgscalr.cdn;

import static javax.servlet.http.HttpServletResponse.SC_OK;

public class CDNResponse extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public final int httpCode;
	public final String message;
	public final CDNRequest cReq;

	public CDNResponse(int httpCode, String message) {
		this.httpCode = httpCode;
		this.message = message;
		this.cReq = null;
	}

	public CDNResponse(CDNRequest cReq) {
		this.httpCode = SC_OK;
		this.message = null;
		this.cReq = cReq;
	}

	@Override
	public String toString() {
		return CDNResponse.class.getName()
				+ "["
				+ (httpCode == SC_OK ? "cReq=" + cReq : "httpCode=" + httpCode
						+ ", message='" + message + "'") + "]";
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