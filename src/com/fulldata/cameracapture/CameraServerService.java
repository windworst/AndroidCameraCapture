package com.fulldata.cameracapture;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class CameraServerService extends Service {
	CameraServer mCs = null;
	WakeLock mWakeLock = null;

	Thread mUdpBroadCastThread = null;
	boolean StillbroadCast = false;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		final int port = intent.getIntExtra("PORT_VALUE", 0);

		Runnable mBroadcastRunner = new Runnable() {
			@Override
			public void run() {
				StillbroadCast = true;
				DatagramSocket ds = null;
				try {

					byte[] data = "I'm Here, as always.".getBytes();
					DatagramPacket sendPacket = new DatagramPacket(data,
							data.length,
							InetAddress.getByName("255.255.255.255"), port);
					ds = new DatagramSocket();
					while (StillbroadCast) {
						try {
							
							ds.send(sendPacket);
						} catch (Exception e) {
						}
						Thread.sleep(1000);

					}

				} catch (Exception e) {
				}
				finally
				{
					if(ds!=null)
					{
						ds.close();
					}
				}
			}
		};

		mUdpBroadCastThread = new Thread(mBroadcastRunner);
		mUdpBroadCastThread.start();

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
		StillbroadCast = false;
		try {
			mCs.join();
			mUdpBroadCastThread.join();
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
