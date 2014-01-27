package com.fulldata.cameracapture;

import java.io.*;
import java.net.*;

import android.annotation.SuppressLint;
import android.graphics.*;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.hardware.Camera.*;
import android.util.Log;

public class CameraServer extends Thread {
	ServerSocket mListen_sck = null;
	Socket mData_sck = null;
	Camera mCamera = null;
	int mQuality = 100;

	static public void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width,
			int height) {
		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}
	
	public double getLight(int rgb[])
	{
		int i;
		double bright = 0;
		for(i=0;i<rgb.length;++i)
		{
			int localTemp = rgb[i];
			int r = (localTemp | 0xff00ffff) >> 16 & 0x00ff;  
        	int g = (localTemp | 0xffff00ff) >> 8 & 0x0000ff;  
        	int	b = (localTemp | 0xffffff00) & 0x0000ff;  
        	bright = bright + 0.299 * r + 0.587 * g + 0.114 * b;  
		}
		return bright / rgb.length ;
	}

	public void onDestroy() {
		if (mData_sck != null) {
			try {
				mData_sck.close();
			} catch (IOException e) {
			}
			mData_sck = null;
		}
		StopListen();
		CloseCamera();
	}

	public void CloseCamera() {
		if (mCamera != null) {
			try {
				mCamera.setPreviewCallback(null);		
			} catch (Exception e) {
			}
			finally
			{
				mCamera.release();
			}
			mCamera = null;
		}
	}

	boolean sendDataPack(byte[] data, OutputStream os) {
		int len = data.length;
		DataOutputStream dos = new DataOutputStream(os);
		try {
			dos.writeInt(len);
			dos.write(data);
			return true;
		} catch (IOException e) {
		}
		return false;
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

	public void StopListen() {
		if (mListen_sck != null) {
			try {
				mListen_sck.close();
			} catch (Exception e) {
			}
			mListen_sck = null;
		}
	}

	public void run() {
		ProcHandleConnect();
	}

	public void ProcHandleConnect() {
		while (mListen_sck != null) {
			Socket data_sck;
			try {
				data_sck = mListen_sck.accept();
				mData_sck = data_sck;
			} catch (Exception e1) {
				break;
			}
			try {
				this.viewCamera(data_sck);
				data_sck.close();
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
	public void viewCamera(final Socket data_sck) {

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
					return;
				}
			}
		} catch (Exception e) {
			return;
		}

		// Open Camera, Set Camera
		int turnDegree = 90;
		SurfaceTexture st = new SurfaceTexture(0);
		try {
			CloseCamera();
			mCamera = Camera.open(cameraindex);

			try {
				Parameters p = mCamera.getParameters();
				if (p != null) {
					if (backCamera) {
						p.setFlashMode(Parameters.FLASH_MODE_OFF);
						p.set("orientation", "portrait");
						p.setRotation(turnDegree);
					} else {
						p.set("orientation", "portrait");
						turnDegree = 270;
						p.setRotation(turnDegree);
					}
					//p.setFocusMode(p.FOCUS_MODE_CONTINUOUS_PICTURE);
					p.setPreviewFpsRange(5, 10);
					p.setAntibanding(p.ANTIBANDING_60HZ);

					mCamera.setParameters(p);
				}
			} catch (Exception e) {
			}
			mCamera.setDisplayOrientation(90);
			mCamera.setPreviewTexture(st);
		} catch (Exception e1) {
			CloseCamera();
			return;
		}

		Size size = mCamera.getParameters().getPreviewSize();
		final int wide = size.width;
		final int high = size.height;
		final int turnValue = turnDegree;

		// Start Camera
		PreviewCallback PreviewCb = new PreviewCallback() {
			double LastLightValue = 0;
			long stableTime = 0;
			boolean isFocused = false;
			int AutoFocusLightThreshold = 5;
			
			public void onPreviewFrame(byte[] data, Camera camera) {
				try {
					OutputStream os = data_sck.getOutputStream();
					int[] rgb = new int[data.length];
					decodeYUV420SP(rgb, data, wide, high);
					// YuvImage image = new YuvImage(data, ImageFormat.NV21,
					// wide,high, null);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					// if (image.compressToJpeg(new Rect(0, 0, wide, high), 100,
					// bos))
					Bitmap bm = Bitmap.createBitmap(rgb, wide, high,
							Config.RGB_565);
					Matrix m = new Matrix();
					m.postRotate(turnValue);
					//bm.compress(CompressFormat.JPEG, 100, bos);
					Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, wide, high,m, true);
					bitmap.compress(CompressFormat.JPEG, mQuality, bos);
					
					byte[] cdata = bos.toByteArray();
					sendDataPack(cdata, os);
					
					double bright = getLight(rgb);
					long   currentTime = System.currentTimeMillis();
					
					//Light Change
					if(LastLightValue -AutoFocusLightThreshold >  bright 
							|| bright  > LastLightValue +AutoFocusLightThreshold )
					{
						LastLightValue = bright;
						stableTime = currentTime;
						isFocused = false;
					}
					else if(!isFocused)//If Change Little ,Focus Camera
					{
						if(currentTime - stableTime > 500)
						{
							camera.autoFocus(null);
							isFocused = true;
						}
					}
					
				} catch (Exception e) {
					try {
						CloseCamera();
					} catch (Exception e1) {
					}
				}
			}
		};

		mCamera.startPreview();
		//mCamera.autoFocus(null);
		mCamera.setPreviewCallback(PreviewCb);

		byte[] command = new byte[100];
		try {
			InputStream is = data_sck.getInputStream();
			while (is.read(command) != -1) {
				mCamera.autoFocus(null);
			}
		} catch (Exception e) {
			try {
				CloseCamera();
			} catch (Exception e1) {
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
	}

}
