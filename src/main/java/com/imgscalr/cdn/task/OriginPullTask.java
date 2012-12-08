package com.imgscalr.cdn.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.imgscalr.cdn.CDNRequest;

public class OriginPullTask implements Runnable {
	private static final Logger L = LoggerFactory
			.getLogger(OriginPullTask.class);

	private CDNRequest cReq;

	public OriginPullTask(CDNRequest cReq) {
		this.cReq = cReq;
	}

	@Override
	public void run() {
		long sTime = System.currentTimeMillis();
//		GetObjectRequest request = new GetObjectRequest(CDN_BUCKET,
//				cReq.distroName + '/' + cReq.fileName);
//
//		L.info("{}-Begin[cReq={}, startTime={}]",
//				OriginPullTask.class.getName(), cReq, sTime);
//
//		try {
//			/*
//			 * Begin download of object from S3. This will throw an exception if
//			 * the filename was invalid and the object doesn't exist.
//			 */
//			S3Object obj = S3_CLIENT.getObject(request);
//
//			// Get stream to download file from.
//			try (S3ObjectInputStream in = obj.getObjectContent()) {
//				/*
//				 * Safely copy stream contents to local file that we already
//				 * vetted to make sure we could create it and write to it.
//				 */
//				// int size = copy(in, cReq.tmpFile);
//
//				// L.info("{}-End[elapsedTime={} ms, size={} bytes]",
//				// OriginPullTask.class.getName(),
//				// (System.currentTimeMillis() - sTime), size);
//			} catch (Exception e) {
//				throw new CDNResponse(SC_INTERNAL_SERVER_ERROR,
//						"Server was unable to retrieve the requested image '"
//								+ cReq.fileName
//								+ "' from the distribution origin.");
//			}
//		} catch (Exception e) {
//			throw new CDNResponse(SC_NOT_FOUND, "Requested file '"
//					+ cReq.fileName + "' does not exist in distribution '"
//					+ cReq.distroName + "'");
//		}
	}
}