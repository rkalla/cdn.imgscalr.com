package com.imgscalr.cdn;

public class CDNServletResponse extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public final int httpCode;
	public final String message;

	public CDNServletResponse(int httpCode, String message) {
		this.httpCode = httpCode;
		this.message = message;
	}

	@Override
	public String toString() {
		return CDNServletResponse.class.getName() + "[" + "httpCode="
				+ httpCode + ", message='" + message + "']";
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