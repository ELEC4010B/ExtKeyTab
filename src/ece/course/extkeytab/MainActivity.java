package ece.course.extkeytab;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;

public class MainActivity extends Activity {
	final int REQUEST_ENABLE_BT = 100;
	final int DISCOVER_DURATION = 0;
	int RESULT_CODE = 0;
	Intent ResultData;
	final String NAME = "ExtKeyTab";
	final UUID MY_UUID = UUID.fromString("4e1422d0-c62c-11e3-9c1a-0800200c9a66");
	BluetoothAdapter mAdapter = null;
	BluetoothSocket socket = null;
	TextView tvStatus;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tvStatus = (TextView) findViewById(R.id.tvStatus);
		tvStatus.setText("Disconnected");
		
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mAdapter == null)
			finish();
		//Makes the device discoverable
		if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE || !mAdapter.isEnabled()){
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVER_DURATION);
			startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
		}
		
		if (mAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
			(new AcceptThread()).start();
		
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (requestCode == REQUEST_ENABLE_BT)
			if (resultCode == RESULT_CANCELED && (!mAdapter.isEnabled() || mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE))
				finish();
	}
	
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	    
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
	        } catch (IOException e) { }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	        socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	            	try {
						mmServerSocket.close();
					} catch (IOException e) { }
	            	break;
	            }
	        }
	        //Code for handling data flow after receiving BluetoothSocket socket
	        tvStatus.setText("Connected");
	        (new ConnectedThread()).start();
	    }
	}
	private class ConnectedThread extends Thread{
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    
	    public ConnectedThread() {
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	    
	    public void run(){

	    }
	}
	
}
