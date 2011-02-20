/*
 * Copyright 2010 Mark Brady
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kupriyanov.android.apps.translate.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.kupriyanov.android.apps.translate.Preferences;
import com.kupriyanov.android.apps.translate.R;
import com.kupriyanov.android.apps.translate.Setup;
import com.zedray.framework.application.BaseApplication;
import com.zedray.framework.application.Cache;
import com.zedray.framework.application.UiQueue;
import com.zedray.framework.utils.Type;

/***
 * Used by the Service to perform long running tasks (e.g. network connectivity)
 * in a single separate thread. This implementation uses a single thread to pop
 * items off the end of a ServiceQueue, providing a clear finishing point (i.e.
 * current task complete & queue empty) where the Services own stopSelf() method
 * can then be called to terminate the background part of the Application.
 */
public class WorkerThread extends Thread {

	/**
	 * Shared buffer used by {@link #getUrlContent(String)} when reading results
	 * from an API request.
	 */
	private static byte[] sBuffer = new byte[512];

	/**
	 * {@link StatusLine} HTTP status code when no server error has occurred.
	 */
	private static final int HTTP_STATUS_OK = 200;

	/**
	 * User-agent string to use when making requests. Should be filled using
	 * {@link #prepareUserAgent(Context)} before making any other calls.
	 */
	private static String sUserAgent = null;

	/**
	 * [Optional] Execution state of the currently running long process, used by
	 * the service to recover the state after the Service has been abnormally
	 * terminated.
	 */
	public static final String PROCESS_STATE = "PROCESS_STATE";
	/** [Optional] Size of long task increment. **/
	// private static final int LONG_TASK_INCREMENT = 10;
	/** [Optional] End of long task. **/
	// private static final int LONG_TASK_COMPLETE = 100;
	/***
	 * [Optional] Configures how much time (in milliseconds) should be wasted
	 * between UI updates - for test use only.
	 */
	// private static final int WASTE_TIME = 2000;

	private static final String TAG = "WorkerThread";

	/** [Optional] Synchronisation lock for the Thread Sleep. **/
	private final Object mWakeLock = new Object();
	/** Queue of incoming messages. **/
	private final List<Message> mWorkQueue = new ArrayList<Message>();
	/** Pointer to the Application Cache. **/
	private final Cache mCache;
	/** Pointer to the Application UiQueue. **/
	private final UiQueue mUiQueue;
	/** Pointer to the parent Service.. **/
	private TranslateService mMyService;
	/***
	 * TRUE when the WorkerThread can no longer handle incoming messages, because
	 * it is shutting down or dead.
	 */
	private boolean stopping = false;

	/***
	 * Constructor which stores pointers to the Application Cache, UiQueue and
	 * parent Service.
	 * 
	 * @param cache
	 *           Application Cache.
	 * @param uiQueue
	 *           UiQueue.
	 * @param myService
	 *           MyService.
	 */
	protected WorkerThread(final Cache cache, final UiQueue uiQueue, final TranslateService myService) {
		mCache = cache;
		mUiQueue = uiQueue;
		mMyService = myService;
	}

	/***
	 * Add a message to the work queue.
	 * 
	 * @param message
	 *           Message containing a description of work to be done.
	 */
	protected final void add(final Message message) {
		synchronized (mWorkQueue) {
			Log.i(BaseApplication.LOG_TAG, "WorkerThread.add() " + "Message type[" + Type.getType(message.what) + "]");
			mWorkQueue.add(message);
		}
		showQueue();
	}

	/***
	 * Returns the current state of the WorkerThread.
	 * 
	 * @return TRUE when the WorkerThread can no longer handle incoming messages,
	 *         because it is dead or shutting down, FALSE otherwise.
	 */
	public final boolean isStopping() {
		return stopping;
	}

