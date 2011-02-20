package com.kupriyanov.android.net.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtils {

	public static boolean ifNetworkConected(Context context) {

		final NetworkInfo mobileNetworkInfo = ((ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		final NetworkInfo wifiNetworkInfo = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mobileNetworkInfo.isConnected()) {
			return true;
		}

		if (wifiNetworkInfo.isAvailable() && wifiNetworkInfo.isConnected()) {
			return true;
		}

		return false;

	}
}
