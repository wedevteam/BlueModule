package com.wedevteam.bluemodule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wedevteam.bluemodule.Database._Database;
import com.wedevteam.bluemodule.Database.tables.BModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ElencoActivity extends AppCompatActivity {

    // VAR
    private ArrayList<BModule> bModules = new ArrayList<BModule>();
    private ArrayList<BModule> scansioneAttuale = new ArrayList<BModule>();
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> bluetoothDevicesAttuali = new ArrayList<BluetoothDevice>();
    _Database db;
    ElencoAdapter elencoAdapter;

    // UI
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;


    static final int PERMISSION_CODE = 1;
    static final int REQUEST_ENABLE_BT = 2;
    static final UUID MY_UUID = UUID.randomUUID();
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice device;
    private Handler mHandler; // handler that gets info from Bluetooth service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elenco);
        setUI();
        setRecycler();
        // Legge Archivio
        setDB();
        bModules = new ArrayList<>(getMainData());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            start();
        }
        registraReceiver();
        verificaBT();
        abilitaBT();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
                unregisterReceiver(receiver);


    }

    // ==============================================
    // UI
    // ==============================================
    private void setUI() {
        recyclerView = findViewById(R.id.lista);
    }
    class ElencoAdapter extends RecyclerView.Adapter<ElencoHolder>{
        private LayoutInflater inflater;
        private ArrayList<BModule> elenco;
        // Costruttore
        public ElencoAdapter(ArrayList<BModule> elenco, ElencoActivity elencoActivity) {
            inflater = LayoutInflater.from(ElencoActivity.this);
            this.elenco = elenco;
        }

        @NonNull
        @Override
        public ElencoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.itemlista, parent, false);
            ElencoHolder holder = new ElencoHolder(view);
            return holder;
        }
        @Override
        public void onBindViewHolder(@NonNull ElencoHolder holder, int position) {
            int index = position ;
            BModule f = elenco.get(index);
            holder.setData(f,  position, "");
        }
        @Override
        public int getItemCount() {
            return elenco.size();
        }
    }

    class ElencoHolder extends RecyclerView.ViewHolder{
        TextView nome,modo;
        Button configura;
        SwitchCompat stato;
        LinearLayout list_item;
        public ElencoHolder(@NonNull View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.nome);
            modo = itemView.findViewById(R.id.modo);
            configura = itemView.findViewById(R.id.configura);
            stato = itemView.findViewById(R.id.stato);
            list_item = itemView.findViewById(R.id.list_item);
        }
        public void setData(final BModule f1, int position, String tipo) {
            StringBuilder sb = new StringBuilder();
            sb.append("NOME: ");
            sb.append(f1.getNome());
            sb.append(" ");
            sb.append(f1.getMSG());
            nome.setText(sb.toString());
            modo.setText(f1.getPW1().isEmpty() && f1.getPW2().isEmpty() ? "MODO: NORMALE" : "MODO: PASSWORD");
            if (modo.getText().toString().equals("PASSWORD")){
                modo.setTextColor(Color.RED);
            }else{
                modo.setTextColor(Color.BLACK);
            }
            stato.setChecked(f1.getDataAttivazione().equals(""));
            configura.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < bluetoothDevices.size(); i++) {
                        if (bluetoothDevices.get(i).getAddress().equals(f1.getMACAddress())){
                            Intent intent = new Intent(ElencoActivity.this,FunzioniActivity.class);
                            intent.putExtra("device",bluetoothDevices.get(i));
                            startActivity(intent);
                            bluetoothAdapter.cancelDiscovery();
                            ElencoActivity.this.finish();
                            break;
                        }
                    }

                }
            });
        }
    }

    private void setRecycler() {
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void fillRecycler() {
        elencoAdapter = new ElencoAdapter(bModules,this);
        recyclerView.setAdapter(elencoAdapter);
        elencoAdapter.notifyDataSetChanged();
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

    // ==============================================
    // BT
    // ==============================================

    // Registra receiver
    private void registraReceiver(){
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
    }

    // Eventi receiver
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // BluetoothDevice trovato
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BModule bModule = new BModule();
                bModule.setAlias("");
                bModule.setDataAttivazione("");
                bModule.setMACAddress(device.getAddress());
                bModule.setNome(device.getName()    );
                bModule.setMSG("");
                bModule.setNuovoNome("");
                bModule.setStato(String.valueOf(device.getBondState()));
                bModule.setPW2("");
                bModule.setPW1("");
                bModule.setTipo(String.valueOf(device.getType()));
                try {
                    ParcelUuid[] x = device.getUuids();
                    ParcelUuid[] y = device.getUuids();
                    if (x!=null){
                        bModule.setMSG("UUID = "+x[0].getUuid());
                    }else{
                        bModule.setMSG("UUID = LEGGIBILE");
                    }
                }catch (Exception e){
                    bModule.setMSG("UUID = LEGGIBILE");
                    String s = (e.getMessage());
                }
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (uuidExtra!=null)
                    for (Parcelable p: uuidExtra){
                        bModule.setUUID(bModule.getUUID()+p+",");
                    }
                boolean isInsert = false;
                for (BModule b:bModules) {
                    if (b.getMACAddress().equals(bModule.getMACAddress())){
                        isInsert=true;
                    }
                }
                if (device.getName()!=null){
                    if (!device.getName().isEmpty()){
                        scansioneAttuale.add(bModule);
                        bluetoothDevicesAttuali.add(device);
                    }
                }
                if (!isInsert){
                    if (device.getName()!=null){
                        if (!device.getName().isEmpty()){
                            bModules.add(bModule);
                            bluetoothDevices.add(device);
                            fillRecycler();
                        }
                    }
                }

                //connectBT();
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                bluetoothAdapter.cancelDiscovery();
                Toast.makeText(context, "STOP", Toast.LENGTH_SHORT).show();
                copiaScansione();
                                                                                                                                                                                                        scansioneAttuale.clear();
                                                                                                                                                                                                        bluetoothDevicesAttuali.clear();
                bluetoothAdapter.startDiscovery();
                Toast.makeText(context, "RESTART", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private void copiaScansione() {
        ArrayList<BModule> supp = new ArrayList<>();
        ArrayList<BluetoothDevice> suppDevices = new ArrayList<>();
        bModules.clear();
        bluetoothDevices.clear();
        for (BModule b:scansioneAttuale) {
            boolean isInserted=false;
            for (int i = 0; i <supp.size() ; i++) {
                if(supp.get(i).getMACAddress().equals(b.getMACAddress())){
                    isInserted=true;
                }
            }
            if (!isInserted){
                supp.add(b);
            }
        }
        for (BluetoothDevice bt:bluetoothDevicesAttuali) {
            boolean isInserted=false;
            for (int i = 0; i <suppDevices.size() ; i++) {
                if(suppDevices.get(i).getAddress().equals(bt.getAddress())){
                    isInserted=true;
                }
            }
            if (!isInserted){
                suppDevices.add(bt);
            }
        }
        bModules.addAll(supp);
        fillRecycler();
    }

    // Verifica se BT è supportato
    private void verificaBT() {
        if (bluetoothAdapter == null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("BLUETOOTH NON SUPPORTATO");
            alertDialogBuilder
                    .setMessage("Questa App non può funzionare in questo device poichè il Bluetooth non è supportato")
                    .setCancelable(false)
                    .setNegativeButton("ESCI", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }
    // Verifica se BT è abilitato
    private void abilitaBT() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            scanBT();
        }
    }

    // Scamsione BT
    private void scanBT(){
        scanPaired();
        discoverBT();
    }
    private void discoverBT(){
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }
    private void connectBT(){
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("TAG", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("TAG", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the client socket", e);
            }
        }
    }
    private void scanPaired() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            StringBuilder sb = new StringBuilder();

            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                sb.append(deviceName);
                sb.append(deviceHardwareAddress);
                sb.append("\n");
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode==REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                scanBT();
            }else{
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("BLUETOOTH NON ABILITATO");
                alertDialogBuilder
                        .setMessage("Questa App non può funzionare in questo device poichè il Bluetooth non è abilitato")
                        .setCancelable(false)
                        .setNegativeButton("ESCI DALL'APP", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("ABILITA", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }

    }
    // RICHIESTA PERMESSI
    // ==============================
    private void start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showInContextUI();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            }
        }
    }
    private void showInContextUI() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Accesso a funzioni background");
        alertDialogBuilder
                .setMessage(getString(R.string.permessobg))
                .setNegativeButton("ESCI DALL'APP", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                        dialogInterface.cancel();
                    }
                })
                .setPositiveButton("RICHIEDI PERMESSO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(ElencoActivity.this, "PERMESSO OK", Toast.LENGTH_SHORT).show();
                            } else {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
                            }
                        }
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("PERMESSO NON CONCESSO");
                alertDialogBuilder
                        .setMessage("Questa App non può funzionare se non viene concesso il permesso. Vai sulle impostazioni di android per concedere il permesso")
                        .setCancelable(false)
                        .setNegativeButton("ESCI", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    // ==============================
    private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("TAG", "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("TAG", "Error occurred when creating output stream", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    // TODO connectionLost();
                    Log.d("TAG", "Input stream was disconnected", e);
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e("TAG", "Error occurred when sending data", e);

            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("TAG", "Could not close the connect socket", e);
            }
        }
    }
}