	/***
	 * Main run method, where all the queued messages are executed.
	 */
	public final void run() {
		setName("WorkerThread");
		while (mWorkQueue.size() > 0) {
			Type type;
			Bundle bundle = null;
			synchronized (mWorkQueue) {
				Message message = mWorkQueue.remove(0);
				Log.i(BaseApplication.LOG_TAG, "WorkerThread.run() " + "Message type[" + Type.getType(message.what) + "]");
				type = Type.getType(message.what);
				if (message.obj != null && message.obj.getClass() == Bundle.class) {
					bundle = (Bundle) message.obj;
				}
			}
			showQueue();

			switch (type) {

			case DO_TRANSLATE_TASK:
				doTranslateTask(bundle);
				break;

			// case DO_SHORT_TASK:
			// doShortTask(bundle);
			// break;

			// case DO_LONG_TASK:
			// doLongTask(bundle);
			// break;

			default:
				// Do nothing.
				break;
			}
		}

		// mCache.setStateLongTask("");
		mUiQueue.postToUi(Type.UPDATE_SHORT_TASK, null, true);
		mCache.setStateShortTask("");
		// mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);

		stopping = true;
		mMyService.stopSelf();
	}

	// /***
	// * [Optional] Example task which takes time to complete and repeatedly
	// * updates the UI.
	// *
	// * @param bundle
	// * Bundle of extra information.
	// */
	// private void doShortTask(final Bundle bundle) {
	// mCache.setStateShortTask("Loading short task");
	// mUiQueue.postToUi(Type.UPDATE_SHORT_TASK, null, true);
	// wasteTime(WASTE_TIME);
	// mCache.setStateShortTask("Running short task");
	// mUiQueue.postToUi(Type.UPDATE_SHORT_TASK, null, true);
	// wasteTime(WASTE_TIME);
	// mCache.setStateShortTask("Finishing short task");
	// mUiQueue.postToUi(Type.UPDATE_SHORT_TASK, null, true);
	// wasteTime(WASTE_TIME);
	// mCache.setStateShortTask("Finished short task");
	// mUiQueue.postToUi(Type.UPDATE_SHORT_TASK, null, true);
	//
	// if (bundle != null) {
	//
	// Bundle outBundle = new Bundle();
	// outBundle.putString("TRANSLATION", "translation");
	//
	// mUiQueue.postToUi(Type.FINISHED_TRANSLATION, outBundle, false);
	// }
	// }

	/***
	 * [Optional] doTranslateTask time to complete and repeatedly updates the UI.
	 * 
	 * @param bundle
	 *           Bundle of extra information.
	 */
	private void doTranslateTask(final Bundle bundle) {

		if (bundle != null) {
			String pageContent = "";
			Bundle outBundle = new Bundle();

			try {

				prepareUserAgent(mMyService.getApplicationContext());
				String strSourceLanguage = "";

				if (bundle.getBoolean(Preferences.API)) {
					String strQuery2 = "https://www.googleapis.com/language/translate/v2?key="
							+ Setup.API_KEY_GOOGLETRANSLATE + "&q=" + Uri.encode(bundle.getString("TEXT")) + "&source="
							+ strSourceLanguage + "&target=" + bundle.getString("LANGUAGE_TO") + "&prettyprint=true";
					Log.d(TAG, "[REQUEST2->]:" + strQuery2);
					pageContent = getPageContent2(strQuery2, false);

				} else {
					String strQuery = "https://ajax.googleapis.com/ajax/services/language/translate?v=1.0" + "&q="
							+ Uri.encode(bundle.getString("TEXT")) + "&langpair=%7C" + bundle.getString("LANGUAGE_TO")
							+ "&key=" + Setup.API_KEY_GOOGLETRANSLATE;
					Log.d(TAG, "[REQUEST1->]:" + strQuery);
					pageContent = getPageContent(strQuery, false);

				}

				// strQuery = Uri.encode(strQuery);

				outBundle.putString("TRANSLATION", pageContent);
				mUiQueue.postToUi(Type.FINISHED_TRANSLATION, outBundle, false);

			} catch (ApiException e) {
				Log.e(TAG, "Couldn't contact API", e);

				outBundle.putString("EXCEPTION", "Couldn't contact API");
				mUiQueue.postToUi(Type.FINISHED_TRANSLATION_WITH_ERROR, outBundle, false);

			} catch (ParseException e) {

				Log.e(TAG, "Couldn't parse API response", e);

				outBundle.putString("EXCEPTION", "Couldn't parse API response");
				mUiQueue.postToUi(Type.FINISHED_TRANSLATION_WITH_ERROR, outBundle, false);

			}

		}
	}

