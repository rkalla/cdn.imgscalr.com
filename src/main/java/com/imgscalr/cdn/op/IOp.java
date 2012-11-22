package com.imgscalr.cdn.op;

import java.awt.image.BufferedImage;

public interface IOp {
	/*
	 * Removed priority concept because I don't know if someone might
	 * specifically want to chain together (in a specific order) operations that
	 * generate a specific output even though I might deem the op-order
	 * unoptimized.
	 */
	public BufferedImage apply(BufferedImage input)
			throws IllegalArgumentException;
}