package com.fulldata.cameracapture;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.util.Log;

public class HandleConnect extends Thread {
	ServerSocket listen_sck;

	public void Close() {
		try {
			listen_sck.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean Listening(int port) {
		try {
			listen_sck = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void ProcHandleConnect() {
		while (true) {
			Socket data_sck;
			try {
				data_sck = listen_sck.accept();
			} catch (IOException e1) {
				break;
			}
			try {
				if (!this.catchCamera(data_sck)) {
					data_sck.close();
				}
			} catch (Exception e) {
			}
		}

	}

	// Get Camera Index
	@SuppressLint("NewApi")
	private int FindCamera(boolean back) {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras(); // get cameras number
		int value = Camera.CameraInfo.CAMERA_FACING_FRONT;
		if (back) {
			value = Camera.CameraInfo.CAMERA_FACING_BACK;
		}
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
			if (cameraInfo.facing == value) {
				return camIdx;
			}
		}
		return -1;
	}

	// Main Func of Catch Camera
	@SuppressLint("NewApi")
	public boolean catchCamera(final Socket data_sck) {

		BufferedReader is = null;
		BufferedWriter os = null;
		int cameraindex = 0;
		boolean backCamera = true;
		try {
			is = new BufferedReader(new InputStreamReader(
					data_sck.getInputStream()));
			os = new BufferedWriter(new OutputStreamWriter(
					data_sck.getOutputStream()));

			char[] buf = new char[100];
			if (is.read(buf) > 0) {
				backCamera = (buf[0] != '0');
				cameraindex = FindCamera(backCamera);
				if (cameraindex == -1) {
					os.write("error");
					os.flush();
					return false;
				}
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return false;
		} finally {
		}

		Camera camera = null;
		try {
			camera = Camera.open(cameraindex);
			camera.setDisplayOrientation(90);

			// Front Camera Can not be set
			try {
				Parameters p = camera.getParameters();
				if (p != null) {
					if(backCamera)
					{
						p.setFlashMode(Parameters.FLASH_MODE_OFF);
						p.set("orientation", "portrait");
						p.setRotation(90);
					}
					else
					{
						p.set("orientation", "portrait");
						p.setRotation(270);
					}

					camera.setParameters(p);
				}
			} catch (Exception e) {
				Log.v("setParameters", e.getMessage());
			}
			camera.setPreviewTexture(new SurfaceTexture(0));
		} catch (IOException e1) {
			return false;
		}

		camera.startPreview();
		camera.autoFocus(new AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				if (success) {
					camera.cancelAutoFocus();
				}
			}

		});

		try {
			camera.takePicture(null, null, new PictureCallback() {

				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					// TODO Auto-generated method stub
					
					//turn 90 jpg data
					/*
					Bitmap bm0 = BitmapFactory.decodeByteArray(data, 0, data.length);
				      Matrix m = new Matrix();
				      //m.setRotate(90,(float) bm0.getWidth(), (float) bm0.getHeight());
				      final Bitmap bm = Bitmap.createBitmap(bm0, 0, 0, bm0.getWidth(), bm0.getHeight(), m, true);
				      //*/
				      
					try {
						OutputStream os = data_sck.getOutputStream();
						os.write(data);
						//bm.compress(CompressFormat.JPEG, MAX_PRIORITY, os);
						os.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block

					} finally {

					}

					try {
						data_sck.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					camera.release();
				}
			});
		} catch (Exception e) {
			camera.release();
			return false;
		}
		return true;
	}

	public void run() {
		ProcHandleConnect();
	}

}
