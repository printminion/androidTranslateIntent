package com.kupriyanov.android.apps.translate.ui;

import java.net.MalformedURLException;

import com.kupriyanov.android.apps.translate.Preferences;
import com.kupriyanov.android.apps.translate.R;
import com.kupriyanov.android.apps.translate.Setup;
import com.kupriyanov.android.net.utils.NetworkUtils;
import com.zedray.framework.ui.BaseActivity;
import com.zedray.framework.utils.Type;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class TranslateActivity extends BaseActivity {
	private static final String TAG = "TranslateActivity";

	// private static String txtToTranslate = "";
	private static final int MENU_SETUP = 0x001;
	private static final int MENU_SHARE = 0x002;

	protected static final int REQUEST_PREFERENCES = 8;

	private LinearLayout llTranslateButtons;
	private LinearLayout llTranslateAddButton;

	private EditText txtSource = null;
	private EditText txtTarget = null;

	private String strLanguages = "";

	GoogleAnalyticsTracker tracker;

	private boolean bFirstStart;

	private String strLastTargetLanguage;

	/** UI TextViews. **/
	// private TextView mTextView, mTextViewQueue;

	// private LocalService mBoundService;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main_activity);

		checkInternetConnection();

		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start(Setup.API_KEY_GOOGLEANALYTICS, 1, this);

		restorePreferences();
		if (this.bFirstStart) {
			// showHelp(true);
		}

		Log.d(TAG, "bFirstStart:" + this.bFirstStart);
		Log.d(TAG, "Setup.API_KEY_GOOGLEANALYTICS_ON:" + Setup.API_KEY_GOOGLEANALYTICS_ON);

		if (!Setup.API_KEY_GOOGLEANALYTICS_ON) {
			tracker.stop();
		}

		txtSource = (EditText) findViewById(R.id.txtSource);
		txtTarget = (EditText) findViewById(R.id.txtTarget);

		llTranslateButtons = (LinearLayout) findViewById(R.id.llTranslateButton);
		llTranslateAddButton = (LinearLayout) findViewById(R.id.llTranslateAddButton);

		if (populateButtonsList() == 0) {
			txtTarget.setVisibility(View.GONE);
		}

		if (txtTarget.getText().toString().length() == 0) {
			txtTarget.setVisibility(View.GONE);
		}

		// get intent data
		if (!checkIntentData()) {
			tracker.trackPageView("/translate/byintent/");
			txtSource.setText(getString(R.string.app_instruction));
		} else {
			tracker.trackPageView("/translate/home/");
		}

		// prediction api?

		// choose language

		// do query

		// speak data?

		// share data

	}

	private boolean checkInternetConnection() {

		if (NetworkUtils.ifNetworkConected(getApplicationContext())) {
			return true;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_connectivity_title).setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage(R.string.dialog_connectivity_message).setCancelable(false)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						TranslateActivity.this.finish();
						startActivity(new Intent(Settings.ACTION_SETTINGS));
					}
				}).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		AlertDialog alert = builder.create();
		alert.show();

		return false;
	}

	/**
	 * read data from intent if exists
	 */
	private boolean checkIntentData() {

		Log.d(TAG, "checkIntentData");

		try {
			Intent intentObj = getIntent();
			setIntent(null);

			if (intentObj != null) {
				String theAction = intentObj.getAction();
				if (Intent.ACTION_SEND.equals(theAction)) {
					System.out.println(intentObj.getExtras());

					Bundle b = intentObj.getExtras();

					if (b != null && b.containsKey(Intent.EXTRA_TEXT)) {

						txtSource.setText((String) b.get(Intent.EXTRA_TEXT));

						// showText(txtToTranslate);

						// System.out.println(txtToTranslate);

						return true;
					} else {
						throw new IllegalStateException(
								"FetchTextForViewIntent requires intent extras parameter 'EXTRA_TEXT'.");
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;

	}

	// private void showText(String teextToTranslate) {
	// // TODO Auto-generated method stub
	// TextView t = (TextView) findViewById(R.id.txtSource);
	// t.setText(teextToTranslate);
	// }

	public void onTranslate(View v) throws MalformedURLException {
		Button b = (Button) v;

		final String strTargetLanguage = (String) b.getTag().toString();
		translate(strTargetLanguage, txtSource.getText().toString());
	}

	private void translate(String strTargetLanguage, String txtToTranslate) {

		/*
		 * no translation on empty strings
		 */
		String txtToTranslateNoSpaces = txtToTranslate.replace(" ", "");
		if (txtToTranslateNoSpaces.length() == 0) {
			Toast.makeText(getApplicationContext(), R.string.toast_nothing_todo, Toast.LENGTH_LONG).show();
			return;
		}
		this.strLastTargetLanguage = strTargetLanguage;

		tracker.trackEvent("Clicks", // Category
				"Button", // Action
				"translate_" + strTargetLanguage, // Label
				77); // Value

		// tracker.trackPageView("/translate/" + strTargetLanguage);

		setProgress(true);
		txtTarget.setText("");

		Bundle outBundle = new Bundle();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs.getBoolean(Preferences.STRIP_URLS, false)) {
			txtToTranslate = txtToTranslate.replaceAll("(http://[^ ]*)", "");
			txtToTranslate = txtToTranslate.replaceAll("(https://[^ ]*)", "");
		}

		outBundle.putString("TEXT", txtToTranslate);
		outBundle.putString("LANGUAGE_TO", strTargetLanguage);

		outBundle.putBoolean(Preferences.API, prefs.getBoolean(Preferences.API, false));

		getServiceQueue().postToService(Type.DO_TRANSLATE_TASK, outBundle);

	}

	private void setProgress(boolean value) {

		if (value) {
			getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
		} else {
			getWindow().setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS, Window.PROGRESS_VISIBILITY_OFF);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.clear();

		// menu.add(0, MENU_SHARE, 0,
		// R.string.share_title).setIcon(android.R.drawable.ic_menu_share);
		menu.add(0, MENU_SETUP, 0, R.string.settings_title).setIcon(android.R.drawable.ic_menu_preferences);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETUP:
			onSetupClick(new View(getApplicationContext()));
			break;
		case MENU_SHARE:
			Toast.makeText(getApplicationContext(), "TODO: Add some sharing ;)", Toast.LENGTH_SHORT).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onSetupClick(View v) {
		addLanguage(v);
	}

	public void addLanguage(View v) {
		tracker.trackEvent("Clicks", // Category
				"Button", // Action
				"add_language", // Label
				77); // Value
		startActivityForResult(new Intent(getApplicationContext(), TranslateIntentPreferences.class), REQUEST_PREFERENCES);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {

			switch (requestCode) {
			case REQUEST_PREFERENCES:

				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

				Setup.API_KEY_GOOGLEANALYTICS_ON = sharedPrefs.getBoolean(Preferences.GOOGLEANALYTICS_ON, true);
				if (Setup.API_KEY_GOOGLEANALYTICS_ON) {
					tracker.stop();
				} else {
					tracker.start(Setup.API_KEY_GOOGLEANALYTICS, this);
				}

				if (populateButtonsList() == 0) {
					txtTarget.setVisibility(View.GONE);
					tracker.trackPageView("/translate/changed_languages/null");
				} else {
					tracker.trackPageView("/translate/changed_languages/" + strLanguages);
				}

				if (txtTarget.getText().toString().length() == 0) {
					txtTarget.setVisibility(View.GONE);
				}

				break;
			default:
				break;
			}
		}
	}

	private int populateButtonsList() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		llTranslateButtons.removeAllViews();

		/*
		 * populate languages
		 */

		final String[] mTestArray = getResources().getStringArray(R.array.entryvalues_language_list_preference);

		this.strLanguages = "";
		int countLanguagesAdded = 0;
		for (int i = 0; i < mTestArray.length; i++) {

			if (!sharedPrefs.getBoolean(mTestArray[i], false)) {
				continue;
			}

			if (countLanguagesAdded > Setup.LANGUAGES_COUNT) {
				break;
			}

			countLanguagesAdded++;

			Button b = new Button(getApplicationContext());
			llTranslateButtons.addView(b, LayoutParams.WRAP_CONTENT);

			// android:layout_width="0.0dip"
			// android:layout_height="fill_parent" android:text="de"
			// android:layout_weight="1.0"

			b.setText(mTestArray[i].substring(0, 1).toUpperCase() + mTestArray[i].substring(1));
			b.setTag(mTestArray[i]);
			b.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

			if (this.strLanguages.length() == 0) {
				this.strLanguages = mTestArray[i];
			} else {
				this.strLanguages = strLanguages + "," + mTestArray[i];
			}

			b.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					final String strTargetLanguage = (String) v.getTag().toString();
					translate(strTargetLanguage, txtSource.getText().toString());
				}
			});

		}

		/*
		 * show right buttons only
		 */
		if (countLanguagesAdded == 0) {
			llTranslateAddButton.setVisibility(View.VISIBLE);
			// llTranslateButtons.setVisibility(View.GONE);
		} else {
			llTranslateAddButton.setVisibility(View.GONE);
			// llTranslateButtons.setVisibility(View.VISIBLE);
		}

		return countLanguagesAdded;
	}

	@Override
	protected final void onResume() {
		updateAll();
		checkInternetConnection();
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	/***
	 * Update all UI elements - called onCreate() and onResume().
	 */
	private void updateAll() {
		// updateTextView();
		// updateTextViewQueue();
	}

	// /***
	// * Update the given TextView with information from the Application Cache.
	// */
	// private void updateTextView() {
	// // txtTarget.setText(getCache().getStateShortTask());
	// // txtTarget.setText(Html.fromHtml(pageContent));
	// txtTarget.setText(txtTarget.getText() + "\nupdateTextView");
	// txtTarget.setVisibility(View.VISIBLE);
	// }
	//
	// /***
	// * Update the given TextView with information from the Application Cache.
	// */
	// private void updateTextViewQueue() {
	// // txtTarget.setText(getCache().getQueue());
	// txtTarget.setText(txtTarget.getText() + "\nupdateTextViewQueue");
	// txtTarget.setVisibility(View.VISIBLE);
	// }

	/***
	 * Override the post method to receive incoming messages from the Service.
	 * 
	 * @param type
	 *           Message type.
	 * @param bundle
	 *           Optional Bundle of extra information, NULL otherwise.
	 */
	@Override
	public final void post(final Type type, final Bundle bundle) {
		switch (type) {
		case UPDATE_SHORT_TASK:
			// updateTextView();
			break;

		case UPDATE_QUEUE:
			// updateTextViewQueue();
			break;

		case FINISHED_TRANSLATION:

			txtTarget.setText(Html.fromHtml(bundle.getString("TRANSLATION")));

			// txtTarget.setText(bundle.getString("TRANSLATION"));
			txtTarget.setVisibility(View.VISIBLE);
			setProgress(false);

			tracker.trackPageView("/translated/" + this.strLastTargetLanguage);

			break;

		case FINISHED_TRANSLATION_WITH_ERROR:

			txtTarget.setText("");
			txtTarget.setVisibility(View.GONE);

			Toast.makeText(getApplicationContext(), bundle.getString("EXCEPTION"), Toast.LENGTH_LONG).show();

			setProgress(false);

			tracker.trackPageView("/translate/error/" + bundle.getString("EXCEPTION"));

			break;

		default:
			/** Let the BaseActivity handle other message types. */
			super.post(type, bundle);
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Stop the tracker when it is no longer needed.
		tracker.stop();
	}

	private void restorePreferences() {
		// SharedPreferences sharedPrefs = getPreferences(0);
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		this.bFirstStart = sharedPrefs.getBoolean(Preferences.IS_FIRST_START, true);
		sharedPrefs.edit().putBoolean(Preferences.IS_FIRST_START, false);
		sharedPrefs.edit().commit();

		Log.d(TAG, "restorePreferences[bFirstStart]:" + this.bFirstStart);

		Setup.API_KEY_GOOGLEANALYTICS_ON = sharedPrefs.getBoolean(Preferences.IS_FIRST_START, true);

		if (this.bFirstStart) {
			sharedPrefs.edit().putBoolean(Preferences.GOOGLEANALYTICS_ON, true);
			sharedPrefs.edit().commit();
		}
	}

}