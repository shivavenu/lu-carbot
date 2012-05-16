package edu.lehigh.cse.paclab.prelims;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import edu.lehigh.cse.paclab.carbot.R;

public class BTFindDeviceActivity extends Activity
{
    private BluetoothAdapter btAdapter;
    private ArrayAdapter<String> pairedDevices;
    private ArrayAdapter<String> newDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // allow the window to show "indeterminate progress" while discovering
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.btdiscovery);

        // since this launches from an intent, set the default return value
        setResult(Activity.RESULT_CANCELED);

        // Wire up the button
        Button b = (Button) findViewById(R.id.btnDiscoverySearch);
        b.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                findDevices();
            }
        });

        // set up the adapters, give them an onclicklistener
        pairedDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView lv = (ListView) findViewById(R.id.lvDiscoveryDevices);
        lv.setAdapter(pairedDevices);
        lv.setOnItemClickListener(myLVListener);
        newDevices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lv = (ListView) findViewById(R.id.lvDiscoveryNewDevices);
        lv.setAdapter(newDevices);
        lv.setOnItemClickListener(myLVListener);

        // register for broadcasts when a device is discovered
        IntentFilter f = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, f);

        // register for broadcasts when discovery is finished
        f = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(myReceiver, f);

        // get bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // get paired devices
        Set<BluetoothDevice> devs = btAdapter.getBondedDevices();
        if (devs.size() > 0) {
            for (BluetoothDevice d : devs)
                pairedDevices.add(d.getName() + "\n" + d.getAddress());
        }
        else {
            pairedDevices.add("No devices");
        }
    }

    /** try to find devices */
    public void findDevices()
    {
        // update the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("scanning");
        // stop existing discovery
        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();
        // start discovering again
        btAdapter.startDiscovery();
    }

    /** if the activity is canceled, shut off the receiver and stop discovery */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (btAdapter != null)
            btAdapter.cancelDiscovery();
        unregisterReceiver(myReceiver);
    }

    /** handle a click of a listview */
    private OnItemClickListener myLVListener = new OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
        {
            // stop discovery, since we've found something
            btAdapter.cancelDiscovery();
            // get MAC address from the selection (last 17 chars)
            String info = ((TextView) v).getText().toString();
            String mac = info.substring(info.length() - 17);
            // create a result, add info, return it
            Intent i = new Intent();
            i.putExtra("MAC_ADDRESS", mac);
            setResult(Activity.RESULT_OK, i);
            finish();
        }
    };

    /** this listens for discovered devices or the end of discovery */
    private final BroadcastReceiver myReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context c, Intent i)
        {
            String action = i.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // get the device name
                BluetoothDevice d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add it only if it isn't already in the list
                if (d.getBondState() != BluetoothDevice.BOND_BONDED)
                    newDevices.add(d.getName() + "\n" + d.getAddress());
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Make your choice:");
                if (newDevices.getCount() == 0) {
                    newDevices.add("No devices found");
                }
            }
        }
    };
}
