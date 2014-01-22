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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	static int port;
	Button btn;
	EditText edittext;

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Exit");
		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			finish();
			return true;
		default:
			break;
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		btn = (Button) findViewById(R.id.startListen);
		btn.setOnClickListener(this);

		edittext = (EditText) findViewById(R.id.listenPort);

		TextView tv = (TextView) findViewById(R.id.localIP);
		try {
			String ip = getLocalIpAddress();
			if (ip != null) {

				tv.setText(ip);
			}
		} catch (Exception e) {
			;
		}

		if (RunService.isStart) {
			edittext.setEnabled(false);
			edittext.setText(Integer.toString(port));
			btn.setText("Stop");
		}
	}

	void Start() {
		Intent serviceIntent = new Intent(MainActivity.this, RunService.class);
		serviceIntent.putExtra(DOWNLOAD_SERVICE, port);
		startService(serviceIntent);
	}

	void Stop() {
		Intent serviceIntent = new Intent(MainActivity.this, RunService.class);
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
		if (RunService.isStart) {
			btn.setText("Listen");
			edittext.setEnabled(true);
			Stop();
		} else {
			port = Integer.parseInt(edittext.getText().toString());
			if (0 < port && port < 65536) {
				btn.setText("Stop");
				edittext.setEnabled(false);
				Start();
			} else {
				Toast.makeText(getApplicationContext(), "Listen Failed",
						Toast.LENGTH_SHORT).show();
			}

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
}