	/**
	 * Prepare the internal User-Agent string for use. This requires a
	 * {@link Context} to pull the package name and version number for this
	 * application.
	 */
	public static void prepareUserAgent(Context context) {
		try {
			// Read package name and version number from manifest
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			sUserAgent = String
					.format(context.getString(R.string.template_user_agent), info.packageName, info.versionName);

		} catch (NameNotFoundException e) {
			Log.e(TAG, "Couldn't find package information in PackageManager", e);
		}
	}

	/**
	 * Read and return the content for a specific Wiktionary page. This makes a
	 * lightweight API call, and trims out just the page content returned.
	 * Because this call blocks until results are available, it should not be run
	 * from a UI thread.
	 * 
	 * @param title
	 *           The exact title of the Wiktionary page requested.
	 * @param expandTemplates
	 *           If true, expand any wiki templates found.
	 * @return Exact content of page.
	 * @throws ApiException
	 *            If any connection or server error occurs.
	 * @throws ParseException
	 *            If there are problems parsing the response.
	 */
	public static String getPageContent(String apiQuery, boolean expandTemplates) throws ApiException, ParseException {

		String content = getUrlContent(apiQuery);

		Log.d(TAG, "[RESPONSE->]:" + content);

		try {
			// Drill into the JSON response to find the content body
			JSONObject response = new JSONObject(content);
			JSONObject data = response.getJSONObject("responseData");
			// JSONObject pages = data.getJSONObject("translations");
			// JSONObject page = pages.getJSONObject((String) pages.keys().next());

			// JSONArray translations = data.getJSONArray("translations");
			// JSONObject revision = translations.getJSONObject(0);

			return data.getString("translatedText");
		} catch (JSONException e) {
			throw new ParseException("Problem parsing API response:" + content, e);
		}

	}

	/**
	 * Read and return the content for a specific Wiktionary page. This makes a
	 * lightweight API call, and trims out just the page content returned.
	 * Because this call blocks until results are available, it should not be run
	 * from a UI thread.
	 * 
	 * @param title
	 *           The exact title of the Wiktionary page requested.
	 * @param expandTemplates
	 *           If true, expand any wiki templates found.
	 * @return Exact content of page.
	 * @throws ApiException
	 *            If any connection or server error occurs.
	 * @throws ParseException
	 *            If there are problems parsing the response.
	 */
	public static String getPageContent2(String apiQuery, boolean expandTemplates) throws ApiException, ParseException {

		String content = getUrlContent(apiQuery);

		Log.d(TAG, "[RESPONSE->]:" + content);

		try {
			// Drill into the JSON response to find the content body
			JSONObject response = new JSONObject(content);
			JSONObject data = response.getJSONObject("data");
			// JSONObject pages = data.getJSONObject("translations");
			// JSONObject page = pages.getJSONObject((String) pages.keys().next());

			JSONArray translations = data.getJSONArray("translations");
			JSONObject revision = translations.getJSONObject(0);
			return revision.getString("translatedText");
		} catch (JSONException e) {
			throw new ParseException("Problem parsing API response:" + content, e);
		}

	}

