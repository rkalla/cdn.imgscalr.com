package com.imgscalr.cdn.task;

import com.imgscalr.cdn.CDNServletRequest;

public class RequestCleanupTask implements Runnable {
	private CDNServletRequest cReq;

	public RequestCleanupTask(CDNServletRequest cReq) {
		this.cReq = cReq;
	}

	@Override
	public void run() {
		if (cReq.tmpFile != null)
			System.out.println("Deleting Tmp File: " + cReq.tmpFile.delete());
	}
}