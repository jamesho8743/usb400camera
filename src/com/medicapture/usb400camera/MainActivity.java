package com.medicapture.usb400camera;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.medicapture.usb400camera.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	static String TAG = "CameraPreview";
	
	private Bitmap mIconVideoSourceOn;
	private Bitmap mIconStorageOn;
	private Bitmap mIconUserOn;
	private Bitmap mIconCaptureOn;
	private Bitmap mIconRecordOn;
	private Bitmap mIconDateTime;
	private Paint mPaintText;
	private Time mSystemTime = new Time();
	private StatFs mFileSysStat = new StatFs( MainActivity.mManager.getStoragePath());

	public CameraPreview(Context context, Camera camera) {
		super(context);
		mCamera = camera;

		mHolder = getHolder();
		mHolder.addCallback(this);
		setupUIResource();
	}
	
	private void setupUIResource() {
		mPaintText = new Paint();
		mPaintText.setTextSize(22);
		mPaintText.setColor(Color.WHITE);
		mIconVideoSourceOn = BitmapFactory.decodeResource(getResources(), R.drawable.source_on);
		mIconStorageOn = BitmapFactory.decodeResource(getResources(), R.drawable.storage_on);
		mIconUserOn = BitmapFactory.decodeResource(getResources(), R.drawable.user_on);
		mIconCaptureOn = BitmapFactory.decodeResource(getResources(), R.drawable.capture_on);
		mIconRecordOn = BitmapFactory.decodeResource(getResources(), R.drawable.record_on);
		mIconDateTime = BitmapFactory.decodeResource(getResources(), R.drawable.datetime);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int width = 128 + 16;
		int x = 16;
		int y = 850;
				
 		long bytes = mFileSysStat.getAvailableBytes();
		
		canvas.drawBitmap( mIconVideoSourceOn, x, y, null);
		canvas.drawText("1920x1080", x, y+150, mPaintText);
		x += width; 
		canvas.drawBitmap( mIconStorageOn, x, y, null);
		canvas.drawText( String.format( "%.1fGB", bytes / 1000000000.0 ), x, y+150, mPaintText);
		x += width;
		canvas.drawBitmap( mIconUserOn, x, y, null);
		canvas.drawText( String.format( "%04d", MainActivity.mPatientIndex), x, y+150, mPaintText);
		x += width;
		canvas.drawBitmap( mIconCaptureOn, x, y, null);
		canvas.drawText( String.format( "%03d", MainActivity.mPictureIndex), x, y+150, mPaintText);
		x += width;
		canvas.drawBitmap( mIconRecordOn, x, y, null);
		canvas.drawText( String.format( "%04d", MainActivity.mVideoIndex), x, y+150, mPaintText);
		x = 1780;
		canvas.drawBitmap( mIconDateTime, x, y, null);
		mSystemTime.setToNow();
		if ( mSystemTime.second % 2 == 0 ) {
			canvas.drawText( mSystemTime.format("%H:%M"), x - 70, y + 90, mPaintText);
		}
		else {
			canvas.drawText( mSystemTime.format("%H %M"), x - 70, y + 90, mPaintText);
		}
		canvas.drawText( mSystemTime.format3339(true), x - 70, y+120, mPaintText);
	}


	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated " );
		setWillNotDraw(false);
		/*
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
		*/
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// release Camera Preview here
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

}

class BlinkingTask extends TimerTask {
	private boolean state;
	@Override
	public void run() {
		if ( state )
			MainActivity.led( 1, 1);
		else
			MainActivity.led( 1, 0);
		state = !state;
	}
}

class DisplayTimeTask extends TimerTask {
	private Handler mHandler;
	DisplayTimeTask( Handler handler) {
		mHandler = handler;
	}
	@Override
	public void run(){
		mHandler.obtainMessage().sendToTarget();
	}
}