	/**
	 * Pull the raw text content of the given URL. This call blocks until the
	 * operation has completed, and is synchronized because it uses a shared
	 * buffer {@link #sBuffer}.
	 * 
	 * @param url
	 *           The exact URL to request.
	 * @return The raw content returned by the server.
	 * @throws ApiException
	 *            If any connection or server error occurs.
	 */
	protected static synchronized String getUrlContent(String url) throws ApiException {
		if (sUserAgent == null) {
			throw new ApiException("User-Agent string must be prepared");
		}

		// Create client and set our specific user-agent string
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", sUserAgent);

		try {
			HttpResponse response = client.execute(request);

			// Check if server response is valid
			StatusLine status = response.getStatusLine();
			if (status.getStatusCode() != HTTP_STATUS_OK) {
				throw new ApiException("Invalid response from server: " + status.toString());
			}

			// Pull content stream from response
			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();

			ByteArrayOutputStream content = new ByteArrayOutputStream();

			// Read response into a buffered stream
			int readBytes = 0;
			while ((readBytes = inputStream.read(sBuffer)) != -1) {
				content.write(sBuffer, 0, readBytes);
			}

			// Return result from buffered stream
			return new String(content.toByteArray());
		} catch (IOException e) {
			throw new ApiException("Problem communicating with API", e);
		}
	}

	/**
	 * Thrown when there were problems contacting the remote API server, either
	 * because of a network error, or the server returned a bad status code.
	 */
	public static class ApiException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ApiException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		public ApiException(String detailMessage) {
			super(detailMessage);
		}
	}

	/**
	 * Thrown when there were problems parsing the response to an API call,
	 * either because the response was empty, or it was malformed.
	 */
	public static class ParseException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ParseException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}
	}

	// /***
	// * [Optional] Example task which takes time to complete and repeatedly
	// * updates the UI.
	// *
	// * @param bundle Bundle of extra information.
	// */
	// private void doLongTask(final Bundle bundle) {
	// mCache.setStateLongTask("Loading long task");
	// mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);
	// wasteTime(WASTE_TIME);
	//
	// int i = 0;
	// if (bundle != null) {
	// i = bundle.getInt(PROCESS_STATE);
	// } else {
	// i = 0;
	// }
	//
	// for (; i <= LONG_TASK_COMPLETE; i += LONG_TASK_INCREMENT) {
	// mCache.setStateLongTask("Long task " + i + "% complete");
	// mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);
	// NotificationUtils.notifyUserOfProgress(mMyService
	// .getApplicationContext(), i);
	// wasteTime(WASTE_TIME);
	// mCache.setLongProcessState(i);
	// }
	// /** Clear Long Process state. **/
	// mCache.setLongProcessState(-1);
	//
	// mCache.setStateLongTask("Long task done");
	// mUiQueue.postToUi(Type.UPDATE_LONG_TASK, null, true);
	// NotificationUtils.notifyUserOfProgress(mMyService
	// .getApplicationContext(), -1);
	// }

	/***
	 * [Optional] Example task which sends the current state of the queue to the
	 * UI.
	 */
	private void showQueue() {
		StringBuffer stringBuffer = new StringBuffer();
		for (Message message : mWorkQueue) {
			stringBuffer.append("Message type[");
			stringBuffer.append(Type.getType(message.what));
			stringBuffer.append("]\n");
		}
		mCache.setQueue(stringBuffer.toString());
		mUiQueue.postToUi(Type.UPDATE_QUEUE, null, true);
	}

	// /***
	// * [Optional] Slow down the running task - for test use only.
	// *
	// * @param time
	// * Amount of time to waste.
	// */
	// private void wasteTime(final long time) {
	// long startTime = System.currentTimeMillis();
	// while (System.currentTimeMillis() < startTime + time) {
	// synchronized (mWakeLock) {
	// try {
	// mWakeLock.wait(startTime + WASTE_TIME - System.currentTimeMillis());
	// } catch (InterruptedException e) {
	// // Do nothing.
	// }
	// }
	// }
	// }
}
