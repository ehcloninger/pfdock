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

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * CarDockReceiver - Handles the mechanics of when the user mounts the device in the car
 * dock and removes it. Forces the device so that purpledock is the default home app for
 * the duration unless they quit
 */
public class CarDockReceiver extends BroadcastReceiver {
	public static int EXTRA_DOCK_STATE_UNDOCKED = 0;
	public static int EXTRA_DOCK_STATE_CAR = 1;
	/**
	 * @see android.content.BroadcastReceiver#onReceive(Context,Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		// This code is courtesy of StackOverflow
		if (intent.getExtras().containsKey("android.intent.extra.DOCK_STATE")) {
			UiModeManager ui = (UiModeManager) context
					.getSystemService(Context.UI_MODE_SERVICE);
			int state = intent.getExtras().getInt(
					"android.intent.extra.DOCK_STATE", -1);
			if (state == EXTRA_DOCK_STATE_CAR) {
				if (ui != null)
					ui.enableCarMode(0);
			} else if (state == EXTRA_DOCK_STATE_UNDOCKED) {
				if (ui != null)
					ui.disableCarMode(0);
				// TODO Remove the comment below for production to prevent this
				// app from running except when docked
				// ((Activity) context).finish();
			}
		}
	}
}
