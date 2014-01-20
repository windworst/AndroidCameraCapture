package com.fulldata.cameracapture;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity implements OnClickListener  {
	
	static int port;
	Button btn;
	EditText edittext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		btn = (Button) findViewById(R.id.button1);
		btn.setOnClickListener(this);
		
		edittext = (EditText) findViewById(R.id.editText1);
		
		
		if(RunService.isStart)
		{
			edittext.setEnabled(false);
			edittext.setText(Integer.toString(port));
			btn.setText("Stop");
		}
	}
	
	void Start()
	{
		Intent serviceIntent = new Intent(MainActivity.this,RunService.class);
		serviceIntent.putExtra(DOWNLOAD_SERVICE, port);
		startService(serviceIntent);
	}
	
	void Stop()
	{
		Intent serviceIntent = new Intent(MainActivity.this,RunService.class);
		stopService(serviceIntent);
	}
	
	void ClickButton()
	{
		if(RunService.isStart)
		{
			btn.setText("Listen");
			edittext.setEnabled(true);
			Stop();
		}
		else
		{	
			port = Integer.parseInt(edittext.getText().toString());
			if(0<port && port<65536)
			{
				btn.setText("Stop");
				edittext.setEnabled(false);
				Start();
			}
			else
			{
				Toast.makeText(getApplicationContext(),"Listen Failed",Toast.LENGTH_SHORT).show();
			}
			
		}
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId())
		{
		case R.id.button1:
			ClickButton();
			break;
		default: break;
		}
	}
}
