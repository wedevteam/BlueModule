package com.wedevteam.bluemodule;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.InputType;
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
import java.util.List;
import java.util.UUID;

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
    ArrayList<BModule>bModules = new ArrayList<>();
    private String statoRele="";

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
    String statoDevice = "N";

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
    private Button esci;

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

    boolean passwordEsistente = false;
    String password1="";
    String password2="";

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
               // Toast.makeText(this, deviceChoosen.getName(), Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            startActivity(new Intent(FunzioniActivity.this,ElencoActivity.class));
            finish();
        }
        setUI();
        setEmptyLayout();
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
      //  if(service != null && !isChangingConfigurations()) service.detach();
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
                        prepareSend(comandibase[2]);
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
                        prepareSend(comandibase[3]);
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
        if (comando.equals(commandGetRele)){
            Log.i("SEQUENZA COMANDI BASE", "4");
            textView=texttempo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=3){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    msg="Stato Relè: "+risposta[2].toUpperCase();
                    statoRele=risposta[2].toUpperCase();
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }
            if (isComandiBase){
                indexcomandibase=0;
                isComandiBase=false;
                if (statoDevice.equals("N"))
                    setEnabledLayout();
                else
                    setEnabledPasswordLayout();
            }
        }
        if (comando.equals(commandTempo)){
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Cambio tempo");
                    alertDialogBuilder
                            .setMessage("Il tempo di accensione è stato cambiato")
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
        }if (comando.equals(commandPwP)){
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Invio password");
                    alertDialogBuilder
                            .setMessage("La password è stata inviata")
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
        }if (comando.equals(commandPwR)){
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Invio password");
                    alertDialogBuilder
                            .setMessage("La password è stata inviata")
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
        }if (comando.equals(commandSetModoD)){
            textView=textmodo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    msg="Modo: NORMALE";
                    statoDevice="N";
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Cambio modo");
                    alertDialogBuilder
                            .setMessage("Il modo è stato corretamente cambiato in NORMALE")
                            .setCancelable(false)
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    hideSystemUI();
                    setEnabledLayout();
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }

        }if (comando.equals(commandSetModoP)){
            textView=textmodo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    msg="Modo: PASSWORD";
                    statoDevice="P";

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Cambio modo");
                    alertDialogBuilder
                            .setMessage("Il modo è stato corretamente cambiato in PASSWORD")
                            .setCancelable(false)
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    hideSystemUI();
                    setEmptyLayout();
                    setEnabledPasswordLayout();
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }
        }if (comando.equals(commandSetReleOn)){
            textView=texttempo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    statoRele="ON";
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Stato");
                    alertDialogBuilder
                            .setMessage("Stato: ON")
                            .setCancelable(false)
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    hideSystemUI();
                    msg = "Stato: ON";
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }

        }if (comando.equals(commandSetReleOff)){
            textView=texttempo;
            String[] risposta = msg.split(" ");
            if (risposta.length>=2){
                int codiceErr = Integer.parseInt(risposta[1]) ;
                if (codiceErr==0){
                    statoRele="OFF";
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                    alertDialogBuilder.setTitle("Stato");
                    alertDialogBuilder
                            .setMessage("Stato: OFF")
                            .setCancelable(false)
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                    hideSystemUI();
                    msg = "Stato: OFF";
                }else{
                    Toast.makeText(this, errs[codiceErr], Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "RISPOSTA ERRATA DAL DEVICE", Toast.LENGTH_SHORT).show();
            }

        }
        if (!comando.equals(commandAlexa)&& !comando.equals(commandNome)&& !comando.equals(commandPwP)&& !comando.equals(commandPwR)&& !comando.equals(commandTempo)){
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


    public void restart(){
        Intent mStartActivity = new Intent(this, ElencoSchedulatoActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(1);
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
        btnrele = findViewById(R.id.btnrele);
        btntime = findViewById(R.id.btntime);
        btnalexa = findViewById(R.id.btnalexa);
        btnnome = findViewById(R.id.btnnome);
        btnmem1 = findViewById(R.id.btnmem1);
        btninvia1 = findViewById(R.id.btninvia1);
        btnmem2 = findViewById(R.id.btnmem2);
        btninvia2 = findViewById(R.id.btninvia2);
        textpw2 = findViewById(R.id.textpw2);
        textpw1 = findViewById(R.id.textpw1);
        esci = findViewById(R.id.esci);

        esci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                service.detach();
               if (connected != Connected.False) disconnect();
                service.cancelNotification();
                stopService(new Intent(FunzioniActivity.this, SerialService.class));

                startActivity(new Intent(FunzioniActivity.this,ElencoSchedulatoActivity.class));
                finishAffinity();
            }
        });

        btnmodo.setOnClickListener(view -> {
            String modo = statoDevice.equals("N") ? "PASSWORD" : "NORMALE";
            setPasswordEsistente();

            if (passwordEsistente){
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("CAMBIO MODO");
                String finalPassword = password1;
                alertDialogBuilder
                        .setIcon(R.drawable.immaginebase)
                        .setMessage("Vuoi passare alla modalità "+modo+"?")
                        .setCancelable(false)
                        .setPositiveButton("SI", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                if (statoDevice.equals("N")) {
                                    comandoCompleeto=commandSetModoP + " "+ finalPassword;
                                    comando = commandSetModoP;
                                    send(commandSetModoP + " "+ finalPassword);
                                } else {
                                    comandoCompleeto=commandSetModoD + " "+ finalPassword;
                                    comando = commandSetModoD;
                                    send(commandSetModoD+ " "+ finalPassword);
                                }
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }else{
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("CAMBIO MODO");
                alertDialogBuilder
                        .setIcon(R.drawable.immaginebase)
                        .setMessage("Per cambiare modo prima devi impostare la password")
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

        });
        btnrele.setOnClickListener(view -> {

            String testo = "";
            testo = statoRele.equals("ON") ? "OFF" : "ON";
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
           /* AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("CAMBIO STATO");
            alertDialogBuilder
                    .setIcon(R.drawable.immaginebase)
                    .setMessage("Vuoi cambiare lo stato in "+testo+"?")
                    .setCancelable(false)
                    .setPositiveButton("SI", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            if (statoDevice.equals("P")){
                                setPasswordEsistente();
                                comandoCompleeto = comando + " "+ password1;
                            }else{
                                comandoCompleeto = comando;
                            }
                           send(comandoCompleeto);
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();*/
        });
        btntime.setOnClickListener(view -> {
            comando=commandTempo;
            prepareSend(comando);
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
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final View customLayout = getLayoutInflater().inflate(R.layout.dialogdati,null);
            final TextView titolo = customLayout.findViewById(R.id.titolo);
            final EditText testo = customLayout.findViewById(R.id.testo);
            testo.setInputType(InputType.TYPE_CLASS_NUMBER);
            Button annulla = customLayout.findViewById(R.id.annulla);
            Button conferma = customLayout.findViewById(R.id.conferma);
            titolo.setText(R.string.memorizzap1);
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
                    String valore = testo.getText().toString().replace(".","");
                    valore = valore.replace(",","");
                    if (isPasswordVerified(valore)){
                        boolean recordExist = isRecordExist();
                        if (recordExist){
                            //TODO aggiorna
                            for (int i = 0; i <bModules.size(); i++) {
                                if (bModules.get(i).getMACAddress().equals(deviceChoosen.getAddress())){
                                    bModules.get(i).setPW1(valore);
                                }
                            }
                            db.bModuleDao().deleteAll();
                            for (int i = 0; i < bModules.size(); i++) {
                                db.bModuleDao().insert(bModules.get(i));
                            }
                            dialogo.dismiss();
                        }else{
                            // TODO inserisci
                            BModule bModule = new BModule();
                            bModule.setPW1(valore);
                            bModule.setAlias("");
                            bModule.setDataAttivazione("");
                            bModule.setMACAddress(deviceChoosen.getAddress());
                            bModule.setNome(nomedevice);
                            bModule.setMSG("");
                            bModule.setNuovoNome("");
                            bModule.setStato(String.valueOf(deviceChoosen.getBondState()));
                            bModule.setPW2("");
                            bModule.setTipo(String.valueOf(deviceChoosen.getType()));
                            db.bModuleDao().insert(bModule);
                            dialogo.dismiss();
                        }
                        showPasswordMemorizzata();
                    }else{
                        Toast.makeText(FunzioniActivity.this, "La password deve essere compresa tra 0 e 2147483647", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            dialogo.show();
        });
        btninvia1.setOnClickListener(view -> {
            setPasswordEsistente();

            if (!password1.isEmpty()){
                comandoCompleeto = commandPwP+" "+password1;
                comando=commandPwP;
                send(commandPwP+" "+password1);
            }else{
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("INVIA PASSWORD");
                alertDialogBuilder
                        .setIcon(R.drawable.immaginebase)
                        .setMessage("Non hai memorizzato la password")
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });
        btnmem2.setOnClickListener(view -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final View customLayout = getLayoutInflater().inflate(R.layout.dialogdati,null);
            final TextView titolo = customLayout.findViewById(R.id.titolo);
            final EditText testo = customLayout.findViewById(R.id.testo);
            testo.setInputType(InputType.TYPE_CLASS_NUMBER);
            Button annulla = customLayout.findViewById(R.id.annulla);
            Button conferma = customLayout.findViewById(R.id.conferma);
            titolo.setText(R.string.memorizzap2);
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
                    String valore = testo.getText().toString().replace(".","");
                    valore = valore.replace(",","");
                    if (isPasswordVerified(valore)){
                        boolean recordExist = isRecordExist();
                        if (recordExist){
                            //TODO aggiorna
                            for (int i = 0; i <bModules.size(); i++) {
                                if (bModules.get(i).getMACAddress().equals(deviceChoosen.getAddress())){
                                    bModules.get(i).setPW2(valore);
                                }
                            }
                            db.bModuleDao().deleteAll();
                            for (int i = 0; i < bModules.size(); i++) {
                                db.bModuleDao().insert(bModules.get(i));
                            }
                            dialogo.dismiss();
                        }else{
                            // TODO inserisci
                            BModule bModule = new BModule();
                            bModule.setPW2(valore);
                            bModule.setAlias("");
                            bModule.setDataAttivazione("");
                            bModule.setMACAddress(deviceChoosen.getAddress());
                            bModule.setNome(nomedevice);
                            bModule.setMSG("");
                            bModule.setNuovoNome("");
                            bModule.setStato(String.valueOf(deviceChoosen.getBondState()));
                            bModule.setPW1("");
                            bModule.setTipo(String.valueOf(deviceChoosen.getType()));
                            db.bModuleDao().insert(bModule);
                            dialogo.dismiss();
                        }
                        showPasswordMemorizzata();
                    }else{
                        Toast.makeText(FunzioniActivity.this, "La password deve essere compresa tra 0 e 2147483647", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            dialogo.show();
        });
        btninvia2.setOnClickListener(view -> {
            setPasswordEsistente();

            if (!password2.isEmpty()){
                comandoCompleeto = commandPwR+" "+password2;
                comando=commandPwR;
                send(commandPwR+" "+password2);
            }else{
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("INVIA PASSWORD");
                alertDialogBuilder
                        .setIcon(R.drawable.immaginebase)
                        .setMessage("Non hai memorizzato la password")
                        .setCancelable(false)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        nomebt.setText(nomedevice.substring(6));
    }
    private void showPasswordMemorizzata() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Memorizzazione password");
        alertDialogBuilder
                .setMessage("La password è stata memorizzata")
                .setCancelable(false)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private boolean isRecordExist() {
        bModules = new ArrayList<>(getMainData());
        for (int i = 0; i < bModules.size(); i++) {
            if (bModules.get(i).getMACAddress().equals(deviceChoosen.getAddress())){
                return true;
            }
        }
        return false;
    }
    private boolean isPasswordVerified(String testo  ) {
        return (testo.length()>0 && testo.length()<=10 && Integer.parseInt(testo)<=2147483647);
    }
    private void setPasswordEsistente() {
        bModules = new ArrayList<>(getMainData());
        for (int i = 0; i < bModules.size(); i++) {
            if (bModules.get(i).getMACAddress().equals(deviceChoosen.getAddress())){
                password1=bModules.get(i).getPW1();
                password2=bModules.get(i).getPW2();

                passwordEsistente = !password1.isEmpty() || !password2.isEmpty();
            }
        }
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
                case commandTempo:
                    final AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                    final View customLayout2 = getLayoutInflater().inflate(R.layout.dialogdati,null);
                    final TextView titolo2 = customLayout2.findViewById(R.id.titolo);
                    final EditText testo2 = customLayout2.findViewById(R.id.testo);
                    testo2.setInputType(InputType.TYPE_CLASS_NUMBER);
                    Button annulla2 = customLayout2.findViewById(R.id.annulla);
                    Button conferma2 = customLayout2.findViewById(R.id.conferma);
                    titolo2.setText(R.string.nuovotempo);
                    builder2.setView(customLayout2);
                    final AlertDialog dialogo2 = builder2.create();
                    annulla2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialogo2.dismiss();
                        }
                    });
                    conferma2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String valore = testo2.getText().toString().replace(".","");
                            valore = valore.replace(",","");
                            if (isTempoVerified(valore)){
                                dialogo2.dismiss();
                                comandoCompleeto = comando + " " + valore;
                                send(comandoCompleeto);
                            }else{
                                Toast.makeText(FunzioniActivity.this, "Il valore deve essere compreso tra 0 e 100000", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    dialogo2.show();
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
    private boolean isTempoVerified(String testo) {
        return (testo.length()>=0 && testo.length()<=6 && Integer.parseInt(testo)<=100000);
    }
    private boolean isNomeVerified(String testo) {
        return (testo.length()>3 && testo.length()<=25);
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
        btnnome . setEnabled(false);
        btnrele . setEnabled(false);
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
         btnnome . setBackgroundColor(Color.LTGRAY);
         btnrele . setBackgroundColor(Color.LTGRAY);
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
        btnnome . setEnabled(true);
        btnrele . setEnabled(true);
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
        btnnome . setBackgroundColor(Color.parseColor("#3999c4"));
        btnrele . setBackgroundColor(Color.parseColor("#3999c4"));
        btntime . setBackgroundColor(Color.parseColor("#3999c4"));
        btnalexa . setBackgroundColor(Color.parseColor("#3999c4"));
        btnmem1 . setBackgroundColor(Color.parseColor("#3999c4"));
        btninvia1 . setBackgroundColor(Color.parseColor("#3999c4"));
        btnmem2 . setBackgroundColor(Color.parseColor("#3999c4"));
        btninvia2 . setBackgroundColor(Color.parseColor("#3999c4"));
    }
    private void setEmptyLayout() {
        textmodo . setVisibility(View.GONE);
        textvhw . setVisibility(View.GONE);
        textvsw . setVisibility(View.GONE);
        texttempo . setVisibility(View.GONE);
        textnome . setVisibility(View.GONE);
        btnmodo . setVisibility(View.GONE);
        btnrele . setVisibility(View.GONE);
        btntime . setVisibility(View.GONE);
        btnalexa . setVisibility(View.GONE);
        btnnome . setVisibility(View.GONE);
        btnmem1 . setVisibility(View.GONE);
        btninvia1 . setVisibility(View.GONE);
        btnmem2 . setVisibility(View.GONE);
        btninvia2 . setVisibility(View.GONE);
        textpw2 . setVisibility(View.GONE);
        textpw1 . setVisibility(View.GONE);
    }
    private void setFullLayout() {
        textmodo . setVisibility(View.VISIBLE);
        textvhw . setVisibility(View.VISIBLE);
        textvsw . setVisibility(View.VISIBLE);
        texttempo . setVisibility(View.VISIBLE);
        btnmodo . setVisibility(View.VISIBLE);
        btnrele . setVisibility(View.VISIBLE);
        btnnome . setVisibility(View.VISIBLE);
        btntime . setVisibility(View.VISIBLE);
        btnalexa . setVisibility(View.VISIBLE);
        btnmem1 . setVisibility(View.VISIBLE);
        btninvia1 . setVisibility(View.VISIBLE);
        btnmem2 . setVisibility(View.VISIBLE);
        btninvia2 . setVisibility(View.VISIBLE);
        textpw2 . setVisibility(View.VISIBLE);
        textpw1 . setVisibility(View.VISIBLE);
    }
    private void setEnabledPasswordLayout() {
        textmodo . setVisibility(View.VISIBLE);
        textvhw . setVisibility(View.VISIBLE);
        textvsw . setVisibility(View.VISIBLE);
        texttempo . setVisibility(View.VISIBLE);
        btnmodo . setVisibility(View.VISIBLE);
        btnmem1 . setVisibility(View.VISIBLE);
        btninvia1 . setVisibility(View.VISIBLE);
        btnmem2 . setVisibility(View.VISIBLE);
        btninvia2 . setVisibility(View.VISIBLE);
        btnrele . setVisibility(View.VISIBLE);
        textpw2 . setVisibility(View.VISIBLE);
        textpw1 . setVisibility(View.VISIBLE);
        btnmem1 . setEnabled(true);
        btninvia1 . setEnabled(true);
        btnmem2 . setEnabled(true);
        btninvia2 . setEnabled(true);
        btnmodo . setEnabled(true);
        btnrele.setEnabled(true);
    }
}