package android.heller.photo.multicastdemo;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.CharArrayReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Button mConnectButton;
    Button mMessageButton;
    TextView mTextView;
    MulticastSocket mSocket;
    InetAddress mAddr;
    Runnable mThread;
    WifiManager.MulticastLock mLock;
    boolean mConnected = false;

    final int PORT = 6789;


    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mAddr = InetAddress.getByName("228.5.6.7");
                    mSocket = new MulticastSocket(PORT);
                    WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if(wifi != null)
                    {
                        mLock = wifi.createMulticastLock("HELLER.PHOTO");
                        mLock.acquire();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mMessageButton = findViewById(R.id.message_button);
        mConnectButton = findViewById(R.id.connect_button);
        mTextView = findViewById(R.id.text_buff);

        mThread = new Runnable() {
            @Override
            public void run() {
                final byte[] buf = new byte[1000];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                try {
                    while(true) {
                        mSocket.receive(recv);
                        final String recvString = new String(recv.getData(), StandardCharsets.UTF_8);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder sb = new StringBuilder();
                                sb.append(mTextView.getText());
                                sb.append(recvString).append("\n");
                                mTextView.setText(sb.toString());
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };



        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show();
                if (!mConnected) {
                    //
                    mConnectButton.setText("Disconnect");
                    try {
                        mSocket.joinGroup(mAddr);
                        mConnected = true;
                        AsyncTask.THREAD_POOL_EXECUTOR.execute(mThread);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    mConnectButton.setText("Connect");
                    try {
                        mSocket.leaveGroup(mAddr);
                        mConnected = false;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mMessageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mConnected) {
                    Toast.makeText(getApplicationContext(), "Sending Message", Toast.LENGTH_SHORT).show();
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String msg = "this is a test message";
                                mSocket.send(new DatagramPacket(msg.getBytes(), msg.length(), mAddr, PORT));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "Not connected!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
