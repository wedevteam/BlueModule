package com.wedevteam.bluemodule;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wedevteam.bluemodule.Database._Database;
import com.wedevteam.bluemodule.Database.tables.BModule;

import java.util.ArrayList;
import java.util.List;


public class ElencoSchedulatoActivity extends AppCompatActivity {

    // CONST
    private static final int PERMISSION_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final long SCAN_PERIOD = 10000;
    private final String prename = "YYmJ4z";

    // UI
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;

    // DB
    _Database db;

    // VAR
    private ArrayList<BModule> bModules = new ArrayList<BModule>();
    private ArrayList<BModule> scansioneAttuale = new ArrayList<BModule>();
    private boolean isScanning = false;
    private Handler scanhandler = new Handler();
    private ElencoSchedulatoActivity.ElencoAdapter elencoAdapter;

    // BLE
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothDevice device;
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<BluetoothDevice> bluetoothDevicesAttuali = new ArrayList<BluetoothDevice>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_elenco);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUI();
        setDB();
        getModulesFromDB();
        setUI();
        setRecycler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            askForPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isScanning){
            isScanning=false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
    // ==============================================
    // UI
    // ==============================================
    public void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    private void setUI() {
        recyclerView = findViewById(R.id.lista);
    }
    private void setRecycler() {
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }
    private void fillRecycler() {
        elencoAdapter = new ElencoAdapter(bModules, this);
        recyclerView.setAdapter(elencoAdapter);
        elencoAdapter.notifyDataSetChanged();
    }
    class ElencoAdapter extends RecyclerView.Adapter<ElencoHolder> {
        private LayoutInflater inflater;
        private ArrayList<BModule> elenco;
        // Costruttore
        public ElencoAdapter(ArrayList<BModule> elenco, ElencoSchedulatoActivity ElencoSchedulatoActivity) {
            inflater = LayoutInflater.from(ElencoSchedulatoActivity.this);
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
            int index = position;
            BModule f = elenco.get(index);
            holder.setData(f, position, "");
        }
        @Override
        public int getItemCount() {
            return elenco.size();
        }
    }

    class ElencoHolder extends RecyclerView.ViewHolder {
        TextView nome, modo;
        Button configura;
        Button stato;
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
            sb.append(f1.getNome().substring(6));
            sb.append(" ");
            sb.append(f1.getMSG());
            nome.setText(sb.toString());
            modo.setText(f1.getPW1().isEmpty() && f1.getPW2().isEmpty() ? "NORMALE" : "PASSWORD");
            if (modo.getText().toString().equals("PASSWORD")) {
                modo.setTextColor(Color.RED);
            } else {
                modo.setTextColor(Color.parseColor("#9ca9ae"));
            }

            configura.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < bluetoothDevices.size(); i++) {
                        if (bluetoothDevices.get(i).getAddress().equals(f1.getMACAddress())) {
                            SharedPreferences sharedPreferences = getSharedPreferences("BTSP", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("stopscan", "S");
                            editor.apply();

                            Intent intent = new Intent(ElencoSchedulatoActivity.this, FunzioniActivity.class);
                            intent.putExtra("device", bluetoothDevices.get(i));
                            intent.putExtra("nomedevice", bModules.get(i).getNome());
                            startActivity(intent);
                            if (isScanning){
                                isScanning=false;
                                bluetoothLeScanner.stopScan(leScanCallback);
                            }
                            ElencoSchedulatoActivity.this.finish();
                            break;
                        }
                    }

                }
            });
        }
    }
    // ==============================================
    // DB
    // ==============================================
    public void setDB() {
        db = _Database.getInstance(this);
    }
    private void getModulesFromDB() {
        bModules = new ArrayList<>(getMainData());
    }
    public List<BModule> getMainData() {
        return db.bModuleDao().getAll();
    }
    // ==============================================
    // PERMESSI
    // ==============================================
    private void askForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                setAdapter();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showInContextUI();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
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
                                setAdapter();
                            } else {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
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
                setAdapter();
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

    // =========================================================================================
    // BLE
    // =========================================================================================
    private void setAdapter() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        try {
            bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
            } else {
                abilitaBT();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void abilitaBT() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

            scanDevices();
        }
    }
    private void scanDevices2(){
        if (!isScanning) {
            // Stops scanning after a pre-defined scan period.
            scanhandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                    copiaScansione();
                    scansioneAttuale.clear();
                    bluetoothDevicesAttuali.clear();
                    SharedPreferences sharedPreferences = getSharedPreferences("BTSP", MODE_PRIVATE);
                    String v = sharedPreferences.getString("stopscan", "N");
                    if (v.equals("N")){
                        scanDevices();}
                }
            }, SCAN_PERIOD);
            isScanning = true;
           // bluetoothAdapter.startLeScan(null, leScanCallback2);
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            copiaScansione();
            scansioneAttuale.clear();
            bluetoothDevicesAttuali.clear();
            SharedPreferences sharedPreferences = getSharedPreferences("BTSP", MODE_PRIVATE);
            String v = sharedPreferences.getString("stopscan", "N");
            if (v.equals("N")){
                scanDevices();}
        }
    }
    private void scanDevices() {
        if (!isScanning) {
            // Stops scanning after a pre-defined scan period.
            scanhandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                    copiaScansione();
                    scansioneAttuale.clear();
                    bluetoothDevicesAttuali.clear();
                    SharedPreferences sharedPreferences = getSharedPreferences("BTSP", MODE_PRIVATE);
                    String v = sharedPreferences.getString("stopscan", "N");
                    if (v.equals("N")){
                    scanDevices();}
                }
            }, SCAN_PERIOD);
            isScanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
            copiaScansione();
            scansioneAttuale.clear();
            bluetoothDevicesAttuali.clear();
            SharedPreferences sharedPreferences = getSharedPreferences("BTSP", MODE_PRIVATE);
            String v = sharedPreferences.getString("stopscan", "N");
            if (v.equals("N")){
                scanDevices();}
        }
    }
    private void copiaScansione() {
        ArrayList<BModule> supp = new ArrayList<>();
        ArrayList<BluetoothDevice> suppDevices = new ArrayList<>();
        ArrayList<BluetoothDevice> suppNames = new ArrayList<>();
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

            Log.i("NAME OF DEVICE", bt.getName());
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
        bluetoothDevices.addAll(suppDevices);
        if (elencoAdapter!=null)
        elencoAdapter.notifyDataSetChanged();
    }

    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    device = result.getDevice();
                    BModule bModule = new BModule();
                    bModule.setAlias("");
                    bModule.setDataAttivazione("");
                    bModule.setMACAddress(device.getAddress());
                    bModule.setNome(result.getScanRecord().getDeviceName());
                    bModule.setMSG("");
                    bModule.setNuovoNome("");
                    bModule.setStato(String.valueOf(device.getBondState()));
                    bModule.setPW2("");
                    bModule.setPW1("");
                    bModule.setTipo(String.valueOf(device.getType()));

                    boolean isInsert = false;
                    for (BModule b : bModules) {
                        if (b.getMACAddress().equals(bModule.getMACAddress())) {
                            isInsert = true;
                        }
                    }
                    if (device.getName() != null) {
                        if (!device.getName().isEmpty() && (device.getName().substring(0,6).equals(prename) || device.getName().substring(0,4).equals("Flow"))) {
                            scansioneAttuale.add(bModule);
                            bluetoothDevicesAttuali.add(device);
                        }
                    }
                    if (!isInsert) {
                        if (device.getName() != null) {
                            if (!device.getName().isEmpty() && device.getName().substring(0,6).equals(prename)||device.getName().substring(0,4).equals("Flow")) {
                                bModules.add(bModule);
                                bluetoothDevices.add(device);
                                fillRecycler();
                            }
                        }
                    }
                }
            };

}