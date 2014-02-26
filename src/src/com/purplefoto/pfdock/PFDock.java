/*
Copyright 2013 Eric H. Cloninger, dba PurpleFoto
 
Licensed under the Apache License, Version 2.0 (the "License"); you 
may not use this file except in compliance with the License. You may 
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing 
permissions and limitations under the License
 */

package com.purplefoto.pfdock;
import com.actionbarsherlock.app.SherlockActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


// Used to store information for custom launches of places, music, and voice search buttons
class ComponentInfo {
	public String name;
	public String category;
};

/*
 * PFDock - This is the main activity for the application. Almost all user
 * interaction other than preferences comes through this class.
 */
public class PFDock extends SherlockActivity {
	CarDockReceiver m_receiver = null;
	TextView m_timeView = null;
	TextView m_speedView = null;
	TextView m_batteryView = null;
	ImageButton m_placesBtn = null;
	ImageButton m_musicBtn = null;
	ImageButton m_voiceBtn = null;
	ImageButton m_phoneBtn = null;
	ImageButton m_mapsBtn = null;
	ImageButton m_homeBtn = null;
	ImageView m_providerIcon = null;
	boolean m_isPluggedIn = false;
	boolean m_coarseGPS = true;
	boolean m_imperial = true;
	final int COMPONENT_PICKER = 1;
	ComponentInfo m_components[] = new ComponentInfo[3];
	ComponentInfo m_currentComponent;
	Location m_lastLocation = new Location(LocationManager.GPS_PROVIDER);
	PFLocationListener m_locationListener = new PFLocationListener();
	HashMap<String, int[]> m_colorIcons = new HashMap<String, int[]>();
	HashMap<String, Integer> m_colorText = new HashMap<String, Integer>();

	/*
	 * Respond to ACTION_BATTERY_CHANGED Determines the battery level if it
	 * exists in the extra data and displays it. As the GPS affects the battery
	 * level when not charging, modify the frequency at which updates occur to
	 * preserve power.
	 */
	BroadcastReceiver m_batteryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			Log.i(getPackageName(), "m_batteryReceiver.onReceive");
			int level = intent.getIntExtra("level", 0);
			if (m_batteryView != null)
				m_batteryView.setText(String.valueOf(level) + "%");

			m_isPluggedIn = false;

			// Only allow GPS to operate continuously if the unit is plugged in
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

