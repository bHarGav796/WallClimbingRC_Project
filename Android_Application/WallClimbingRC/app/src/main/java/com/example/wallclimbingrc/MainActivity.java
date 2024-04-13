package com.example.wallclimbingrc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
//    CONNECT BUTTON
    private Button connectButton; // Declare the Button variable
// 1. BLUETOOTH DECLARATIONS
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice selectedDevice;
    private BluetoothSocket bluetoothSocket;
    private boolean isConnected = false;
    private Spinner pairedDevSpinner;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final UUID HC_05_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

//  2. CAMERA DECLARATIONS
    private static final String TAG = "MainActivity::";
    private HandlerThread stream_thread,flash_thread;
    private Handler stream_handler,flash_handler;
    private Button flash_button;
    private ImageView monitor;
    private String ip_text;
    private final int ID_CONNECT = 200;
    private final int ID_FLASH = 201;
    private boolean flash_on_off = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check and request Bluetooth and other necessary permissions at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }
        pairedDevSpinner = findViewById(R.id.pairedDevSpinner);
        connectButton = findViewById(R.id.retryConnectionButton);
        ImageView connectionStatusLight = findViewById(R.id.connectionStatusLight);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
//            finish();
            return;
        }

        // Check if Bluetooth is enabled, and request to enable it if not
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // Initialize paired devices spinner
        pairedDevicesArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        pairedDevicesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pairedDevSpinner.setAdapter(pairedDevicesArrayAdapter);

        // Set up the paired devices list and add to the spinner
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }

        pairedDevSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String deviceInfo = (String) pairedDevSpinner.getSelectedItem();
                String deviceAddress = deviceInfo.substring(deviceInfo.indexOf("\n") + 1);
                selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedDevice = null;
            }
        });

        // Handle connection button click
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedDevice != null) {
                    if (!isConnected) {
                        connectToBluetoothDevice(selectedDevice, connectionStatusLight);
                    } else {
                        disconnectFromBluetoothDevice(connectionStatusLight);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please select a paired device.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton btnUp = findViewById(R.id.btnUp);
        ImageButton btnDown = findViewById(R.id.btnDown);
        ImageButton btnStop = findViewById(R.id.btnStop);
        ImageButton btnLeft = findViewById(R.id.btnLeft);
        ImageButton btnRight = findViewById(R.id.btnRight);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    sendCommand('S');
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to a device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    sendCommand('F');
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to a device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    sendCommand('B');
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to a device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    sendCommand('R');
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to a device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConnected) {
                    sendCommand('L');
                } else {
                    Toast.makeText(MainActivity.this, "Not connected to a device", Toast.LENGTH_SHORT).show();
                }
            }
        });

// * * * * * * * * * *  CAMERA OPERATIONS * * * * * * * * * *

        findViewById(R.id.btnCamToggle).setOnClickListener(this);
        findViewById(R.id.btnFlashToggle).setOnClickListener(this);

        flash_button = findViewById(R.id.btnFlashToggle);
        monitor = findViewById(R.id.monitor);
            ip_text = "192.168.200.190";

        stream_thread = new HandlerThread("http");
        stream_thread.start();
        stream_handler = new MainActivity.HttpHandler(stream_thread.getLooper());

        flash_thread = new HandlerThread("http");
        flash_thread.start();
        flash_handler = new MainActivity.HttpHandler(flash_thread.getLooper());
    }
// * - * - * - * - * - * - * - * - * - * - * - * - * - * - * - * -
// * - * - * - * - * - * - * - * - * - * - * - * - * - * - * - * -


// * * * * * * * * * *  CONNECT & DISCONNECT BLUETOOTH * * * * * * * * * *
            // 1. Connect to the selected Bluetooth device
                @SuppressLint("MissingPermission")
                private void connectToBluetoothDevice(BluetoothDevice device, ImageView connectionStatusLight) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(HC_05_UUID);
                    bluetoothSocket.connect();
                    isConnected = true;
                    connectionStatusLight.setImageResource(R.drawable.circle_green); // Set green light
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                    connectButton.setText("DISCONNECT");
                } catch (IOException e) {
                    isConnected = false;
//                    Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "Connection timed out!", Toast.LENGTH_SHORT).show();

                    try {
                        bluetoothSocket.close();
                    } catch (IOException closeException) {
                        // Handle any exceptions during close()
                    }
                    connectionStatusLight.setImageResource(R.drawable.circle_red); // Set red light
                    connectButton.setText("CONNECT");
                }
            }

            // 2. Disconnect from the connected Bluetooth device
            private void disconnectFromBluetoothDevice(ImageView connectionStatusLight) {
                try {
                    bluetoothSocket.close();
                    isConnected = false;
                    connectionStatusLight.setImageResource(R.drawable.circle_red); // Set red light
                    Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
                    connectButton.setText("CONNECT");
                } catch (IOException e) {
                    // Handle any exceptions during close()
                }
            }

