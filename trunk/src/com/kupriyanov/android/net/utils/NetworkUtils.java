package com.kupriyanov.android.net.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkUtils {

	private static String TAG = "NetworkUtils";

	public static boolean ifNetworkConected(Context context) {

		try {

			final NetworkInfo mobileNetworkInfo = ((ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

			if (mobileNetworkInfo.isConnected()) {
				return true;
			}

		} catch (Exception e) {
			// TODO: handle exception
			Log.e(TAG, "Failed to get mobileNetworkInfo");
		}

		try {

			final NetworkInfo wifiNetworkInfo = ((ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if (wifiNetworkInfo.isAvailable() && wifiNetworkInfo.isConnected()) {
				return true;
			}

		} catch (Exception e) {
			Log.e(TAG, "Failed to get wifiNetworkInfo");
		}

		return false;

	}
}
