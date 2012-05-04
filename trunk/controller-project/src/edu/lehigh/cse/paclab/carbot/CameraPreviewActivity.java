package edu.lehigh.cse.paclab.carbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Example for taking a picture. Unlike previous example, this actually works.
 * 
 * TODO: crop the image, resize it, and then send it over bluetooth. Then on to
 * video streaming, which may benefit from
 * http://stackoverflow.com/questions/3376672/how-to-capture-preview-image-frames-from-camera-application-in-android-programmi
 */
public class CameraPreviewActivity extends Activity {

	private final String TAG = "CameraPreviewActivity";
	private Camera mCamera;
	private CameraPreviewView mPreview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camerapreviewlayout);

		// Create an instance of Camera
		mCamera = getCameraInstance();

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreviewView(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		// now drop a square onto it
		ImageView sq = new ImageView(this);
		Drawable d = getResources().getDrawable(R.drawable.square);
		sq.setImageDrawable(d);
		preview.addView(sq);
		
		// Add a listener to the Capture button
		Button captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get an image from the camera
				mCamera.takePicture(null, null, mPicture);
			}
		});
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			Toast.makeText(CameraPreviewActivity.this,
					pictureFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}
	};

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera(); // release the camera immediately on pause event
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	public static final int MEDIA_TYPE_IMAGE = 1;

	/** Create a file Uri for saving an image or video */
	public static Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else {
			return null;
		}

		return mediaFile;
	}
}

/* Example for cropping images, from http://www.androidworks.com/crop_large_photos_with_android:
@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
thiz = this;
setContentView(R.layout.main);
mBtn = (Button) findViewById(R.id.btnLaunch);
photo = (ImageView) findViewById(R.id.imgPhoto);

mBtn.setOnClickListener(new OnClickListener(){

public void onClick(View v) {
try {
// Launch picker to choose photo for selected contact
Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
intent.setType("image/*");
intent.putExtra("crop", "true");
intent.putExtra("aspectX", aspectX);
intent.putExtra("aspectY", aspectY);
intent.putExtra("outputX", outputX);
intent.putExtra("outputY", outputY);
intent.putExtra("scale", scale);
intent.putExtra("return-data", return_data);
intent.putExtra(MediaStore.EXTRA_OUTPUT, getTempUri());
intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
intent.putExtra("noFaceDetection",!faceDetection); // lol, negative boolean noFaceDetection
if (circleCrop) {
intent.putExtra("circleCrop", true);
}

startActivityForResult(intent, PHOTO_PICKED);
} catch (ActivityNotFoundException e) {
Toast.makeText(thiz, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
}
}
});

}

private Uri getTempUri() {
return Uri.fromFile(getTempFile());
}

private File getTempFile() {
if (isSDCARDMounted()) {

File f = new File(Environment.getExternalStorageDirectory(),TEMP_PHOTO_FILE);
try {
f.createNewFile();
} catch (IOException e) {
// TODO Auto-generated catch block
Toast.makeText(thiz, R.string.fileIOIssue, Toast.LENGTH_LONG).show();
}
return f;
} else {
return null;
}
}

private boolean isSDCARDMounted(){
String status = Environment.getExternalStorageState();

if (status.equals(Environment.MEDIA_MOUNTED))
return true;
return false;
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
super.onActivityResult(requestCode, resultCode, data);

switch (requestCode) {
case PHOTO_PICKED:
if (resultCode == RESULT_OK) {
if (data == null) {
Log.w(TAG, "Null data, but RESULT_OK, from image picker!");
Toast t = Toast.makeText(this, R.string.no_photo_picked,
Toast.LENGTH_SHORT);
t.show();
return;
}

final Bundle extras = data.getExtras();
if (extras != null) {
File tempFile = getTempFile();
// new logic to get the photo from a URI
if (data.getAction() != null) {
processPhotoUpdate(tempFile);
}
}
}
break;
}
}
*/