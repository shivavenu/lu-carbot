package edu.lehigh.cse.paclab.prelims;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import edu.lehigh.cse.paclab.carbot.R;

/**
 * from http://www.edumobile.org/android/android-development/socket-programming/
 */
public class WifiClientActivity extends Activity
{
    private Button bt;
    private Socket socket;
    
    // AND THAT’S MY DEV’T MACHINE WHERE PACKETS TO
    // PORT 5000 GET REDIRECTED TO THE SERVER EMULATOR’S
    // PORT 6000
    private static final int REDIRECTED_SERVERPORT = 5000;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wificlientlayout);
    }

    public void onClickConnect(View v)
    {
        bt = (Button) findViewById(R.id.btnWifiSend);
        EditText et1 = (EditText)findViewById(R.id.etWifiIP);
        try {
            InetAddress serverAddr = InetAddress.getByName(et1.getText().toString());
            socket = new Socket(serverAddr, REDIRECTED_SERVERPORT);
        }
        catch (UnknownHostException e1) {
            e1.printStackTrace();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        bt.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                try {
                    EditText et = (EditText) findViewById(R.id.etWifiMessage);
                    String str = et.getText().toString();
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true);
                    out.println(str);
                    Log.d("Client", "Client sent message");
                }
                catch (UnknownHostException e) {
                    Toast.makeText(WifiClientActivity.this, "Error1", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                catch (IOException e) {
                    Toast.makeText(WifiClientActivity.this, "Error2", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                catch (Exception e) {
                    Toast.makeText(WifiClientActivity.this, "Error3", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

}
