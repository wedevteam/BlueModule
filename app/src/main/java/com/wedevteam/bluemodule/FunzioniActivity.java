package com.wedevteam.bluemodule;

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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wedevteam.bluemodule.Database._Database;
import com.wedevteam.bluemodule.Database.tables.BModule;
import com.wedevteam.bluemodule.Funzioni.Funzioni;
import com.wedevteam.bluemodule.Funzioni.TextUtil;
import com.wedevteam.bluemodule.Servizi.SerialListener;
import com.wedevteam.bluemodule.Servizi.SerialService;
import com.wedevteam.bluemodule.Servizi.SerialSocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;

public class FunzioniActivity extends AppCompatActivity implements ServiceConnection, SerialListener {

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
    private BluetoothDevice deviceChoosen;
    BluetoothGattCharacteristic gattCharacteristicgen;
    private  BluetoothGattCharacteristic  TX;
    private final String prename = "YYmJ4z";
    private boolean isFromCambioNome=false;

    // NEW SYSTEM
    private enum Connected { False, Pending, True }
    private Connected connected = Connected.False;
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

    // UI
    private TextView nomebt;
    private TextView statobt;
    private TextView textmodo;
    private TextView textvhw;
    private TextView textvsw;
    private TextView texttempo;
    private TextView textnome;
    private Button btnmodo;
    private Button btnversion;
    private Button btntime;
    private Button btnalexa;
    private Button btnnome;
    private Button btnmem1;
    private Button btninvia1;
    private Button btnmem2;
    private Button btninvia2;

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
    private String[] comandibase = new String[]{commandGetModo,commandVSW, commandVHW, commandGetRele};
    private int indexcomandibase=0;
    private boolean isComandiBase = false;

