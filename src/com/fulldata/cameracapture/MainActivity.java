package com.fulldata.cameracapture;


import android.app.Activity;
import android.content.pm.ActivityInfo;

import android.os.Bundle;


public class MainActivity extends Activity  {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		//Button btn = (Button) findViewById(R.id.button1);
		//btn.setOnClickListener(this);
		HandleConnect hc = new HandleConnect();
		if (hc.Listening(5555))
		{
			hc.start();
		}
	}


}
