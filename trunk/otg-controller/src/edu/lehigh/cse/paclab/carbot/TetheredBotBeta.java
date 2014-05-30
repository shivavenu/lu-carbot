package edu.lehigh.cse.paclab.carbot;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * This is the interface to TetheredBot. There are on-screen buttons for controlling the robot
 */
public class TetheredBotBeta extends BasicBotActivityBeta
{
    /**
     * On activity creation, we just inflate a menu
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // TODO: manage screen size better

        // Use this code for managing screens of different size...this uses a more recent API than the previous
        // TetheredBot
        //
        // Display display = getWindowManager().getDefaultDisplay(); Point size = new Point(); display.getSize(size);
        // int width = size.x; int height = size.y;
        setContentView(R.layout.tetheredbot_beta);
    }

    /**
     * Whenever one of the buttons is pressed, we issue the appropriate command to move the robot
     * 
     * @param v
     *            A reference to the button that was pressed
     */
    public void onClickImage(View v)
    {
        if (v == findViewById(R.id.ivTetherForward))
            myRobotForward();
        if (v == findViewById(R.id.ivTetherReverse))
            myRobotReverse();
        if (v == findViewById(R.id.ivTetherLeft))
            myRobotPointTurnLeft();
        if (v == findViewById(R.id.ivTetherRight))
            myRobotPointTurnRight();
        if (v == findViewById(R.id.ivTetherRotPos))
            myRobotClockwise();
        if (v == findViewById(R.id.ivTetherRotNeg))
            myRobotCounterClockwise();
        if (v == findViewById(R.id.ivTetherStop))
            myRobotStop();
    }

    /**
     * Provide an empty callback method, so that we are compatible with AlarmCallbackReceiver
     */
    public void callback()
    {
    }

    // /// OTG Hacks

    /**
     * Driver instance, passed in statically via {@link #show(Context, UsbSerialDriver)}.
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using arguments passed in with the
     * {@link #startActivity(Intent)} intent. We can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialDriver                  sDriver   = null;

    private final ExecutorService                   mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager                mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener()
                                                              {
                                                                  @Override
                                                                  public void onRunError(Exception e)
                                                                  {
                                                                  }
                                                                  @Override
                                                                  public void onNewData(final byte[] data)
                                                                  {
                                                                  }
                                                              };

    byte[]                                          data      = new byte[1];

    @Override
    protected void onPause()
    {
        super.onPause();
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            }
            catch (IOException e) {
                // Ignore.
            }
            sDriver = null;
        }
        finish();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Log.d(TAG, "Resumed, sDriver=" + sDriver);
        if (sDriver == null) {
            Toast.makeText(this, "No serial device.", Toast.LENGTH_SHORT).show();
        }
        else {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            }
            catch (IOException e) {
                // Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    sDriver.close();
                }
                catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            Toast.makeText(this, "Serial device: " + sDriver.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
        }
        onDeviceStateChange();
    }

    private void stopIoManager()
    {
        if (mSerialIoManager != null) {
            // Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager()
    {
        if (sDriver != null) {
            // Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange()
    {
        stopIoManager();
        startIoManager();
    }

    /**
     * Starts the activity, using the supplied driver instance.
     * 
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver)
    {
        sDriver = driver;
        final Intent intent = new Intent(context, TetheredBotBeta.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    /**
     * Send a byte to the Arduino that instructs it to stop
     */
    public void myRobotStop()
    {
        otgSendCommand((byte) 0);
    }

    /**
     * Send a byte to the Arduino that instructs it to go forward
     */
    public void myRobotForward()
    {
        otgSendCommand((byte) 1);
    }

    /**
     * Send a byte to the Arduino that instructs it to go backward
     */
    public void myRobotReverse()
    {
        otgSendCommand((byte) 2);
    }

    /**
     * Send a byte to the Arduino that instructs it to go clockwise
     */
    public void myRobotClockwise()
    {
        otgSendCommand((byte) 3);
    }

    /**
     * Send a byte to the Arduino that instructs it to go counterclockwise
     */
    public void myRobotCounterClockwise()
    {
        otgSendCommand((byte) 4);
    }

    /**
     * Send a byte to the Arduino that instructs it to do a point turn right
     */
    public void myRobotPointTurnRight()
    {
        otgSendCommand((byte) 6);
    }

    /**
     * Send a byte to the Arduino that instructs it to do a pont turn left
     */
    public void myRobotPointTurnLeft()
    {
        otgSendCommand((byte) 5);
    }

    /**
     * Custom sendCommand for interacting with OTG
     */
    void otgSendCommand(byte b)
    {
        data[0] = b;
        mSerialIoManager.writeAsync(data);
    }
}