package edu.lehigh.cse.paclab.prelims;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;
import edu.lehigh.cse.paclab.carbot.R;

public class DriveTheBotActivity extends Activity 
{
	public static final String TAG = "Carbot";
	
	// Following code block is setting up the android to arduino communication. 
	private static final String ACTION_USB_PERMISSION = "com.google.android.Demokit.action.USB_PERMISSION";
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	ToggleButton forward;
	ToggleButton reverse;
	ToggleButton clockwise;
	ToggleButton counterClockwise;
	ToggleButton pTurnLeft;
	ToggleButton pTurnRight;
	
	/**
	 * BroadcastReceiver is the object responsible for establishing communication with any sort of entity sending 
	 * information to the application, in this case, the application is receiving information from an arduino. 
	 * The BroadcastReceiver sees if the entity sending information is a supported usb accessory, in which case
	 * opens full communication with the device beyond the handshake which is initiated upon application launch.
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} 
			else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        
        //Looks for input
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		//Creates new IntentFilter to indicate future communication with a particular entity
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
		setContentView(R.layout.drivethebotlayout);
		
		forward = (ToggleButton) findViewById(R.id.button1);
		reverse = (ToggleButton) findViewById(R.id.button2);
		clockwise = (ToggleButton) findViewById(R.id.button3);
		counterClockwise = (ToggleButton) findViewById(R.id.button4);
		pTurnRight = (ToggleButton) findViewById(R.id.button5);
		pTurnLeft = (ToggleButton) findViewById(R.id.button6);
    }
	
	/**
	 * If the application has stopped and then has resumed, the application
	 * will check to see if the input and output stream of data is still active then checks to see if
	 * an accessory is present, if so, opens communication; if not, the application will request permission
	 * for communication.
	 */
	@Override
	public void onResume() 
	{
		super.onResume();
		
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}
	/**
	 * If the application is paused, the accessory is closed.
	 */
	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}
	/**
	 * More watchdogs...
	 */
	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
	
	/**
	 * In the event of the application being terminated or paused closeAccessory is called to end the data input stream.
	 */
	protected void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}
	/**
	 * Called upon the construction of the BroadcastReceiver assuming the BroadcastReceiver has found an accessory to
	 * interact with. openAccessory is also called in the onResume method. Opens up a data output and input stream
	 * for communication with an accessory. 
	 * @param accessory
	 */
	protected void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}
	
	/**
	 * Called anytime information is to be sent out from the application over the output stream. An array of bytes is created
	 * with the first value holding the "address" of the hardware being communicated with. In this case, 1 means the 
	 * forward, 2 means reverse, etc. In our system, arduino handles the task to be carried out by the hardware, so, 
	 * arduino only needs to know whether or not to carry out a particular action indicated by the action reference number. 
	 * @param target
	 */
	public void sendCommand(byte target) {
		byte[] buffer = new byte[1];
		buffer[0] = target;
		
		Log.e(TAG, "Message sent" + buffer[0]);
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}
	
	/**
	 * The last few methods dictate what command will be sent based on what button is activated by the user
	 */
	public void forward (View V){
		if(!forward.isChecked())
			sendCommand((byte)0);
		else sendCommand((byte)1);
	}
	public void reverse (View V){
		if(!reverse.isChecked())
			sendCommand((byte)0);
		else sendCommand((byte)2);
	}
	public void clockwise (View V){
		if(!clockwise.isChecked())
			sendCommand((byte)0);
		else sendCommand((byte)3);
	}
	public void cclockwise (View V){
		if(!counterClockwise.isChecked())
			sendCommand((byte)0);
		else sendCommand((byte)4);
	}
	public void pointTurnR (View V){
		if(!pTurnRight.isChecked())
			sendCommand((byte)0);
		else sendCommand((byte)5);
	}
	public void pointTurnL (View V){
		if(!pTurnLeft.isChecked())
			sendCommand((byte)0);
		else sendCommand((byte)6);
	}
}