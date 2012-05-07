package edu.lehigh.cse.paclab.carbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class WifiServerActivity extends Activity
{
    ServerSocket ss = null;
    String mClientMsg = "";
    Thread myCommsThread = null;
    protected static final int MSG_ID = 4823957;
    public static final int SERVERPORT = 5000;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifiserverlayout);
        TextView tv = (TextView) findViewById(R.id.tvServer);
        tv.setText("Nothing from client yet");
        this.myCommsThread = new Thread(new CommsThread());
        this.myCommsThread.start();
        
        getIP();

    }

    @Override
    protected void onStop()
    {
        super.onStop();
        try {
            // make sure you close the socket upon exiting
            ss.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    Handler myUpdateHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what) {
                case MSG_ID:
                    TextView tv = (TextView) findViewById(R.id.tvServer);
                    tv.setText(mClientMsg);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    class CommsThread implements Runnable
    {
        public void run()
        {
            Socket s = null;
            try {
                ss = new ServerSocket(SERVERPORT);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                Message m = new Message();
                m.what = MSG_ID;
                try {
                    if (s == null)
                        s = ss.accept();
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String st = null;
                    st = input.readLine();
                    mClientMsg = st;
                    myUpdateHandler.sendMessage(m);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    
    private void getIP()
    {
        Log.i("getIP", "in getIP");
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                Log.i("getIP", "in outer loop");
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    Log.i("getIP", "in inner loop");
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipaddress=inetAddress.getHostAddress().toString();
                        Log.i("getIP", ipaddress);
                        Toast.makeText(this, ipaddress, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Socket exception in GetIP Address of Utilities", ex.toString());
        }
    }

}
