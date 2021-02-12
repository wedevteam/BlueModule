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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.wedevteam.bluemodule.Database._Database;
import com.wedevteam.bluemodule.Database.tables.BModule;
import com.wedevteam.bluemodule.Funzioni.TextUtil;
import com.wedevteam.bluemodule.Servizi.SerialListener;
import com.wedevteam.bluemodule.Servizi.SerialService;
import com.wedevteam.bluemodule.Servizi.SerialSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ElencoSchedulatoActivity extends AppCompatActivity implements ServiceConnection, SerialListener {

    // VARIABILI PER CONNESSIONE
    // ==================================================================================

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
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final UUID ricezione = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID trasmissione = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID descriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGatt bluetoothGatt;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
    BluetoothGattCharacteristic gattCharacteristicgen;
    private  BluetoothGattCharacteristic  TX;
    private final String prename = "YYmJ4z";
    private boolean isFromCambioNome=false;
    ArrayList<BModule>bModules = new ArrayList<>();
    private String statoRele="";
    // ==================================================================================


    // NEW SYSTEM
    private enum Connected { False, Pending, True }
    private ElencoSchedulatoActivity.Connected connected = ElencoSchedulatoActivity.Connected.False;
    private SerialService service;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    TextUtil textUtil = new TextUtil();
    private String newline =textUtil.newline_crlf;
    String comando = "";
    String comandoCompleeto="";
    String nomenuovo="";
    String nomedevice="";
    String statoDevice = "";

    // UI
    private TextView nomebt;
    private TextView statobt;
    private TextView textmodo;
    private TextView textvhw;
    private TextView textvsw;
    private TextView texttempo;
    private TextView textnome;
    private Button btnmodo;
    private Button btnrele;
    private Button btntime;
    private Button btnalexa;
    private Button btnnome;
    private Button btnmem1;
    private Button btninvia1;
    private Button btnmem2;
    private Button btninvia2;
    private TextView textpw2;
    private TextView textpw1;


    private final String commandVSW = "v sw";
    private final String commandVHW = "v hw";
    private final String commandAlexa = "a res";
    private final String commandNome = "n ";
    private final String commandGetModo = "l sta";
    private final String commandGetRele = "l rel";
    private final String commandTempo = "t ";
    private final String commandPwP = "p pri";
    private final String commandPwR = "p rec";
    private final String commandSetModoD = "m def";
    private final String commandSetModoP = "m pwd";
    private final String commandSetReleOn = "r on";
    private final String commandSetReleOff = "r off";

    private String[] errs = new String[]{"OK", "Comando errato", "Argomento 1 non valido", "Argomento 2 non valido", "Password non valida", "Comando non valido"};
    private String[] comandibase = new String[]{commandGetModo,commandGetRele};
    private int indexcomandibase=0;
    private boolean isComandiBase = false;

    private final String rispostaGetModoDef = "def";
    private final String rispostaGetModoPwd = "pwd";

    boolean passwordEsistente = false;
    String password1="";
    String password2="";

    _Database db;



    // CONST
    private static final int PERMISSION_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final long SCAN_PERIOD = 10000;
   // private final String prename = "YYmJ4z";

    // UI
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    RecyclerView.LayoutManager layoutManager;
    Button esci;
    FloatingActionButton scan;
    ProgressBar progressBar;
    // DB
 //   _Database db;

    // VAR
 //   private ArrayList<BModule> bModules = new ArrayList<BModule>();
    private ArrayList<BModule> scansioneAttuale = new ArrayList<BModule>();
    private boolean isScanning = false;
    private Handler scanhandler = new Handler();
    private ElencoSchedulatoActivity.ElencoAdapter elencoAdapter;
    private BModule bModuleScelto = new BModule();

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
    // FUNZIONI DI CONNESSIONE
    // ====================================================================
    private void connectBT(){
        // NEW SYSTEM
        // =================================================================
        try {
            SerialSocket socket = null;
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice deviceFromSocket = bluetoothAdapter.getRemoteDevice(bModuleScelto.getMACAddress());
            status("in connessione...");
            connected = ElencoSchedulatoActivity.Connected.Pending;
            socket = new SerialSocket(getApplicationContext(), deviceFromSocket);
            service.connect(socket);

        } catch (Exception e) {
            onSerialConnectError(e);
        }
        // =================================================================

    }
    private void disconnect() {
        connected = ElencoSchedulatoActivity.Connected.False;
        service.disconnect();
    }
    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str);
        if (str.equals("CONNESSO")){
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            elencoAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
            }
        else{
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.gray)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void send(String str) {
        if(connected != ElencoSchedulatoActivity.Connected.True) {
            Toast.makeText(this, "non connesso", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {

                data = (str + newline).getBytes();
            }

            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }
    private void receive(byte[] data) {
        TextView textView = null;
        byte[] bytes = pulisciBytes(data);
        String msg = new String(bytes);
        if (comando.equals(commandGetModo)){
            Log.i("SEQUENZA COMANDI BASE", "1");
            textView=textmodo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=3){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    if (risposta[2].equals(rispostaGetModoDef)){
                        statoDevice="N";
                        msg="Modo: NORMALE";
                    }else{
                        msg="Modo: PASSWORD";
                        statoDevice="P";
                    }
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }
            CountDownTimer cdt = new CountDownTimer(500,100) {
                @Override
                public void onTick(long l) {

                }
                @Override
                public void onFinish() {
                    if (isComandiBase){
                        ++indexcomandibase;
                        prepareSend(comandibase[1]);
                    }
                }
            }.start();

        }
        if (comando.equals(commandGetRele)) {
            Log.i("SEQUENZA COMANDI BASE", "4");
            textView = texttempo;
            String[] risposta = msg.split(" ");
            if (risposta.length >= 3) {
                int codiceErr = Integer.parseInt(risposta[1]);
                if (codiceErr == 0) {
                    msg = "Stato: " + risposta[2].toUpperCase();
                    statoRele = msg;
                    elencoAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }
            if (isComandiBase) {
                indexcomandibase = 0;
                isComandiBase = false;
            }
        }
        if (comando.equals(commandSetReleOn)) {
            String[] risposta = msg.split(" ");
            if (risposta.length >= 2) {
                int codiceErr = Integer.parseInt(risposta[1]);
                if (codiceErr == 0) {
                    statoRele = "Stato: ON";
                    elencoAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }

        }
        if (comando.equals(commandSetReleOff)) {
            String[] risposta = msg.split(" ");
            if (risposta.length >= 2) {
                int codiceErr = Integer.parseInt(risposta[1]);
                if (codiceErr == 0) {
                    statoRele = "Stato: OFF";
                    elencoAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }

        }
    }
    private byte[] pulisciBytes(byte[] data) {
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            if(data[i]<10 || data[i]>=14){
                bytes[i]=data[i];
            }else{
                bytes[i]=32;
            }
        }
        return bytes;
    }
    private boolean isRecordExist() {
        bModules = new ArrayList<>(getMainData());
        for (int i = 0; i < bModules.size(); i++) {
            if (bModules.get(i).getMACAddress().equals(bModuleScelto.getMACAddress())){
                return true;
            }
        }
        return false;
    }
    public void prepareSend(String cmd){
        comando = cmd;
        comandoCompleeto=comando;
        if (isComandiBase) {
            switch (comando) {
                case commandGetModo:
                    send(commandGetModo);
                    break;
                case commandGetRele:
                    send(commandGetRele);
                    break;
                case commandVHW:
                    send(commandVHW);
                    break;
                case commandVSW:
                    send(commandVSW);
                    break;
                default:
            }
        } else {
            switch (comando) {

            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
    }
    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
    }
    @Override
    public void onSerialConnect() {
        status("CONNESSO");
        connected = ElencoSchedulatoActivity.Connected.True;
        eseguiComandiBase();
    }
    private void eseguiComandiBase() {
        isComandiBase = true;
        prepareSend(comandibase[indexcomandibase]);
    }
    @Override
    public void onSerialConnectError(Exception e) {
        if (isFromCambioNome){
            isFromCambioNome=false;
            connectBT();
        }
    }
    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }
    @Override
    public void onSerialIoError(Exception e) {
        status("connessione persa: " + e.getMessage());
        disconnect();
        CountDownTimer countDownTimer = new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long l) {
                Toast.makeText(ElencoSchedulatoActivity.this, "Tentativo di riconnessione entro 10 secondi...", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFinish() {
                connectBT();
            }
        }.start();
    }
    // ====================================================================


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
        progressBar = findViewById(R.id.progressbar);
        recyclerView = findViewById(R.id.lista);
        esci = findViewById(R.id.esci);
        scan = findViewById(R.id.scan);
        esci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isScanning){
                    isScanning=false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
                finishAndRemoveTask();
            }
        });
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanDevices();
            }
        });
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
        TextView nome, modo,stato1;
        Button configura;
        Button stato;
        LinearLayout list_item;
        FrameLayout bordogrigio,bordobianco;
        public ElencoHolder(@NonNull View itemView) {
            super(itemView);
            bordogrigio = itemView.findViewById(R.id.bordogrigio);
            bordobianco = itemView.findViewById(R.id.bordobianco);
            nome = itemView.findViewById(R.id.nome);
            modo = itemView.findViewById(R.id.modo);
            configura = itemView.findViewById(R.id.configura);
            stato = itemView.findViewById(R.id.stato);
            stato1 = itemView.findViewById(R.id.stato1);
            list_item = itemView.findViewById(R.id.list_item);
        }
        public void setData(final BModule f1, int position, String tipo) {
            StringBuilder sb = new StringBuilder();
            sb.append(f1.getNome().substring(6));
            sb.append(" ");
            sb.append(f1.getMSG());
            if (f1.getMACAddress().equals(bModuleScelto.getMACAddress())){
                bordogrigio.setVisibility(View.GONE);
                bordobianco.setVisibility(View.VISIBLE);
                configura.setTextColor(Color.parseColor("#ffffff"));
                stato.setTextColor(Color.parseColor("#ffffff"));
            }else{
                bordogrigio.setVisibility(View.VISIBLE);
                bordobianco.setVisibility(View.GONE);
                configura.setTextColor(Color.parseColor("#9ca9ae"));
                stato.setTextColor(Color.parseColor("#9ca9ae"));
                stato1.setEnabled(false);
            }
            if (statoDevice.equals("N")){
                modo.setText(R.string.normale);
            }
            if (statoDevice.equals("S")){
                modo.setText(R.string.pw);
            }
            stato1.setText(statoRele);
            if (!statoRele.isEmpty()){
                stato1.setEnabled(true);
            }
            if (statoRele.isEmpty()){
                stato1.setEnabled(false);
            }
            nome.setText(sb.toString());
            modo.setText(f1.getPW1().isEmpty() && f1.getPW2().isEmpty() ? "NORMALE" : "PASSWORD");
            if (modo.getText().toString().equals("PASSWORD")) {
                modo.setTextColor(Color.RED);
            } else {
                modo.setTextColor(Color.parseColor("#9ca9ae"));
            }




            list_item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isScanning){
                        isScanning=false;
                        bluetoothLeScanner.stopScan(leScanCallback);
                    }
                    for (int i = 0; i <bModules.size() ; i++) {
                        if (f1.getMACAddress().equals(bModules.get(i).getMACAddress())){
                            bModuleScelto = new BModule();
                            bModuleScelto = bModules.get(i);
                        }
                    }
                    indexcomandibase=0;
                    progressBar.setVisibility(View.VISIBLE);
                    connectBT();
                    //elencoAdapter.notifyDataSetChanged();
                }
            });
            stato.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String testo = "";
                    testo = statoRele.equals("Stato: ON") ? "OFF" : "ON";
                    if (testo.equals("ON")) {
                        comando = commandSetReleOn;
                    }else{
                        comando = commandSetReleOff;
                    }
                    if (statoDevice.equals("P")){
                        setPasswordEsistente();
                        comandoCompleeto = comando + " "+ password1;
                    }else{
                        comandoCompleeto = comando;
                    }
                    send(comandoCompleeto);
                }
            });
            configura.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < bluetoothDevices.size(); i++) {
                        if (bluetoothDevices.get(i).getAddress().equals(f1.getMACAddress())) {
                            SharedPreferences sharedPreferences = getSharedPreferences("BTSP", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("stopscan", "S");
                            editor.apply();
                            disconnect();
                            Intent intent = new Intent(ElencoSchedulatoActivity.this, FunzioniActivity.class);
                            intent.putExtra("device", bluetoothDevices.get(i));
                            intent.putExtra("nomedevice", bModules.get(i).getNome());
                            startActivity(intent);
                            if (isScanning){
                                isScanning=false;
                                bluetoothLeScanner.stopScan(leScanCallback);
                            }
                            finish();
                            //ElencoSchedulatoActivity.this.finish();
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
      //  bModules = new ArrayList<>(getMainData());
    }
    public List<BModule> getMainData() {
        return db.bModuleDao().getAll();
    }
    private void setPasswordEsistente() {
        bModules = new ArrayList<>(getMainData());
        for (int i = 0; i < bModules.size(); i++) {
            if (bModules.get(i).getMACAddress().equals(bModuleScelto.getMACAddress())){
                password1=bModules.get(i).getPW1();
                password2=bModules.get(i).getPW2();

                passwordEsistente = !password1.isEmpty() || !password2.isEmpty();
            }
        }
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
        bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
        if(service != null)
            service.attach(this);
        else
            startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change

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
                    /*if (v.equals("N")){
                    scanDevices();
                        scanDevices();
                        SharedPreferences sharedPreferences2 = getSharedPreferences("BTSP", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences2.edit();
                        editor.putString("stopscan", "S");
                        editor.apply();
                    }*/
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
               /* scanDevices();
                SharedPreferences sharedPreferences2 = getSharedPreferences("BTSP", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences2.edit();
                editor.putString("stopscan", "S");
                editor.apply();*/
            }
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
                    if (device.getName() != null) {
                        Log.d("SCANNING", device.getName());
                    }

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