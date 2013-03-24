package de.thomaslaemmlein.tlaebluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class TlaeBluetoothTest extends Activity implements 
			INumberReceiver {
    // Debugging
    private static final String TAG = "TlaeBluetoothTest";
    private static final boolean D = true;
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    
    private TlaeBluetoothTestService mTlaeService = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
    private TextView mTitle;
    
    private EditText m_CurrentNumberEditText;
    private CounterView m_CounterView;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
   
    

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tlae_bluetooth_test);
		
		mTitle = (TextView) findViewById(R.id.Title_TextView);
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        // If BT is not on, request that it be enabled.
        // onActivityResult will be called
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mTlaeService == null) setupService();
        }
	}

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mTlaeService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mTlaeService.getState() == TlaeBluetoothTestService.STATE_NONE) {
              // Start the Bluetooth chat services
            	mTlaeService.start();
            }
        }
    }	

	private void setupService()
	{
		Log.d(TAG, "setupService()");
		
		m_CounterView = (CounterView) findViewById(R.id.counterView1);
        
		m_CounterView.SetNumberReceiver(this);
        
        final Button sendNumber_Button = (Button) findViewById(R.id.sendNumber_Button);
        
        m_CurrentNumberEditText = (EditText) findViewById(R.id.currentNumber_EditText);
        
        sendNumber_Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	
            	String strNumber = m_CurrentNumberEditText.getText().toString();
            	
            	int number = Integer.parseInt(strNumber);
            	
            	m_CounterView.SetNumber(number);
            	
            	sendMessage(strNumber);
            }
        });		
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mTlaeService = new TlaeBluetoothTestService(this, mHandler);

	}
	
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mTlaeService != null) mTlaeService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
	
	
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mTlaeService.getState() != TlaeBluetoothTestService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mTlaeService.write(send);
        }
    }   
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case TlaeBluetoothTestService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    if ( mConnectedDeviceName != null )
                    {
                    	mTitle.append(mConnectedDeviceName);
                    }
                    break;
                case TlaeBluetoothTestService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case TlaeBluetoothTestService.STATE_LISTEN:
                case TlaeBluetoothTestService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                
            	m_CurrentNumberEditText.setText(readMessage);
            	
            	int number = Integer.parseInt(readMessage);
            	
            	m_CounterView.SetNumber(number);
                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };    
	
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                Toast.makeText(this, "Bluetooth is now enabled, so set up a chat session", Toast.LENGTH_SHORT).show();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, "User did not enable Bluetooth or an error occured", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        Toast.makeText(this, "Attempt to connect to the device", Toast.LENGTH_SHORT).show();
        mTlaeService.connect(device, secure);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_tlae_bluetooth_test, menu);
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    

	@Override
	public void SetNumber(int newNumber) {
		String strNumber = Integer.toString(newNumber);
		m_CurrentNumberEditText.setText(strNumber);
		sendMessage(strNumber);
	}


	

}
