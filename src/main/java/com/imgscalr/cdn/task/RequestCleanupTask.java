package com.imgscalr.cdn.task;

import com.imgscalr.cdn.CDNRequest;

public class RequestCleanupTask implements Runnable {
	private CDNRequest cReq;

	public RequestCleanupTask(CDNRequest cReq) {
		this.cReq = cReq;
	}

	@Override
	public void run() {
		System.out.println("@@HERE!");
//		System.out.println("@@tmp: " + cReq.tmpFile);
//		System.out.println("@@exists: " + cReq.tmpFile.exists());
//		System.out.println("@@read/write: " + cReq.tmpFile.canRead() + "/" + cReq.tmpFile.canWrite());
//		System.out.println("@@delete: " + cReq.tmpFile.delete());
		
//		if (cReq.tmpFile != null)
//			System.out.println("Deleting Tmp File: " + cReq.tmpFile.delete());
	}
}