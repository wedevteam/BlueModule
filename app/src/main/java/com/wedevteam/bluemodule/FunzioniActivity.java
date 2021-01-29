package com.wedevteam.bluemodule;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wedevteam.bluemodule.Database._Database;
import com.wedevteam.bluemodule.Database.tables.BModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;

public class FunzioniActivity extends AppCompatActivity  {

    // BTLE
    public final static String ACTION_GATT_CONNECTED = "CONNESSO";
    public final static String ACTION_GATT_DISCONNECTED = "DISCONNESSO";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "SERVIZIOK";
    public final static String ACTION_DATA_AVAILABLE = "DATIDISPONIBILI";
    public final static String EXTRA_DATA = "EXTRADATI";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int connectionState = STATE_DISCONNECTED;
    private boolean connected = false;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final UUID ricezione = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID trasmissione = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID descriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt bluetoothGatt;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
    private BluetoothDevice device;
    BluetoothGattCharacteristic gattCharacteristicgen;
    private  BluetoothGattCharacteristic  TX;
    // UI
    private TextView nomebt;
    private TextView statobt;
    private Button btnmodo;

    _Database db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_funzioni);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUI();
        try {
            Intent intent = getIntent();
            if (intent!=null){
                device=intent.getExtras().getParcelable("device");
                Toast.makeText(this, device.getName(), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            startActivity(new Intent(FunzioniActivity.this,ElencoActivity.class));
            finish();
        }
        setUI();
        setDB();
        connectBT();
    }
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }
    @Override
    protected void onStop() {
        super.onStop();
        closeBT();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gattUpdateReceiver!=null)
        unregisterReceiver(gattUpdateReceiver);
        closeBT();
    }
    // ==============================================
    // BT
    // ==============================================
    private void connectBT(){
        bluetoothGatt = device.connectGatt(this, true, gattCallback,2);
    }
    public void closeBT() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String intentAction;
                    // Connesso
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        connectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        bluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        connectionState = STATE_DISCONNECTED;
                        broadcastUpdate(intentAction);
                    }
                }
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Toast.makeText(FunzioniActivity.this, "ERRORE SERVIZI: " + status, Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                // Characteristic notification
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String value =  Arrays.toString(characteristic.getValue());
        //    bluetoothGatt.readCharacteristic(TX);
        }
        @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    String value =  Arrays.toString(characteristic.getValue());

                    /*charas.remove(charas.get(indexGen));
                    if (charas.size() >= 0) {
                        indexGen--;
                        if (indexGen == -1) {
                            Log.i("Read Queue: ", "Complete");
                        }
                        else {
                            ReadCharacteristics(indexGen);
                        }
                    }*/
                    /*if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        Toast.makeText(ScanActivity.this, "characteristic lette", Toast.LENGTH_SHORT).show();
                    }*/
                }

            };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                    stringBuilder.toString());
        }
        sendBroadcast(intent);
    }
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState("GATT_CONNECTED");
                // invalidateOptionsMenu();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState("GATT_DISCONNECTED");
                clearUI();
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(getSupportedGattServices());
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                //   displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }
    private void displayGattServices(List<BluetoothGattService> gattServices)  {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown Service";
        String unknownCharaString = "Unknown Characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                currentCharaData.put(LIST_NAME, lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                if (gattCharacteristic.getUuid().equals(ricezione)){
                    gattCharacteristicgen=gattCharacteristic;
                    statobt.setText(statobt.getText().toString()+" ricez. OK");
                }
                if (gattCharacteristic.getUuid().equals(trasmissione)){
                    List<UUID> descriptorUUIDsList     = new ArrayList<>();
                    List<BluetoothGattDescriptor> bluetoothGattDescriptorsList = gattCharacteristic.getDescriptors();
                    for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattDescriptorsList)
                    {
                        descriptorUUIDsList.add(bluetoothGattDescriptor.getUuid());
                    }
                    TX = gattCharacteristic;
                    if (!bluetoothGatt.readCharacteristic(gattCharacteristic)){
                        String x = "NO";
                    }
                    statobt.setText(statobt.getText().toString()+" trasm. OK");
                    setAdv(gattCharacteristic, trasmissione);
                    String comando = "v sw";
                    try {
                        byte[] bytes = comando.getBytes("UTF-8");
                        String text = new String(bytes, "UTF-8");
                        final int properties = gattCharacteristicgen.getProperties();
                        int tipo = -1;
                        if ((properties & PROPERTY_WRITE) != 0) {
                            tipo = WRITE_TYPE_DEFAULT;
                        } else {
                            tipo = WRITE_TYPE_NO_RESPONSE;
                        }
                        gattCharacteristicgen.setValue(bytes);
                        gattCharacteristicgen.setWriteType(tipo);
                        if (!bluetoothGatt.writeCharacteristic(gattCharacteristicgen)) {
                            Log.e("TAG", String.format("ERROR: writeCharacteristic failed for characteristic: %s", gattCharacteristicgen.getUuid()));
                            //completedCommand();
                        } else {

                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }
    private void setAdv(BluetoothGattCharacteristic gattCharacteristic, UUID uuid) {
        boolean isEnabling = true;
        final int properties = gattCharacteristic.getProperties();

        if (isEnabling){
            bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
            BluetoothGattDescriptor descriptorBT = gattCharacteristic.getDescriptor(descriptor);
            descriptorBT.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptorBT);
        }

    }

    private static HashMap<String, String> attributes = new HashMap();
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
    private void updateConnectionState(final String st) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectionState == STATE_CONNECTED){
                    statobt.setText("CONNESSO");
                    statobt.setTextColor(Color.WHITE);
                }
                if (connectionState == STATE_DISCONNECTED){
                    statobt.setText("DISCONNESSO");
                    statobt.setTextColor(Color.parseColor("#9ca9ae"));
                }
            }
        });
    }
    private void clearUI() {
        //  mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }



    // ==============================================
    // UI
    // ==============================================
    private void setUI() {
        nomebt = findViewById(R.id.nomebt);
        statobt = findViewById(R.id.statobt);
        btnmodo = findViewById(R.id.btnmodo);
        btnmodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] v = TX.getValue();
            }
        });
        nomebt.setText(device.getName());
    }
    public void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    //==============================================
    // DB
    // ==============================================
    public void setDB() {
        db = _Database.getInstance(this);
    }
    public List<BModule> getMainData() {
        return db.bModuleDao().getAll() ;
    }

}