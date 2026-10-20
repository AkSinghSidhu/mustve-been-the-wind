package com.windsmoke.client;

/**
 * Implemented on {@code BannerFlagModel} by the client mixin. It gives the
 * custom cloth renderer the pivot and world phase that vanilla keeps private.
 */
public interface BannerFlagAccess {
	float windsmoke$phase();

	float windsmoke$baseY();

	float windsmoke$baseZ();
}
