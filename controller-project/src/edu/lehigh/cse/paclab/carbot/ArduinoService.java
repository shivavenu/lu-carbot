// stolen from http://www.vogella.com/articles/AndroidServices/article.html

package edu.lehigh.cse.paclab.carbot;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.util.Log;

public class ArduinoService extends Service
{
	// Following code block is setting up the android to arduino communication.
	//
	// [mfs] I don't think it is entirely right
	private static final String ACTION_USB_PERMISSION = "com.google.android.Demokit.action.USB_PERMISSION";
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

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
						Log.d("CARBOT", "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
	// Used to receive messages from the Activity
	final Messenger inMessenger = new Messenger(new IncomingHandler());

	// Use to send message to the Activity
	//
	// [mfs] currently unused
	public static final String RETURNVAL = "urlPath";
	private int result = Activity.RESULT_CANCELED;
	private Messenger outMessenger;

	public ArduinoService() {
		super();
		// Don't do this
		// Network Stuff will run in the main thread
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		
	}
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			String CMD = data.getString("CMD");
			if (CMD.equals("INIT"))
				createConnection();
			if (CMD.equals("FWD"))
				cmdForward();
			if (CMD.equals("REV"))
				cmdReverse();
			if (CMD.equals("STOP"))
				cmdStop();
			/*
			Log.e("MESSAGE", "Got message");
			Bundle data = msg.getData();
			String urlPath = data.getString(ArduinoService.URLPATH);
			String fileName = data.getString(ArduinoService.FILENAME);
			String outputPath = download(urlPath, fileName);

			Message backMsg = Message.obtain();
			backMsg.arg1 = result;
			Bundle bundle = new Bundle();
			bundle.putString(RESULTPATH, outputPath);
			backMsg.setData(bundle);
			try {
				outMessenger.send(backMsg);
			} catch (android.os.RemoteException e1) {
				Log.w(getClass().getName(), "Exception sending message", e1);
			}
			*/
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		Bundle extras = intent.getExtras();
		// Get messager from the Activity
		if (extras != null) {
			outMessenger = (Messenger) extras.get("MESSENGER");
		}
		// Return our messenger to the Activity to get commands
		return inMessenger.getBinder();
	}

    public void createConnection() 
	{
        //Looks for input
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		
		//Creates new IntentFilter to indicate future communication with a particular entity
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		onResumeDoesntHappen();
    }
	
	/**
	 * If the application has stopped and then has resumed, the application
	 * will check to see if the input and output stream of data is still active then checks to see if
	 * an accessory is present, if so, opens communication; if not, the application will request permission
	 * for communication.
	 * 
	 * [mfs] It seems that we need this to run after we get a reply from the intent...
	 */
	public void onResumeDoesntHappen() 
	{
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
			Log.d("CARBOT", "mAccessory is null");
		}
	}
	/**
	 * More watchdogs...
	 */
	@Override
	public void onDestroy() {
		closeAccessory();
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
			Log.d("CARBOT", "accessory open fail");
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
		
		Log.e("CARBOT", "Message sent" + buffer[0]);
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e("CARBOT", "write failed", e);
			}
		}
	}
	
	/**
	 * The last few methods dictate what command will be sent based on what
	 * button is activated by the user
	 */
	public void cmdStop() {
		sendCommand((byte) 0);
	}

	public void cmdForward() {
		sendCommand((byte) 1);
	}

	public void cmdReverse() {
		sendCommand((byte) 2);
	}

	public void cmdClockwise() {
		sendCommand((byte) 3);
	}

	public void cmdCclockwise() {
		sendCommand((byte) 4);
	}

	public void cmdPointTurnR() {
		sendCommand((byte) 5);
	}

	public void cmdPointTurnL() {
		sendCommand((byte) 6);
	}

}
