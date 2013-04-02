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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

	public class ResetComponentsDialogPreference extends DialogPreference {

	    public ResetComponentsDialogPreference(Context context, AttributeSet attrs) {
	        super(context, attrs);
	        // TODO Auto-generated constructor stub
	    }

	    protected void onDialogClosed (boolean positiveResult)
	    {
	    	if (positiveResult)
	    	{
    			SharedPreferences preferences = PreferenceManager
    					.getDefaultSharedPreferences(getContext());
    			SharedPreferences.Editor editor = preferences.edit();

    			editor.putString("component_name_places",  "com.google.android.apps.maps");
    			editor.putString("component_category_places", "com.google.android.maps.PlacesActivity");
    			editor.putString("component_name_music", "com.google.android.music");
    			editor.putString("component_category_music", "com.android.music.activitymanagement.TopLevelActivity");
    			editor.putString("component_name_voice", "com.google.android.voicesearch");
    			editor.putString("component_category_voice", "com.google.android.voicesearch.RecognitionActivity");

    			editor.putBoolean("first_time", false);

    			// Commit the edits!
    			editor.commit();
    		}
	    }
	} 