// * * * * * * * * * * SEND A CHARACTER COMMAND TO BLUETOOTH DEVICE * * * * * * * * * *
    private void sendCommand(char command) {
        try {
            if (bluetoothSocket != null) {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(command);
                outputStream.flush();
                Toast.makeText(this, "Sent command: " + command, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth socket is not available", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to send command: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

// * * * * * * * * * * HANDEL CAMERA CLICKS * * * * * * * * * *
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnCamToggle) {
                    stream_handler.sendEmptyMessage(ID_CONNECT);
                } else if (v.getId() == R.id.btnFlashToggle) {
                    flash_handler.sendEmptyMessage(ID_FLASH);
                }
            }


    // * * * * * * * * * * HTTP_HANDLER & CAMERA CONTROLS * * * * * * * * * *
            // 1. HTTP HANDLER
            private class HttpHandler extends Handler {
                public HttpHandler(Looper looper) {
                    super(looper);
                }
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what)
                    {
                        case ID_CONNECT:
                            VideoStream();
                            break;
                        case ID_FLASH:
                            SetFlash();
                            break;
                        default:
                            break;
                    }
                }
            }

            // 2. TOGGLE Camera ON/OFF
            private void VideoStream() {
                String stream_url = "http://" + ip_text + ":81/stream";

                BufferedInputStream bis = null;
                FileOutputStream fos = null;
                try {
                    URL url = new URL(stream_url);
                    try {
                        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                        huc.setRequestMethod("GET");
                        huc.setConnectTimeout(1000 * 5); // Set a timeout of 5 seconds
                        huc.setReadTimeout(1000 * 5);
                        huc.setDoInput(true);
                        huc.connect();

                        int responseCode = huc.getResponseCode();
                        if (responseCode == 200) {
                            InputStream in = huc.getInputStream();

                            InputStreamReader isr = new InputStreamReader(in);
                            BufferedReader br = new BufferedReader(isr);

                            String data;

                            int len;
                            byte[] buffer;

                            while ((data = br.readLine()) != null) {
                                if (data.contains("Content-Type:")) {
                                    data = br.readLine();

                                    len = Integer.parseInt(data.split(":")[1].trim());

                                    bis = new BufferedInputStream(in);
                                    buffer = new byte[len];

                                    int t = 0;
                                    while (t < len) {
                                        t += bis.read(buffer, t, len - t);
                                    }

                                    Bytes2ImageFile(buffer, getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/0A.jpg");

                                    final Bitmap bitmap = BitmapFactory.decodeFile(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/0A.jpg");

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            monitor.setImageBitmap(bitmap);
                                        }
                                    });
                                }
                            }
                        } else {
                            // Display a toast message for other response codes
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Connection failed. Response Code: " + responseCode, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        if (e instanceof SocketTimeoutException) {
                            // Handle timeout exception
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Connection lost!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            e.printStackTrace();
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bis != null)
                            bis.close();
                        if (fos != null)
                            fos.close();
                        stream_handler.sendEmptyMessageDelayed(ID_CONNECT, 3000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

    // 3. TOGGLE Camera Flash ON/OFF
            private void SetFlash() {
                flash_on_off ^= true;

                String flash_url;
                if(flash_on_off){
                    flash_url = "http://" + ip_text+ ":80/led?var=flash&val=255";
                }
                else {
                    flash_url = "http://" + ip_text + ":80/led?var=flash&val=0";
                }

                try
                {

                    URL url = new URL(flash_url);

                    HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                    huc.setRequestMethod("GET");
                    huc.setConnectTimeout(1000 * 5);
                    huc.setReadTimeout(1000 * 5);
                    huc.setDoInput(true);
                    huc.connect();
                    if (huc.getResponseCode() == 200)
                    {
                        InputStream in = huc.getInputStream();

                        InputStreamReader isr = new InputStreamReader(in);
                        BufferedReader br = new BufferedReader(isr);
                    }

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

    //* * * * * * * * * * Bytes2ImageFile FUNCTION * * * * * * * * * *
    private void Bytes2ImageFile(byte[] bytes, String fileName)
    {
        try
        {
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    // * * * * * * * * * * ON_DESTROY  * * * * * * * * * *
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isConnected) {
            disconnectFromBluetoothDevice((ImageView) findViewById(R.id.connectionStatusLight));
        }
    }
}

