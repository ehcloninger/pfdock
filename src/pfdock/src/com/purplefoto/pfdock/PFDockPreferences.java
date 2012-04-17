/*
Copyright 2012 Eric H. Cloninger, dba PurpleFoto
 
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

import android.os.Bundle;
import android.preference.PreferenceActivity;

/*
 * PFDockPreferences - minimalist class that exists just to define the preferences
 */
public class PFDockPreferences extends PreferenceActivity {
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
