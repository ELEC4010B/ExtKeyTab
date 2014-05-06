package ece.course.extkeytab;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;

public class MainActivity extends Activity {
	final int REQUEST_ENABLE_BT = 100;
	final int DISCOVER_DURATION = 0;
	int RESULT_CODE = 0;
	Intent ResultData;
	final String NAME = "ExtKeyTab";
	final UUID MY_UUID = UUID
			.fromString("4e1422d0-c62c-11e3-9c1a-0800200c9a66");
	BluetoothAdapter mAdapter = null;
	BluetoothSocket socket = null;
	TextView tvStatus;
	public EditText etText;
	Button btnSave;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		tvStatus = (TextView) findViewById(R.id.tvStatus);
		tvStatus.setText("Disconnected");
		etText = (EditText) findViewById(R.id.etText);
		btnSave = (Button) findViewById(R.id.btnSave);

		btnSave.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				AlertDialog.Builder alert = new AlertDialog.Builder(view
						.getContext());

				alert.setTitle("Please input file name");

				// Set an EditText view to get user input
				final EditText input = new EditText(view.getContext());
				alert.setView(input);

				alert.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								String filename = input.getText().toString();
								try {
									File root = new File(Environment
											.getExternalStorageDirectory(),
											"Extended Keyboard");
									if (!root.exists()) {
										root.mkdirs();
									}
									File gpxfile = new File(root, filename);
									FileWriter writer = new FileWriter(gpxfile);
									writer.append(etText.getText().toString());
									writer.flush();
									writer.close();
									Toast.makeText(getBaseContext(), "Saved",
											Toast.LENGTH_SHORT).show();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						});

				alert.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Canceled.
							}
						});

				alert.show();
			}
		});

		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mAdapter == null) {
			Toast.makeText(MainActivity.this, "No Bluetooth adapter found",
					Toast.LENGTH_LONG).show();
			finish();
		} else {
			// Makes the device discoverable
			if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
					|| !mAdapter.isEnabled()) {
				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(
						BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
						DISCOVER_DURATION);
				startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
			}

			if (mAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
				(new AcceptThread()).start();
		}

	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT)
			if (resultCode == RESULT_CANCELED
					&& (!mAdapter.isEnabled() || mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE))
				finish();
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
			}
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
					} catch (IOException e) {
					}
					break;
				}
			}
			// Code for handling data flow after receiving BluetoothSocket
			// socket
			runOnUiThread(new Runnable() {
				public void run() {
					tvStatus.setText("Connected");
				}
			});
			(new ConnectedThread()).start();
		}
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;

		public ConnectedThread() {
			InputStream tmpIn = null;
			try {
				tmpIn = socket.getInputStream();
			} catch (IOException e) {
			}
			mmInStream = tmpIn;
		}

		public void run() {
			int bytes;
			final byte[] buffer = new byte[1024];

			while (true) {
				try {
					bytes = mmInStream.read(buffer);
				} catch (IOException e) {
					// Toast.makeText(MainActivity.this, "IO Error",
					// Toast.LENGTH_LONG).show();
					break;
				}
				if (bytes == -1)
					continue;

				else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							String s = etText.getText().toString();
							if ((buffer[0] != 'L') && (buffer[0] != 'R')
									&& (buffer[0] != 'U') && (buffer[0] != 'D')) {
								s = s + (char) buffer[0];
								etText.setText(s);
								etText.setSelection(s.length());
							} else {
								if (buffer[0] == 'L') {
									if (etText.getSelectionEnd() - 1 >= 0)
										etText.setSelection(etText
												.getSelectionEnd() - 1);
								} else if (buffer[0] == 'R') {
									if (etText.getSelectionEnd() + 1 <= s
											.length())
										etText.setSelection(etText
												.getSelectionEnd() + 1);
								} else if (buffer[0] == 'U') {
									int end = etText.getSelectionEnd();
									Layout layout = etText.getLayout();
									int line = layout.getLineForOffset(end);
									if (line > 0) {
										int move;
										if (layout.getParagraphDirection(line) == layout
												.getParagraphDirection(line - 1)) {
											float h = layout
													.getPrimaryHorizontal(end);
											move = layout
													.getOffsetForHorizontal(
															line - 1, h);
										} else {
											move = layout
													.getLineStart(line - 1);
										}
										etText.setSelection(move);
									}
								} else if (buffer[0] == 'D') {
									int end = etText.getSelectionEnd();
									Layout layout = etText.getLayout();
									int line = layout.getLineForOffset(end);
									if (line < layout.getLineCount() - 1) {
										int move;
										if (layout.getParagraphDirection(line) == layout
												.getParagraphDirection(line + 1)) {
											float h = layout
													.getPrimaryHorizontal(end);
											move = layout
													.getOffsetForHorizontal(
															line + 1, h);
										} else {
											move = layout
													.getLineStart(line + 1);
										}
										etText.setSelection(move);
									}
								}
							}
						}
					});
				}
				// TODO: Handle disconnect request
			}
		}
	}

}