class StorageManager {
	StringBuffer mCurrentStorageDir;
	String path = "/storage/usbdisk";
	File currentPath;
	public StorageManager() {
		update();
	}
	public void update() {
		if ( mCurrentStorageDir != null ) 
			currentPath = new File(mCurrentStorageDir.toString());
	
		if ( mCurrentStorageDir == null || !currentPath.canWrite()) {
			mCurrentStorageDir = null;
			for ( int i = 6; i > 0; i--) {
				File file = new File( path + Integer.toString(i));
				if ( file.canWrite() )
					mCurrentStorageDir = new StringBuffer(file.getPath());
			}
		}
	}
	public String getStoragePath() {
		if ( mCurrentStorageDir != null )
			return mCurrentStorageDir.toString();
		else
			return null;
	}

}

class CheckingDisk extends Thread {
	@Override 
	public void run() {
		while (true) {
		// Checking Disk
			MainActivity.mManager.update();
			if ( MainActivity.mManager.getStoragePath() != null )
				MainActivity.diskInserted = true;
			else
				MainActivity.diskInserted = false;
		
		// Update LED
			if ( !MainActivity.isRecording)
				MainActivity.UpdatePanelLED();
			try {
				sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
};

public class MainActivity extends Activity {
	private Camera mCamera;
	private CameraPreview mPreview;
	private MediaRecorder mMediaRecorder;
	private PictureCallback mPicture;
	private Timer mRecordingBlink;
	private TimerTask mBlinkingTask;
	private TimerTask mDisplayTimeTask;
	static boolean diskInserted;
	private static boolean hasRecorded = true;
	private boolean isCapturing;
	
	private int mInputResolutionWidth = 1920;
	private int mInputResolutionHeight = 1080;
	
	static int mPictureIndex = 0;
	static int mVideoIndex = 0;
	static int mPatientIndex = 1;
	
	static StorageManager mManager = new StorageManager();
	static final String KEY_RESOLUTION_WIDTH = "INPUT_RES_WIDTH";
	static final String KEY_RESOLUTION_HEIGHT = "INPUT_RES_HEIGHT";
	static TextView textView2;
		
	Button recordButton;
	Button captureButton;

	// This class must be public and static
	public static class USBPlugReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ( action.equals(Intent.ACTION_MEDIA_MOUNTED) ) {
				MainActivity.mManager.update();
				mPatientIndex = 1;
				mVideoIndex = 0;
				mPictureIndex = 0;
				/*
				boolean ok = new File(mManager.getStoragePath() + "/Patient" + String.valueOf(MainActivity.mPatientIndex)).mkdir();
				StatFs stat = new StatFs(mManager.getStoragePath());
		 		long bytes = stat.getAvailableBytes();
		 		MainActivity.textView2.setTextSize(20);
		 		MainActivity.textView2.setTextColor(Color.GREEN);
		        String size = "Avail " + String.valueOf(bytes);
		        MainActivity.textView2.setText(size);
		        */
		        
			}
			else if ( action.equals(Intent.ACTION_MEDIA_EJECT) ) {
				MainActivity.textView2.setText(null);
				MainActivity.mManager.update();
			} 
		}
	}
	
	/*
	public static boolean isSDCardPresent(){
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	*/
	
	static void UpdatePanelLED () {
		if ( diskInserted )
		{
			if ( hasRecorded )
				led( 0, 1);
			else
				led( 0, 0);
			led( 1, 1);
			led( 2, 0);
		} else {
			led( 0, 0);
			led( 1, 0);
			led( 2, 0);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		TextView myTextView = (TextView)findViewById(R.id.textView1);
        myTextView.setTextSize(30);
        myTextView.setTextColor(Color.GREEN);
		 switch (keyCode) {
		 	case KeyEvent.KEYCODE_F8:
		 		/*
		 		if ( mInputResolutionHeight != 720 )
		 		{
		 			mInputResolutionWidth = 1280;
		 			mInputResolutionHeight = 720;
		 			this.recreate();
		 		}
		 		*/
		 		/*
		 		Camera.Parameters camPara = mCamera.getParameters();
				camPara.setPreviewSize(1280, 720);
				mCamera.setParameters(camPara);
				*/
		 		return true;
		 	case KeyEvent.KEYCODE_F9:
		 		/*
		 		if ( mInputResolutionHeight != 1080 )
		 		{
		 			mInputResolutionWidth = 1920;
		 			mInputResolutionHeight = 1080;
		 			this.recreate();
		 		}
		 		*/
		 		/*
		 		camPara = mCamera.getParameters();
				camPara.setPreviewSize(1920, 1080);
				mCamera.setParameters(camPara);
				*/
		 		return true;
		 		
	        case KeyEvent.KEYCODE_F1:
	        	if ( diskInserted && hasRecorded && !isCapturing )
	        	{
	        		isCapturing = true;
	        		captureButton.callOnClick();
	        	}
	        	myTextView.setText("Capture");
	            return true;
	        case KeyEvent.KEYCODE_F2:
	        	if ( diskInserted)
	        		recordButton.callOnClick();
	        	myTextView.setText("Record");
	            return true;
	        case KeyEvent.KEYCODE_F3:
	            myTextView.setText("Patient");
	            mPatientIndex++;
	            new File(mManager.getStoragePath() + "/Patient" + String.valueOf(mPatientIndex)).mkdir();
	            return true;
	        case KeyEvent.KEYCODE_F4:
	            myTextView.setText("Remote1");
	            return true;
	        case KeyEvent.KEYCODE_F5:
	            myTextView.setText("Remote2");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_UP:
	            myTextView.setText("Up");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_DOWN:
	            myTextView.setText("Down");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_LEFT:
	            myTextView.setText("Left");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_RIGHT:
	            myTextView.setText("Right");
	            return true;
	        case KeyEvent.KEYCODE_ENTER:
	            myTextView.setText("Enter");
	            return true;	            
	        default:
	            return super.onKeyDown(keyCode, event);
	    }
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		TextView myTextView = (TextView)findViewById(R.id.textView1);
	    switch (keyCode) {
	        case KeyEvent.KEYCODE_F1:
	        	myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_F2:
	        	myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_F3:
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_F4:	    		
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_F5:	    		
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_UP:
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_DOWN:
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_LEFT:
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_DPAD_RIGHT:
	            myTextView.setText("");
	            return true;
	        case KeyEvent.KEYCODE_ENTER:
	            myTextView.setText("");
	            return true;	
	        default:
	            return super.onKeyUp(keyCode, event);
	    }
	}
	
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			int cameraId = 0;
			c = Camera.open(cameraId);
		} catch (Exception e) {
			// Camera is not available ( in use or does not exist)
		}
		return c;
	}

