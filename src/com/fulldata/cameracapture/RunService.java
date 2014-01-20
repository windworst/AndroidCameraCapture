package com.fulldata.cameracapture;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class RunService extends Service{
	HandleConnect hc;
	static boolean isStart;
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	 @Override  
	 public int onStartCommand(Intent intent, int flags, int startId) {  
		if(!RunService.isStart)
		{
			int port = intent.getIntExtra(DOWNLOAD_SERVICE,0);
			hc = new HandleConnect();
			if (hc.Listening(port))
			{
				RunService.isStart = true;
				hc.start();
			}
		}
	    return super.onStartCommand(intent, flags, startId);  
	 }
	 
	 @Override
	 public void onDestroy(){
		 try {
			hc.Close();
			hc.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 RunService.isStart = false;
	 }
}
