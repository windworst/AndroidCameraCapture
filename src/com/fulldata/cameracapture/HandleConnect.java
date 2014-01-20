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
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;

public class HandleConnect extends Thread {
	ServerSocket listen_sck;
	public void Close()
	{
		try {
			listen_sck.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean Listening(int port)
	{
		try {
			listen_sck = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public void ProcHandleConnect()
	{
		while(true)
		{
			Socket accept_sck;
			try {
				accept_sck = listen_sck.accept();
			} catch (IOException e1) {
				break;
			}
			try {
				if(!this.catchCamera(accept_sck))
				{
					accept_sck.close();
				}
			} catch (Exception e) {
			}
		}


	}
	
	
	// Get Camera Index
	@SuppressLint("NewApi") 
	private int FindCamera(boolean back){  
        int cameraCount = 0;  
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();  
        cameraCount = Camera.getNumberOfCameras(); // get cameras number  
        int value = Camera.CameraInfo.CAMERA_FACING_FRONT;
        if(back)
        {
        	value = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {  
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
        	if ( cameraInfo.facing == value ) {
        		return camIdx;  
        	}  
        }  
        return -1;  
    }  

	// Main Func of Catch Camera
	@SuppressLint("NewApi")
	public boolean catchCamera(final Socket accept_sck) {
		
		BufferedReader is = null;
		BufferedWriter os = null;
		int cameraindex = 0;
		try {
			is = new BufferedReader(new InputStreamReader(accept_sck.getInputStream()));
			os = new BufferedWriter(new OutputStreamWriter(accept_sck.getOutputStream()));
			
			char[] buf= new char[10];
			if(is.read(buf)>0)
			{
				cameraindex= FindCamera(buf[0]!='0');
				if(cameraindex==-1)
				{
					os.write("error");
					os.flush();
					return false;
				}
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return false;
		}finally{
		}

		Camera camera = null;
		try {
			camera = Camera.open(cameraindex);
			
			//Front Camera Can not be set
			try{
				Parameters p = camera.getParameters();
				p.setFlashMode(Parameters.FLASH_MODE_OFF);
				camera.setParameters(p);
			}
			catch (Exception e)
			{}
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
					try {
						OutputStream os = accept_sck.getOutputStream();
						os.write(data);
						os.flush();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						
					}finally{

					}

					try {
						accept_sck.close();
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
	
	public void run()
	{
		ProcHandleConnect();
	}

}
