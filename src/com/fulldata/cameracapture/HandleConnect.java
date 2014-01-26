package com.fulldata.cameracapture;

import java.io.*;
import java.net.*;

import android.annotation.SuppressLint;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.Camera.*;
import android.util.Log;

public class HandleConnect extends Thread {
	ServerSocket mListen_sck;

	public void Close() {
		try {
			mListen_sck.close();
		} catch (IOException e) {
		}
	}

	public boolean Listening(int port) {
		try {
			mListen_sck = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void run() {
		ProcHandleConnect();
	}

	public void ProcHandleConnect() {
		while (true) {
			Socket data_sck;
			try {
				data_sck = mListen_sck.accept();
			} catch (IOException e1) {
				break;
			}
			try {
				if (!this.viewCamera(data_sck)) {
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
	public boolean viewCamera(final Socket data_sck) {

		// Read Socket Command

		int cameraindex = 0;
		boolean backCamera = true;
		try {
			BufferedReader is = null;
			BufferedWriter os = null;
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
		} catch (Exception e) {
			return false;
		}

		// Open Camera, Set Camera
		Camera camera = null;
		SurfaceTexture st = new SurfaceTexture(0);
		try {
			camera = Camera.open(cameraindex);
			camera.setDisplayOrientation(90);

			try {
				Parameters p = camera.getParameters();
				if (p != null) {
					if (backCamera) {
						p.setFlashMode(Parameters.FLASH_MODE_OFF);
						p.set("orientation", "portrait");
						p.setRotation(90);
					} else {
						p.set("orientation", "portrait");
						p.setRotation(270);
					}
					p.setPreviewFormat(MIN_PRIORITY);
					p.setPreviewFpsRange(1, 2);

					camera.setParameters(p);
				}
			} catch (Exception e) {
			}
			camera.setPreviewTexture(st);
		} catch (Exception e1) {
			if(camera!=null)
			{
				camera.release();
			}
			return false;
		}

		Size size = camera.getParameters().getPreviewSize();
		final int wide = size.width;
		final int high = size.height;
		
		// Start Camera
		PreviewCallback PreviewCb = new PreviewCallback() {
			public void onPreviewFrame(byte[] data, Camera camera) {
				try {
					OutputStream os = data_sck.getOutputStream();
					YuvImage image = new YuvImage(data, ImageFormat.NV21, wide,
							high, null);
					ByteArrayOutputStream bos = new ByteArrayOutputStream(
							data.length);
					if (image.compressToJpeg(new Rect(0, 0, wide, high), 100,
							bos)) {
						byte[] cdata = bos.toByteArray();
						DataPack.sendDataPack(cdata, os);
					}
				} catch (Exception e) {
					try {
						data_sck.close();
					} catch (IOException e1) {
					}
					camera.release();
				}
			}
		};

		camera.startPreview();
		camera.autoFocus(null);
		camera.setPreviewCallback(PreviewCb);
		
		byte[] command = new byte[100];
		try {
			InputStream is = data_sck.getInputStream();
			while(is.read(command)!=-1)
			{
				;
			}
		} catch (IOException e) {
			try {
				data_sck.close();
				camera.release();
			} catch (IOException e1) {
			}
		}

		
		// camera.autoFocus(new AutoFocusCallback() {
		// @Override
		// public void onAutoFocus(boolean success, Camera camera) {
		// if (success) {
		// PictureCallback pcb = new PictureCallback() {
		// @Override
		// public void onPictureTaken(byte[] data, Camera camera) {
		// try {
		// OutputStream os = data_sck.getOutputStream();
		// os.write(data);
		// os.flush();
		// } catch (IOException e1) {
		// }
		//
		// try {
		// data_sck.close();
		// } catch (IOException e) {
		// }
		// camera.release();
		// }
		// };
		//
		// try {
		// camera.takePicture(null, null, pcb);
		// } catch (Exception e) {
		// camera.release();
		// }
		// }
		// }
		//
		// });

		return true;
	}

}
