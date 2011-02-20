package com.kupriyanov.android.apps.translate.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.kupriyanov.android.apps.translate.R;
import com.kupriyanov.android.apps.translate.Setup;

public class TranslateIntentPreferences extends PreferenceActivity implements OnPreferenceChangeListener,
		DialogInterface.OnDismissListener {

	private static String PREFERENCE_LANGUAGES = "languages";
	private PreferenceScreen preferenceScreen;

	private int mLanguagesCount = 0;

	GoogleAnalyticsTracker tracker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start(Setup.API_KEY_GOOGLEANALYTICS, 1, this);

		if (!Setup.API_KEY_GOOGLEANALYTICS_ON) {
			tracker.stop();
		}

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		preferenceScreen = (PreferenceScreen) findPreference(TranslateIntentPreferences.PREFERENCE_LANGUAGES);

		preferenceScreen.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Toast.makeText(getApplicationContext(), "onPreferenceChange(" +
				// preference.getKey() + "):" + newValue,
				// Toast.LENGTH_SHORT).show();

				return false;
			}
		});

		preferenceScreen.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				// Toast.makeText(getApplicationContext(), "onPreferenceClick(" +
				// preference.getKey() + ")",
				// Toast.LENGTH_SHORT).show();

				// PreferenceScreen a = (PreferenceScreen) preference;
				// a.getDialog().getWindow().setTitle("dgdgdfg");

				return false;
			}
		});

		for (int item = 0; item < preferenceScreen.getPreferenceCount(); item++) {
			CheckBoxPreference chk = (CheckBoxPreference) preferenceScreen.getPreference(item);
			if (chk.isChecked()) {
				mLanguagesCount++;
			}
			preferenceScreen.getPreference(item).setOnPreferenceChangeListener(this);
			preferenceScreen.getPreference(item).setDependency(TranslateIntentPreferences.PREFERENCE_LANGUAGES);
		}

		// updateTitle(null);
		// preferenceScreen.setTitle("wewe");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		if ((Boolean) newValue) {

			if (mLanguagesCount >= Setup.LANGUAGES_COUNT) {
				Toast.makeText(getApplicationContext(), "Sorry only " + Setup.LANGUAGES_COUNT + " are allowed",
						Toast.LENGTH_SHORT).show();
				return false;
			} else {
				mLanguagesCount++;
				updateTitle(preference);
			}

		} else {
			mLanguagesCount--;
			updateTitle(preference);
		}

		return true;
	}

	private void updateTitle(Preference preference) {
		setResult(RESULT_OK);

		// Toast.makeText(getApplicationContext(),
		// "updateTitle " + preference + ":Languages " + mLanguagesCount + "/" +
		// Setup.LANGUAGES_COUNT,
		// Toast.LENGTH_SHORT).show();

		findPreference(TranslateIntentPreferences.PREFERENCE_LANGUAGES).setTitle(
				"Languages " + mLanguagesCount + "/" + Setup.LANGUAGES_COUNT);

		preferenceScreen.setTitle("Languages " + mLanguagesCount + "/" + Setup.LANGUAGES_COUNT);

		// this.setTitle("Languages " + mLanguagesCount + "/" +
		// Setup.LANGUAGES_COUNT);

		// try {
		// preference.notifyAll();
		// } catch (Exception e) {
		// // TODO: handle exception
		// }

		try {
			preferenceScreen.getDialog().getWindow()
					.setTitle("Languages " + mLanguagesCount + "/" + Setup.LANGUAGES_COUNT);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	@Override
	protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
		// Toast.makeText(getApplicationContext(), "onChildTitleChanged " + title,
		// Toast.LENGTH_SHORT).show();
		super.onChildTitleChanged(childActivity, title);
	}

	@Override
	protected void onTitleChanged(CharSequence title, int color) {
		// Toast.makeText(getApplicationContext(), "onTitleChanged " + title,
		// Toast.LENGTH_SHORT).show();

		super.onTitleChanged(title, color);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Stop the tracker when it is no longer needed.
		tracker.stop();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		// Toast.makeText(getApplicationContext(), "DialogInterface " + dialog,
		// Toast.LENGTH_SHORT).show();

	}

	@Override
	protected void onRestart() {
		// Toast.makeText(getApplicationContext(), "onRestart",
		// Toast.LENGTH_SHORT).show();
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// Toast.makeText(getApplicationContext(), "onResume",
		// Toast.LENGTH_SHORT).show();
		super.onResume();
	}

//	@Override
//	public void onBackPressed() {
//		// Toast.makeText(getApplicationContext(), "onBackPressed",
//		// Toast.LENGTH_SHORT).show();
//		super.onBackPressed();
//	}

}
