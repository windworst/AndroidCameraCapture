package com.fulldata.cameracapture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class CameraServerActivity extends Activity implements OnClickListener, OnSeekBarChangeListener {

	int mPort = 6666;
	int mQuality = 70;
	Button mBtn;
	EditText mEdittext;
	TextView mIpTextView = null;
	boolean mIsStart = false;
	SeekBar mQualitySeekBar = null;
	TextView mQualityTextView = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		mBtn = (Button) findViewById(R.id.startListen);
		mBtn.setOnClickListener(this);

		mEdittext = (EditText) findViewById(R.id.listenPort);
		mEdittext.setText(Integer.toString(mPort));
		
		mQualitySeekBar = (SeekBar) findViewById(R.id.qualitySeekBar);
		mQualitySeekBar.setMax(100);
		mQualitySeekBar.setProgress(mQuality);
		mQualitySeekBar.setOnSeekBarChangeListener(this);
		
		mQualityTextView = (TextView) findViewById(R.id.qualityTextView);
		mQualityTextView.setText(""+mQuality);
		

		mIpTextView = (TextView) findViewById(R.id.localIP);
		
		setLocalIp();
	}

	public void onDestroy() {
		if (mIsStart)
			Stop();
		super.onDestroy();
	}
	
	private String intToIp(int i) {

		return (i & 0xFF) + "." +

		((i >> 8) & 0xFF) + "." +

		((i >> 16) & 0xFF) + "." +

		(i >> 24 & 0xFF);

	}

	public String getLocalIpAddress() {
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		if (!wifiManager.isWifiEnabled()) {

			wifiManager.setWifiEnabled(true);

		}

		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		int ipAddress = wifiInfo.getIpAddress();

		return intToIp(ipAddress);
	}
	
	public void setLocalIp()
	{
		try {
			String ip = getLocalIpAddress();
			if (ip == null) {
				ip = "0.0.0.0";
			}
			mIpTextView.setText(ip);
		} catch (Exception e) {
			;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Exit");
		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			if (mIsStart)
				Stop();
			finish();
			return true;
		default:
			break;
		}
		return false;
	}

	void Start() {
		Intent serviceIntent = new Intent(CameraServerActivity.this,
				CameraServerService.class);
		serviceIntent.putExtra("PORT_VALUE", mPort);
		serviceIntent.putExtra("QUALITY_VALUE",mQuality);
		startService(serviceIntent);
	}

	void Stop() {
		Intent serviceIntent = new Intent(CameraServerActivity.this,
				CameraServerService.class);
		stopService(serviceIntent);
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addCategory(Intent.CATEGORY_HOME);
		startActivity(i);
	}

	void ClickButton() {
		if (mIsStart) {
			mBtn.setText("Listen");
			mEdittext.setEnabled(true);
			mQualitySeekBar.setEnabled(true);
			Stop();
			mIsStart = false;
		} else {
			mPort = Integer.parseInt(mEdittext.getText().toString());
			if (0 < mPort && mPort < 65536) {
				mBtn.setText("Stop");
				setLocalIp();
				mQualitySeekBar.setEnabled(false);
				mEdittext.setEnabled(false);
				Start();
			} else {
				Toast.makeText(getApplicationContext(), "Listen Failed",
						Toast.LENGTH_SHORT).show();
			}
			mIsStart = true;
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.startListen:
			ClickButton();
			break;
		default:
			break;
		}
	}
	
	void SetSeekBar(int value)
	{
		mQuality = value;
		int minValue = 5;
		if(mQuality<minValue)
		{
			mQuality = minValue;
		}		
		mQualitySeekBar.setProgress(mQuality);
		mQualityTextView.setText(""+mQuality);
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		SetSeekBar(arg1);
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		
	}
}