    private final String rispostaGetModoDef = "def";
    private final String rispostaGetModoPwd = "pwd";

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
                deviceChoosen =intent.getExtras().getParcelable("device");
                nomedevice = intent.getExtras().getString("nomedevice");
                Toast.makeText(this, deviceChoosen.getName(), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            startActivity(new Intent(FunzioniActivity.this,ElencoActivity.class));
            finish();
        }
        setUI();
        setDB();
        bindService(new Intent(this, SerialService.class), this, Context.BIND_AUTO_CREATE);
        if(service != null)
            service.attach(this);
        else
            startService(new Intent(this, SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
        connectBT();
    }
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if(initialStart && service != null) {
            initialStart = false;
            runOnUiThread(this::connectBT);
        }
       // registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    @Override
    public void onStart() {
        super.onStart();

    }
    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(gattUpdateReceiver);
    }
    @Override
    protected void onStop() {
        if(service != null && !isChangingConfigurations()) service.detach();
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if (connected != Connected.False) disconnect();
        stopService(new Intent(this, SerialService.class));
        super.onDestroy();

    }
    // ==============================================
    // BT
    // ==============================================
    private void connectBT(){
        // NEW SYSTEM
        // =================================================================
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice deviceFromSocket = bluetoothAdapter.getRemoteDevice(deviceChoosen.getAddress());
            status("in connessione...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getApplicationContext(), deviceFromSocket);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
        // =================================================================

    }
    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }
    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str);
        if (str.equals("CONNESSO")){
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        statobt.setText(spn);}
        else{
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.gray)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            statobt.setText(spn);
        }
    }
    private void send(String str) {
        if(connected != Connected.True) {
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

        if (comando.equals(commandVSW)){
            Log.i("SEQUENZA COMANDI BASE", "2");
            textView=textvsw;
            String[] risposta = msg.split(" ");
            if (risposta.length>=3){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    msg="Vers. software: "+risposta[2];
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
                        prepareSend(comandibase[indexcomandibase]);
                    }
                }
            }.start();
        }
        if (comando.equals(commandVHW)){
            Log.i("SEQUENZA COMANDI BASE", "3");
            textView=textvhw;
            String[] risposta = msg.split(" ");
            if (risposta.length>=3){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    msg="Vers. hardware: "+risposta[2];
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
                        prepareSend(comandibase[indexcomandibase]);
                    }
                }
            }.start();


        }

        if (comando.equals(commandAlexa)){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Dissociazione da Alexa");
            alertDialogBuilder
                    .setMessage("Alexa è stata dissociata dal device. Il dispositivo viene riavviato.")
                    .setCancelable(false)
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }
        if (comando.equals(commandNome)){
            textView=nomebt;
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Cambio nome");
                    alertDialogBuilder
                            .setMessage("Il nome è stato corretamente cambiato")
                            .setCancelable(false)
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                   AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    hideSystemUI();
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }
        }
        if (comando.startsWith(commandGetModo)){

        }
        if (comando.equals(commandGetModo)){
            Log.i("SEQUENZA COMANDI BASE", "1");
            textView=textmodo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=3){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    if (risposta[2].equals(rispostaGetModoDef)){
                        setLayoutDef();
                        msg="Modo: NORMALE";
                    }else{
                        setLayoutPwd();
                        msg="Modo: PASSWORD";
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
                        prepareSend(comandibase[indexcomandibase]);
                    }
                }
            }.start();

        }
        if (comando.equals(commandGetRele)){
            Log.i("SEQUENZA COMANDI BASE", "4");
            textView=texttempo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=3){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    msg="Stato Relè: "+risposta[2].toUpperCase();
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }
            if (isComandiBase){
                indexcomandibase=0;
                isComandiBase=false;
            }
        }
        if (comando.startsWith(commandTempo)){

        }if (comando.startsWith(commandPwP)){

        }if (comando.startsWith(commandPwR)){

        }if (comando.startsWith(commandSetModoD)){

        }if (comando.startsWith(commandSetModoP)){

        }if (comando.startsWith(commandSetReleOn)){

        }if (comando.startsWith(commandSetReleOff)){

        }
        if (!comando.equals(commandAlexa)&& !comando.equals(commandNome)){
            textView.setVisibility(View.VISIBLE);
            textView.setText(msg);
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
    private void setLayoutPwd() {
    }
    private void setLayoutDef() {
    }



    // ==============================================
    // UI
    // ==============================================
    private void setUI() {
        nomebt = findViewById(R.id.nomebt);
        textmodo = findViewById(R.id.textmodo);
        textvhw = findViewById(R.id.textvhw);
        textvsw = findViewById(R.id.textvsw);
        texttempo = findViewById(R.id.texttempo);
        textnome = findViewById(R.id.textnome);
        statobt = findViewById(R.id.statobt);
        btnmodo = findViewById(R.id.btnmodo);
        btnversion = findViewById(R.id.btnnome);
        btntime = findViewById(R.id.btntime);
        btnalexa = findViewById(R.id.btnalexa);
        btnnome = findViewById(R.id.btnversion);
        btnmem1 = findViewById(R.id.btnmem1);
        btninvia1 = findViewById(R.id.btninvia1);
        btnmem2 = findViewById(R.id.btnmem2);
        btninvia2 = findViewById(R.id.btninvia2);
        
        btnmodo.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });
        btnversion.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });
        btntime.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });
        btnalexa.setOnClickListener(view -> {
            comando=commandAlexa;
            prepareSend(comando);
        });
        btnnome.setOnClickListener(view -> {
            comando=commandNome;
            prepareSend(comando);
        });
        btnmem1.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });
        btninvia1.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });
        btnmem2.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });
        btninvia2.setOnClickListener(view -> {
            comando="v sw";
            send(comando);
        });

        nomebt.setText(nomedevice.substring(6));
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
                case commandAlexa:
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("SCOLLEGAMENTO DA ALEXA");
                    alertDialogBuilder
                            .setIcon(R.drawable.immaginebase)
                            .setMessage("Vuoi dissociare il dispositivo da Alexa?")
                            .setCancelable(false)
                            .setPositiveButton("SI", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    send(commandAlexa);
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    break;
                case commandNome:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final View customLayout = getLayoutInflater().inflate(R.layout.dialogdati,null);
                    final TextView titolo = customLayout.findViewById(R.id.titolo);
                    final EditText testo = customLayout.findViewById(R.id.testo);
                    Button annulla = customLayout.findViewById(R.id.annulla);
                    Button conferma = customLayout.findViewById(R.id.conferma);
                    titolo.setText(R.string.nuovonome);
                    builder.setView(customLayout);
                    final AlertDialog dialogo = builder.create();
                    annulla.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialogo.dismiss();
                        }
                    });
                    conferma.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isNomeVerified(testo.getText().toString())){
                                dialogo.dismiss();
                                comandoCompleeto = comando + " " + prename+testo.getText().toString();
                                nomebt.setText((prename+testo.getText().toString()).substring(6));
                                nomenuovo = (prename+testo.getText().toString()).substring(6);
                                send(comandoCompleeto);
                            }else{
                                Toast.makeText(FunzioniActivity.this, "Il nome deve avere lunghezza compresa tra 5 e 25 caratteri", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    dialogo.show();
                    break;
                case commandVHW:
                    send(commandVHW);
                    break;
                case commandVSW:
                    send(commandVSW);
                    break;
                default:
            }
        }
    }
    private boolean isNomeVerified(String testo) {
        return (testo.length()>4 && testo.length()<=25);
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

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart) {
            initialStart = false;
            runOnUiThread(this::connectBT);
        }
    }
    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
        if (isFromCambioNome){
            isFromCambioNome=false;
            connectBT();
        }
    }
    @Override
    public void onSerialConnect() {
        status("CONNESSO");
        connected = Connected.True;
        setEnabledLayout();
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
                setDisabledLayout();
                Toast.makeText(FunzioniActivity.this, "Tentativo di riconnessione entro 10 secondi...", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFinish() {
                connectBT();
            }
        }.start();
    }
    private void setDisabledLayout() {
        setFullLayout();
        nomebt.setEnabled(false);
        textmodo . setEnabled(false);
        textvhw . setEnabled(false);
        textvsw . setEnabled(false);
        texttempo . setEnabled(false);

        statobt . setEnabled(false);
        btnmodo . setEnabled(false);
        //btnversion . setEnabled(false);
        btntime . setEnabled(false);
        btnalexa . setEnabled(false);
        btnnome . setEnabled(false);
        btnmem1 . setEnabled(false);
        btninvia1 . setEnabled(false);
        btnmem2 . setEnabled(false);
        btninvia2 . setEnabled(false);

        nomebt.setBackgroundColor(Color.LTGRAY);
        textmodo . setBackgroundColor(Color.LTGRAY);
        textvhw . setBackgroundColor(Color.LTGRAY);
        textvsw . setBackgroundColor(Color.LTGRAY);
        texttempo . setBackgroundColor(Color.LTGRAY);
        textnome . setBackgroundColor(Color.LTGRAY);
        statobt . setBackgroundColor(Color.LTGRAY);
        btnmodo . setBackgroundColor(Color.LTGRAY);
    //   btnversion . setBackgroundColor(Color.LTGRAY);
        btntime . setBackgroundColor(Color.LTGRAY);
        btnalexa . setBackgroundColor(Color.LTGRAY);
        btnnome . setBackgroundColor(Color.LTGRAY);
        btnmem1 . setBackgroundColor(Color.LTGRAY);
        btninvia1 . setBackgroundColor(Color.LTGRAY);
        btnmem2 . setBackgroundColor(Color.LTGRAY);
        btninvia2 . setBackgroundColor(Color.LTGRAY);
    }
    private void setEnabledLayout() {
        setFullLayout();
        nomebt.setEnabled(true);
        textmodo . setEnabled(true);
        textvhw . setEnabled(true);
        textvsw . setEnabled(true);
        texttempo . setEnabled(true);
        textnome . setEnabled(true);
        statobt . setEnabled(true);
        btnmodo . setEnabled(true);
     //   btnversion . setEnabled(true);
        btntime . setEnabled(true);
        btnalexa . setEnabled(true);
        btnnome . setEnabled(true);
        btnmem1 . setEnabled(true);
        btninvia1 . setEnabled(true);
        btnmem2 . setEnabled(true);
        btninvia2 . setEnabled(true);

        nomebt.setBackgroundColor(Color.parseColor("#186f96"));
        textmodo . setBackgroundColor(Color.parseColor("#186f96"));
        textvhw . setBackgroundColor(Color.parseColor("#186f96"));
        textvsw . setBackgroundColor(Color.parseColor("#186f96"));
        texttempo . setBackgroundColor(Color.parseColor("#186f96"));
        textnome . setBackgroundColor(Color.parseColor("#186f96"));
        statobt . setBackgroundColor(Color.parseColor("#186f96"));
        btnmodo . setBackgroundColor(Color.parseColor("#3999c4"));
   //     btnversion . setBackgroundColor(Color.parseColor("#3999c4"));
        btntime . setBackgroundColor(Color.parseColor("#3999c4"));
        btnalexa . setBackgroundColor(Color.parseColor("#3999c4"));
        btnmem1 . setBackgroundColor(Color.parseColor("#3999c4"));
        btninvia1 . setBackgroundColor(Color.parseColor("#3999c4"));
        btnmem2 . setBackgroundColor(Color.parseColor("#3999c4"));
        btninvia2 . setBackgroundColor(Color.parseColor("#3999c4"));
    }
    private void setEmptyLayout() {
        nomebt.setVisibility(View.GONE);
        textmodo . setVisibility(View.GONE);
        textvhw . setVisibility(View.GONE);
        textvsw . setVisibility(View.GONE);
        texttempo . setVisibility(View.GONE);
        textnome . setVisibility(View.GONE);
        statobt . setVisibility(View.GONE);
        btnmodo . setVisibility(View.GONE);
    //    btnversion . setVisibility(View.GONE);
        btntime . setVisibility(View.GONE);
        btnalexa . setVisibility(View.GONE);
        btnnome . setVisibility(View.GONE);
        btnmem1 . setVisibility(View.GONE);
        btninvia1 . setVisibility(View.GONE);
        btnmem2 . setVisibility(View.GONE);
        btninvia2 . setVisibility(View.GONE);
    }
    private void setFullLayout() {
        nomebt.setVisibility(View.VISIBLE);
        textmodo . setVisibility(View.VISIBLE);
        textvhw . setVisibility(View.VISIBLE);
        textvsw . setVisibility(View.VISIBLE);
        texttempo . setVisibility(View.VISIBLE);
        statobt . setVisibility(View.VISIBLE);
        btnmodo . setVisibility(View.VISIBLE);
     //   btnversion . setVisibility(View.VISIBLE);
        btntime . setVisibility(View.VISIBLE);
        btnalexa . setVisibility(View.VISIBLE);
        btnmem1 . setVisibility(View.VISIBLE);
        btninvia1 . setVisibility(View.VISIBLE);
        btnmem2 . setVisibility(View.VISIBLE);
        btninvia2 . setVisibility(View.VISIBLE);
    }
}