			int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			if ((plugged == BatteryManager.BATTERY_PLUGGED_AC)
					|| (plugged == BatteryManager.BATTERY_PLUGGED_USB)) {
				m_isPluggedIn = true;

				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 5,
						m_locationListener);
			} else
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000,
						100, m_locationListener);
		}
	};

	/*
	 * Respond to ACTION_TIME_TICK Once a minute, update the clock.
	 */
	BroadcastReceiver m_timeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			Log.i(getPackageName(), "m_timeReceiver.onReceive");
			if (m_timeView != null)
				m_timeView.setText(getSimpleTimeString());
		}
	};

	/*
	 * Respond to ACTION_EXIT_CAR_MODE When the user pulls the device from the
	 * dock, quit the app and dispose of resources.
	 */
	BroadcastReceiver m_carDockExitReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			Log.i(getPackageName(), "m_carDockExitReceiver.onReceive");
			PFDock.this.finish();
		}
	};

	/*
	 * Respond to ACTION_BATTERY_LOW If the battery is low and the user had been
	 * running the app (silly user), go ahead and quit.
	 */
	BroadcastReceiver m_batteryLowReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			Log.i(getPackageName(), "m_batteryLowReceiver.onReceive");
			if (!m_isPluggedIn)
				PFDock.this.finish();
		}
	};

	/*
	 * PFLocationListener - Custom version of LocationListener to handle UI
	 * updates
	 */
	private final class PFLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location loc) {
			Log.i(getPackageName(), "PFLocationListener.onLocationChanged");
			PFDock.this.updateLocationUI(loc);
		}

		private void updateActivityUI(String provider) {
			Location loc = new Location(provider);
			// TODO: Look into this. Might be inefficient. Is there a better way
			// to get a Location without polling the provider?
			onLocationChanged(loc);
		}

		@Override
		public void onProviderDisabled(String provider) {
			Log.i(getPackageName(), "PFLocationListener.onProviderDisabled");
			updateActivityUI(provider);
		}

		@Override
		public void onProviderEnabled(String provider) {
			Log.i(getPackageName(), "PFLocationListener.onProviderEnabled");
			updateActivityUI(provider);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.i(getPackageName(), "PFLocationListener.onStatusChanged");
			updateActivityUI(provider);
		}
	}

	/*
	 * updateLocationUI - Handle UI update for speed
	 */
	public void updateLocationUI(Location loc) {
		float speed = 0;
		if (loc.hasSpeed())
			speed = loc.getSpeed();

		final float METERSPERSEC_to_MPH = (float) 2.23693629;
		final float METERSPERSEC_to_KPH = (float) 3.6;
		String formatString;

		if (m_imperial)
			formatString = String.format("%d mph",
					(int) (speed * METERSPERSEC_to_MPH));
		else
			formatString = String.format("%d kph",
					(int) (speed * METERSPERSEC_to_KPH));

		m_speedView.setText(formatString);

		m_lastLocation = loc;
	}

	/*
	 * PFDock.onDestroy - lifecycle method
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		Log.i(getPackageName(), "PFDock.onDestroy");

		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5,
				m_locationListener);
		lm.removeUpdates(m_locationListener);

		// Get instance of UI manager and disconnect the forced return to our
		// activity
		UiModeManager manager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
		manager.disableCarMode(0);

		super.onDestroy();
	}

	/*
	 * PFDock.onPause - Handle lifecycle events for pause
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();
		Log.i(getPackageName(), "PFDock.onPause");

		// Disconnect the broadcast receiver so car dock events will cease
		if (m_receiver != null) {
			unregisterReceiver(m_receiver);
			m_receiver = null;
		}

		// Disconnect the GPS when paused
		if (m_locationListener != null) {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			lm.removeUpdates(m_locationListener);
		}

		// Save the component selections that the user might've changed
		putAppPreferences();

		// Disconnect other receivers
		this.unregisterReceiver(m_batteryReceiver);
		this.unregisterReceiver(m_timeReceiver);
		this.unregisterReceiver(m_carDockExitReceiver);
		this.unregisterReceiver(m_batteryLowReceiver);
	}

	/*
	 * PFDock.onResume
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.i(getPackageName(), "PFDock.onResume");

		// Reload the preferences and set anything that needs them
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String default_units = getString(R.string.default_units);
		String units = preferences.getString("speed_units", default_units);
		m_imperial = units.contentEquals(default_units);
		m_coarseGPS = preferences.getBoolean("coarse_gps", true);

		// Only allow GPS to operate continuously if the unit is plugged in
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		boolean network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		boolean gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

		// If the user requests coarse GPS and the network is running, use network.
		// Also use network if the GPS isn't enabled and the network is enabled
		if ((m_coarseGPS && network_enabled) || (!gps_enabled && network_enabled))
		{
			// Issue #5 -  http://github.com/ehcloninger/pf-public/issues/issue/5
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 5,
					m_locationListener);
			
			if (m_providerIcon != null)
				m_providerIcon.setImageResource(R.drawable.ic_network);
		}
		else
		{
			if (gps_enabled)
			{
				if (m_isPluggedIn)
					lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 5,
							m_locationListener);
				else
					lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 100,
							m_locationListener);
				if (m_providerIcon != null)
					m_providerIcon.setImageResource(R.drawable.ic_gps);
			}
			else
			{
				// you're screwed. No location provider found
			}
		}
		
		// Reconnect broadcast receivers
		this.registerReceiver(m_batteryReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		this.registerReceiver(m_timeReceiver, new IntentFilter(
				Intent.ACTION_TIME_TICK));
		this.registerReceiver(m_receiver, new IntentFilter(
				UiModeManager.ACTION_ENTER_CAR_MODE));
		this.registerReceiver(m_carDockExitReceiver, new IntentFilter(
				UiModeManager.ACTION_EXIT_CAR_MODE));
		this.registerReceiver(m_batteryLowReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_LOW));

		// Using the most recent saved location, try to guess where we were
		// when we were paused or quit
		updateLocationUI(m_lastLocation);

		// Set the time
		if (m_timeView != null)
			m_timeView.setText(getSimpleTimeString());

		this.setIconsColor(preferences);
		this.setTextColor(preferences);
		this.setAppPreferences(preferences);
	}

	/*
	 * GetComponentInfo - Retrieve values of the 3 user overrideable buttons
	 */
	void setAppPreferences(SharedPreferences preferences) {

		m_components[0].name = preferences.getString("component_name_places",
				"com.google.android.apps.maps");
		m_components[0].category = preferences.getString(
				"component_category_places",
				"com.google.android.maps.PlacesActivity");
		m_components[1].name = preferences.getString("component_name_music",
				"com.google.android.music");
		m_components[1].category = preferences.getString(
				"component_category_music",
				"com.android.music.activitymanagement.TopLevelActivity");
		m_components[2].name = preferences.getString("component_name_voice",
				"com.google.android.voicesearch");
		m_components[2].category = preferences.getString(
				"component_category_voice",
				"com.google.android.voicesearch.RecognitionActivity");
	}

	/*
	 * PutComponentInfo - Write the values of the user overrideable buttons into
	 * shared preferences
	 */
	void putAppPreferences() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = preferences.edit();

		editor.putString("component_name_places", m_components[0].name);
		editor.putString("component_category_places", m_components[0].category);
		editor.putString("component_name_music", m_components[1].name);
		editor.putString("component_category_music", m_components[1].category);
		editor.putString("component_name_voice", m_components[2].name);
		editor.putString("component_category_voice", m_components[2].category);

		editor.putBoolean("first_time", false);

		// Commit the edits!
		editor.commit();
	}

	/*
	 * PFDock.onCreate
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		for (int i = 0; i < 3; i++)
			m_components[i] = new ComponentInfo();

		Log.i(getPackageName(), "PFDock.onCreate");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		setAppPreferences(preferences);

		/*
		 * PFDock.onSharedPreferenceChanged
		 * 
		 * Listener to detect when the user has changed a preference
		 */
		preferences
				.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
					public void onSharedPreferenceChanged(
							SharedPreferences sharedPreferences, String key) {
						if (key.contentEquals("icon_color"))
							PFDock.this.setIconsColor(sharedPreferences);
						if (key.contentEquals("text_color"))
							PFDock.this.setTextColor(sharedPreferences);
					}
				});

		// Define colors and icon IDs used in setIconsColor. Can't store these
		// in a database as the values of the icons change each build. If you
		// add new colors, add an entry in this map to go along with the name
		// in the strings.xml, colors_dont_localize
		m_colorIcons.put("white", new int[] { R.drawable.ics_places_white,
				R.drawable.ics_music_white, R.drawable.ics_voicesearch_white,
				R.drawable.ics_phone_white, R.drawable.ics_maps_white,
				R.drawable.ics_home_white });
		m_colorIcons.put("red", new int[] { R.drawable.ics_places_red,
				R.drawable.ics_music_red, R.drawable.ics_voicesearch_red,
				R.drawable.ics_phone_red, R.drawable.ics_maps_red,
				R.drawable.ics_home_red });
		m_colorIcons.put("blue", new int[] { R.drawable.ics_places_blue,
				R.drawable.ics_music_blue, R.drawable.ics_voicesearch_blue,
				R.drawable.ics_phone_blue, R.drawable.ics_maps_blue,
				R.drawable.ics_home_blue });
		m_colorIcons.put("green", new int[] { R.drawable.ics_places_green,
				R.drawable.ics_music_green, R.drawable.ics_voicesearch_green,
				R.drawable.ics_phone_green, R.drawable.ics_maps_green,
				R.drawable.ics_home_green });
		m_colorIcons.put("yellow", new int[] { R.drawable.ics_places_yellow,
				R.drawable.ics_music_yellow, R.drawable.ics_voicesearch_yellow,
				R.drawable.ics_phone_yellow, R.drawable.ics_maps_yellow,
				R.drawable.ics_home_yellow });
		m_colorIcons.put("orange", new int[] { R.drawable.ics_places_orange,
				R.drawable.ics_music_orange, R.drawable.ics_voicesearch_orange,
				R.drawable.ics_phone_orange, R.drawable.ics_maps_orange,
				R.drawable.ics_home_orange });
		m_colorIcons.put("purple", new int[] { R.drawable.ics_places_purple,
				R.drawable.ics_music_purple, R.drawable.ics_voicesearch_purple,
				R.drawable.ics_phone_purple, R.drawable.ics_maps_purple,
				R.drawable.ics_home_purple });

		// These are the colors for the text components
		m_colorText.put("white", Color.rgb(255, 255, 255));
		m_colorText.put("red", Color.rgb(255, 0, 0));
		m_colorText.put("blue", Color.rgb(0, 0, 255));
		m_colorText.put("green", Color.rgb(0, 255, 0));
		m_colorText.put("yellow", Color.rgb(255, 255, 0));
		m_colorText.put("orange", Color.rgb(255, 153, 51));
		m_colorText.put("purple", Color.rgb(255, 102, 255));

		boolean firstTime = preferences.getBoolean("first_time", true);
		if (firstTime) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.warning_msg)
					.setCancelable(false)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
								}
							}).setTitle(R.string.no_jerk);
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}

		// Set the Activity to receive events for car dock
		IntentFilter filter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
		m_receiver = new CarDockReceiver();
		registerReceiver(m_receiver, filter);

		m_timeView = (TextView) this.findViewById(R.id.time);
		m_speedView = (TextView) this.findViewById(R.id.speed);
		m_batteryView = (TextView) this.findViewById(R.id.battery);
		m_providerIcon = (ImageView) this.findViewById(R.id.provider);

		if (m_timeView != null)
			m_timeView.setText(getSimpleTimeString());

		if (m_speedView != null)
			m_speedView.setText(" ");

		if (m_batteryView != null)
			m_batteryView.setText(" ");

		// Set up long-click listeners for the 3 user-overrideable buttons
		// #1 - Places
		m_placesBtn = (ImageButton) this.findViewById(R.id.places);
		if (m_placesBtn != null) {
			m_placesBtn.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					// There's certainly a better way to do this. But for now,
					// set a storage variable (m_currentComponent)
					// to hold the value of the current data going to and from
					// the dialog and then
					// reassign it when it returns. In the dialog handler, if
					// the user presses, OK
					// pull the data out of the edit fields, otherwise do
					// nothing and the reassignment
					// has no damage.
					// TODO: Learn the proper way to communicate with dialogs
					m_currentComponent = m_components[0];
					PFDock.this.showDialog(COMPONENT_PICKER);
					m_components[0] = m_currentComponent;
					return true;
				}
			});
		}

		// #2 - Music
		m_musicBtn = (ImageButton) this.findViewById(R.id.music);
		if (m_musicBtn != null) {
			m_musicBtn.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					m_currentComponent = m_components[1];
					PFDock.this.showDialog(COMPONENT_PICKER);
					m_components[1] = m_currentComponent;
					return true;
				}
			});
		}

		// #3 - Voice Search
		m_voiceBtn = (ImageButton) this.findViewById(R.id.voicesearch);
		if (m_voiceBtn != null) {
			m_voiceBtn.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					m_currentComponent = m_components[2];
					PFDock.this.showDialog(COMPONENT_PICKER);
					m_components[2] = m_currentComponent;
					return true;
				}
			});
		}

		// Add buttons for phone, maps, and home
		m_phoneBtn = (ImageButton) this.findViewById(R.id.phone);
		m_mapsBtn = (ImageButton) this.findViewById(R.id.googlemaps);
		m_homeBtn = (ImageButton) this.findViewById(R.id.home);

		this.setIconsColor(preferences);
		this.setTextColor(preferences);
	}

	/*
	 * PFDock.setIconsColor -
	 * 
	 * Set the color of the icons on the main screen, based on user choice in
	 * preferences. Values are stored in a hashmap initialized in onCreate
	 */
	void setIconsColor(SharedPreferences preferences) {
		String defaultColor = getApplicationContext().getString(
				R.string.default_icon_color);
		String prefColor = preferences.getString("icon_color", defaultColor);
		int[] values = (int[]) m_colorIcons.get(prefColor);
		if (values != null && values.length == 6) {
			m_placesBtn.setImageResource(values[0]);
			m_musicBtn.setImageResource(values[1]);
			m_voiceBtn.setImageResource(values[2]);
			m_phoneBtn.setImageResource(values[3]);
			m_mapsBtn.setImageResource(values[4]);
			m_homeBtn.setImageResource(values[5]);
		}
		else
		{
			// If, for whatever reason, the colors don't exist, revert to purple
			m_placesBtn.setImageResource(R.drawable.ics_places_purple);
			m_musicBtn.setImageResource(R.drawable.ics_music_purple);
			m_voiceBtn.setImageResource(R.drawable.ics_voicesearch_purple);
			m_phoneBtn.setImageResource(R.drawable.ics_phone_purple);
			m_mapsBtn.setImageResource(R.drawable.ics_maps_purple);
			m_homeBtn.setImageResource(R.drawable.ics_home_purple);
		}
	}

	/*
	 * PFDock.setTextColor -
	 * 
	 * Set the color of the text on the main screen, based on user choice in
	 * preferences.
	 */
	void setTextColor(SharedPreferences preferences) {
		String defaultColor = getApplicationContext().getString(
				R.string.default_text_color);
		String prefColor = preferences.getString("text_color", defaultColor);
		Integer value = m_colorText.get(prefColor);

		// Pull the color out of the table. if no value found, revert to white text
		if (value != null) {
			m_timeView.setTextColor(value.intValue());
			m_speedView.setTextColor(value.intValue());
			m_batteryView.setTextColor(value.intValue());
		}
		else
		{
			m_timeView.setTextColor(0xffffff);
			m_speedView.setTextColor(0xffffff);
			m_batteryView.setTextColor(0xffffff);
		}
	}

	/*
	 * PFDock.onCreateDialog - Handle the modal dialogs that arise from PFDock
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	public Dialog onCreateDialog(int id) {
		switch (id) {
		/*
		 * COMPONENT_PICKER occurs when the user long-presses on one of the 3
		 * user-overrideable buttons (places, music, voice-search). The eventual
		 * goal is to give them the ability to search for an app to launch, but
		 * for now, just text enter the component name and category (ick).
		 */
		case COMPONENT_PICKER:
			LayoutInflater li = LayoutInflater.from(this);
			final View cpd = li.inflate(R.layout.componentdialog, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.component_picker_title));
			builder.setView(cpd);
			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							EditText et;
							et = (EditText) cpd
									.findViewById(R.id.component_name);
							// TODO: Validate contents of edit field before
							// assigning back to structure
							if ((et != null) && (m_currentComponent != null))
								m_currentComponent.name = et.getText()
										.toString();
							et = (EditText) cpd
									.findViewById(R.id.component_class);
							if ((et != null) && (m_currentComponent != null))
								m_currentComponent.category = et.getText()
										.toString();
							dialog.dismiss();
						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			return builder.create();
		}
		return null;

	}

	/*
	 * PFDock.onPrepareDialog - Pushes data into an awaiting dialog
	 * 
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case COMPONENT_PICKER:
			EditText et;
			et = (EditText) dialog.findViewById(R.id.component_name);
			if ((et != null) && (m_currentComponent != null))
				et.setText(m_currentComponent.name);
			et = (EditText) dialog.findViewById(R.id.component_class);
			if ((et != null) && (m_currentComponent != null))
				et.setText(m_currentComponent.category);
			break;

		default:
			return;
		}
	}

	/*
	 * PFDock.onRestoreInstanceState - lifecycle events that cause the Activity
	 * to go away, such as rotation, phone calls, etc.
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.i(getPackageName(), "PFDock.onRestoreInstanceState");

		double latitude = savedInstanceState.getDouble("last_latitude");
		double longitude = savedInstanceState.getDouble("last_longitude");

		m_lastLocation.setLatitude(latitude);
		m_lastLocation.setLongitude(longitude);
	}

	/*
	 * PFDock.onSaveInstanceState
	 * 
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	protected void onSaveInstanceState(Bundle outState) {
		// Save the last known latitude and longitude for fast restarts of the
		// maps service
		Log.i(getPackageName(), "PFDock.onSaveInstanceState");
		outState.putDouble("last_latitude", m_lastLocation.getLatitude());
		outState.putDouble("last_longitude", m_lastLocation.getLongitude());
		super.onSaveInstanceState(outState);
	}

	/*
	 * getSimpleTimeString - Create a string for display in either 12 hour or 24
	 * hour format
	 */
	private String getSimpleTimeString() {
		String formatString = "hh:mm a";
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		if (preferences.getBoolean("clock_24_hour", false)) {
			formatString = "HH:mm";
		}

		SimpleDateFormat df = new SimpleDateFormat(formatString);
		String s = df.format(new Date());

		// remove leading '0' if it exists
		if (s.charAt(0) == '0')
			s = s.substring(1);

		return s;
	}

	/*
	 * btnClickedGoogleMaps - Operate when user presses the Google maps button
	 */
	public void btnClickedGoogleMaps(View theButton) {
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Start by trying to get close enough. Maybe it's a little out of date,
		// but it might be good enough to get Google Maps started
		Location location = m_lastLocation;
		double latitude = m_lastLocation.getLatitude();
		double longitude = m_lastLocation.getLongitude();

		if (locationManager != null) {
			locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
			}
		}

		// Create the URL and launch. This should launch Google maps if
		// installed, but it
		// will go to whichever provider the user has chosen to satisfy the geo:
		// Intent
		String uri = "geo:" + ((float) latitude) + "," + ((float) longitude);
		try {
			startActivity(new Intent(android.content.Intent.ACTION_VIEW,
					Uri.parse(uri)));
		} catch (ActivityNotFoundException e) {
			Toast toast = Toast.makeText(getApplicationContext(),
					R.string.no_maps_service, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	/*
	 * btnClickedHome - Quit this app and go to home screen.
	 */
	public void btnClickedHome(View theButton) {
		// Determine if we should ask the user first or just quit
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		if (preferences.getBoolean("confirm_exit", true)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.confirm_exit_to_homescreen)
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									PFDock.this.finish();
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		} else {
			// Just quit already
			this.finish();
		}
	}

	/*
	 * btnClickedPhone - Go to phone dialer
	 */
	public void btnClickedPhone(View theButton) {
		Intent iImp = new Intent(Intent.ACTION_DIAL);
		try {
			startActivity(iImp);
		} catch (ActivityNotFoundException e) {
			Toast toast = Toast.makeText(getApplicationContext(),
					R.string.no_dialer_service, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	/*
	 * LaunchCustomServiceButton - Used to handle the launch of one of the 3
	 * custom buttons (places, music, voice search) in a generic way.
	 */
	protected void launchCustomServiceButton(String cmpName, String cmpClass) {
		final Intent intent = new Intent(Intent.CATEGORY_LAUNCHER);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		final ComponentName cn = new ComponentName(cmpName, cmpClass);
		intent.setComponent(cn);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast toast = Toast.makeText(getApplicationContext(),
					R.string.no_such_service, Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	/*
	 * btnClickedPlaces - Button click handler for places. Referenced from the
	 * XML
	 */
	public void btnClickedPlaces(View theButton) {
		launchCustomServiceButton(m_components[0].name,
				m_components[0].category);
	}

	/*
	 * btnClickedMusic
	 */
	public void btnClickedMusic(View theButton) {
		launchCustomServiceButton(m_components[1].name,
				m_components[1].category);
	}

	/*
	 * btnClickedVoiceSearch
	 */
	public void btnClickedVoiceSearch(View theButton) {
		launchCustomServiceButton(m_components[2].name,
				m_components[2].category);
	}

	/*
	 * PFDock.onCreateOptionsMenu
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main, menu);
		Log.i(getPackageName(), "PFDock.onCreateOptionsMenu");

		return (super.onCreateOptionsMenu(menu));
	}

	/**
	 * Define menu action
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		Log.i(getPackageName(), "PFDock.onOptionsItemSelected");
		if (item.getItemId() == R.id.settings) {
			// Show the preferences dialog (via a separate class)
			startActivity(new Intent(this, PFDockPreferences.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