	static boolean isRecording = false;

	@Override
	protected void onSaveInstanceState( Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_RESOLUTION_WIDTH, mInputResolutionWidth );
		outState.putInt(KEY_RESOLUTION_HEIGHT, mInputResolutionHeight );
	}
	public Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
	    	mPreview.invalidate();	        
	    }
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ( savedInstanceState != null ) {
			mInputResolutionWidth = savedInstanceState.getInt(KEY_RESOLUTION_WIDTH);
			mInputResolutionHeight = savedInstanceState.getInt(KEY_RESOLUTION_HEIGHT);
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);

		Timer timeTimer = new Timer(true);
		mDisplayTimeTask = new DisplayTimeTask( mHandler );
		timeTimer.scheduleAtFixedRate( mDisplayTimeTask, 0, 1000);

		boolean success = new File(mManager.getStoragePath() + "/Patient" + String.valueOf(mPatientIndex)).mkdir();

		textView2 = (TextView)findViewById(R.id.textView2);
		CheckingDisk bgTask = new CheckingDisk();
		bgTask.start();

		mCamera = getCameraInstance();
		Camera.Parameters camPara = mCamera.getParameters();
		camPara.setPictureSize(mInputResolutionWidth, mInputResolutionHeight);
		camPara.setPreviewSize(mInputResolutionWidth, mInputResolutionHeight);
		
		mCamera.setParameters(camPara);

		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
		preview.addView(mPreview);
	
		mPicture = new PictureCallback() {
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {

				//File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				//File pictureFile = new File(picDir, "ImageCapture.jpg");
				
				mPictureIndex++;
				File pictureFile = new File( mManager.getStoragePath() + "/Patient" + String.valueOf(mPatientIndex) + "/imagecapture" + String.valueOf(mPictureIndex) + ".jpg"); //SDCARD1 is SDCARD

				try {
					FileOutputStream fos = new FileOutputStream(pictureFile);
					Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
					bmp.compress(CompressFormat.JPEG, 100, fos);
					fos.write(data);
					fos.close();
				} catch (FileNotFoundException e) {
					// Log.d(TAG, "File not found: " + e.getMessage());
				} catch (IOException e) {
					// Log.d(TAG, "Error accessing file: " + e.getMessage());
				}
				mCamera.startPreview();
				captureButton.setTextColor( Color.BLACK);
				isCapturing = false;
			}
		};

		captureButton = (Button) findViewById(R.id.buttonCapture);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ( diskInserted && hasRecorded && !isCapturing )
					captureButton.setTextColor( Color.GREEN);
					mCamera.takePicture(null, null, mPicture );
			}
		});

		recordButton = (Button) findViewById(R.id.buttonRecord);
		recordButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ( !diskInserted )
					return;
				
				hasRecorded = true;
				if (isRecording) {
					mMediaRecorder.stop();
					recordButton.setTextColor(Color.BLACK);
					releaseMediaRecorder();
					//mCamera.lock();

					// setCaptureButtonText("Record");
					isRecording = false;
					if ( mRecordingBlink != null )
						mRecordingBlink.cancel();
				} else {
					isRecording = true;
					if (prepareVideoRecorder()) {
						mMediaRecorder.start();
						
						recordButton.setTextColor( Color.GREEN);
						mRecordingBlink = new Timer(true);
						mBlinkingTask = new BlinkingTask();
						mRecordingBlink.scheduleAtFixedRate( mBlinkingTask, 0, 500);
						 
						// setCaptureButtonText("Stop");
					} else {
						releaseMediaRecorder();
					}
				}
			}
		}

		);	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private boolean prepareVideoRecorder() {

		//mCamera = getCameraInstance();
		mMediaRecorder = new MediaRecorder();
		
		Camera.Parameters camPara = mCamera.getParameters();
		//List<Size> sizes = camPara.getSupportedPictureSizes();
		//sizes.isEmpty();
		//List<Size> previewSizes = camPara.getSupportedPreviewSizes();
		camPara.setPictureSize(mInputResolutionWidth, mInputResolutionHeight);
		camPara.setPreviewSize(mInputResolutionWidth, mInputResolutionHeight);
		//camPara.setPreviewSize(864, 480); // If we don't setup preview size here, we will see black screen in BSP 2.1.
		mCamera.setParameters(camPara);

		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera); // Must be called before prepare()

		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		
		//mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		
		mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // this line must be after setOutputFormat, or throw IllegalStateException
		mMediaRecorder.setVideoFrameRate(60);
		mMediaRecorder.setVideoSize(mInputResolutionWidth, mInputResolutionHeight);
		mMediaRecorder.setVideoEncodingBitRate(10000000);
		mMediaRecorder.setVideoEncoder(VideoEncoder.H264);

		mVideoIndex++;
		mMediaRecorder.setOutputFile(mManager.getStoragePath() + "/Patient" + String.valueOf(mPatientIndex) + "/recording" + String.valueOf(mVideoIndex) + ".mp4");
		mMediaRecorder.setMaxFileSize( 2000000000);
		//mMediaRecorder.setMaxDuration(10000);

		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder();
		releaseCamera();
	}
	
	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();
			mMediaRecorder.release();
			mMediaRecorder = null;
			//mCamera.lock();
			try {
				mCamera.reconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release();
			mCamera = null;
		}
	}
	
	public static native int led(int i, int j);
	static
    {
        System.loadLibrary("MainActivity");
    }  
}

