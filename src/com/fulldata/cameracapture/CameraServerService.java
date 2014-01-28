package com.fulldata.cameracapture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class CameraServerService extends Service{
	CameraServer mCs;
	WakeLock mWakeLock = null;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		 
		int port = intent.getIntExtra("PORT_VALUE", 0);
		mCs = new CameraServer();
		if (mCs.Listening(port)) {
			mCs.start();
		}
		
		acquireWakeLock();
		super.onStartCommand(intent, flags, startId);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		releaseWakeLock();
		mCs.onDestroy();
		try {
			mCs.join();
		} catch (Exception e) {
		}
	}

	private void acquireWakeLock() {
		
		if (null == mWakeLock) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, "LOCK");
			if (null != mWakeLock) {
				mWakeLock.acquire();
			}
		}
	}

	private void releaseWakeLock() {
		if (null != mWakeLock) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}

}
