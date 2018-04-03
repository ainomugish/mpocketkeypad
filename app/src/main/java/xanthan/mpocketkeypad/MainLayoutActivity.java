package xanthan.mpocketkeypad;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.usage.UsageEvents;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;

import Mpock.Globals;
import Mpock.Vars;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import entities.Student;
import eventBusClasses.MqConnected;
import eventBusClasses.MqDisonnected;
import eventBusClasses.StudentChargeFinished;
import eventBusClasses.StudentChargeReply;
import models.Minput;
import models.StudentCharge;
import mqes.MpockMqService;
import sugarorms.studentorm;

public class MainLayoutActivity extends AppCompatActivity implements
        Handler.Callback {

    Vars vars;

    StudentCharge studentCharge;

    TextView student_name;
    TextView connected;

    //DEVICE STUFF
    TextView device_name;
    TextView partner_device_name;

    //LINEAR LAYOUTS
    LinearLayout enter_amount_layout;
    LinearLayout awaiting_trans_amount_layout;

    EditText student_charge_amount;
    Button connectButton;

    ProgressDialog progressBar;

    //APPLICATION CLASS
    GlobalState appVar;

    private static final int RESULT_CONN_CODE = 0;
    private boolean mDeviceConnected = false;
    private Handler handler = new Handler(this);
    private ChatManager chatManager;

    public Handler getHandler() {
        return handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        //VARS
        vars = new Vars(this);

        //INIT APP CLASS
        appVar = (GlobalState) this.getApplicationContext();

        //SET UI STUFF
        student_name = (TextView) findViewById(R.id.student_name);
        connected = (TextView) findViewById(R.id.connected);
        device_name = (TextView) findViewById(R.id.device_name);
        partner_device_name = (TextView) findViewById(R.id.partner_device_name);

        //LINEAR LAYOUTS
        enter_amount_layout = (LinearLayout) findViewById(R.id.enter_amount_layout);
        awaiting_trans_amount_layout = (LinearLayout) findViewById(R.id.awaiting_trans_amount_layout);


        student_charge_amount = (EditText) findViewById(R.id.student_charge_amount);
        connectButton = (Button) findViewById(R.id.connectButton);

        //CHECK IF DEVICE NAME IS SET
//            if (vars.prefs.getString(Globals.DEVICE_NAME, null) == null) {
        if (false) {
            log("DEVICE NAME NOT SET");

            vars.alerter.alerterAnySuccessActivity("Error", "Device name not set!", SetDeviceName.class);
//            } else if (vars.prefs.getString(Globals.PARTNER_DEVICE_NAME, null) == null) {
        } else if(false) {
            log("PARTNER DEVICE NAME NOT SET");

            vars.alerter.alerterAnySuccessActivity("Error", "PARTNER Device name not set!", SetPartnerDeviceName.class);
        } else {

            //BIND DEVICE NAMES
            device_name.setText("DEVICE NAME:" + vars.prefs.getString(Globals.DEVICE_NAME, null));
            partner_device_name.setText("PARTNER DEVICE NAME:" + vars.prefs.getString(Globals.PARTNER_DEVICE_NAME, null));

//                We are going to replace Mqtt connections with wifi direct connections
//
//                START MQ
//                startService(new Intent(this, MpockMqService.class));
//                showProgress();
        }

        if(!mDeviceConnected) {
            Intent i = DevicesActivity.newIntent(MainLayoutActivity.this, mDeviceConnected);
            startActivityForResult(i, RESULT_CONN_CODE);
        } else {
            connected.setText(Globals.CONNECTED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(DevicesActivity.TAG, "Returned from Device Activity");
        Log.d(DevicesActivity.TAG, "resultCode: " + resultCode);
        Log.d(DevicesActivity.TAG, "RESULT_OK: " + FragmentActivity.RESULT_OK);
        if (data == null) {
            Log.d(DevicesActivity.TAG, "RESULT_CONN_CODE: " + requestCode);
        }
        Log.d(DevicesActivity.TAG, "data: " + data);
        if (resultCode != FragmentActivity.RESULT_OK) {
            return;
        }
        if (requestCode == RESULT_CONN_CODE) {
            if (data == null) {
                return;
            }
            if (mDeviceConnected = DevicesActivity.wasDeviceConnected(data)) {
                Toast.makeText(this, "Devices has been successfully connected!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void showProgress(){
        //SHOW CONNECTING PROGRESS DIALOG
        progressBar = new ProgressDialog(this);
        progressBar.setMessage("Connecting...");
        progressBar.setCancelable(false);
        progressBar.show();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MainThread)
    public void onEvent(ConnCreated evt) {
        Thread handler = null;
        WifiP2pInfo info = evt.getDeviceInfo();
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */
        Log.d(DevicesActivity.TAG, "We have received a connection created event!");
        if (info.isGroupOwner) {
            Log.d(DevicesActivity.TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(
                        (this).getHandler());
                handler.start();
            } catch (IOException e) {
                Log.d(DevicesActivity.TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(DevicesActivity.TAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    (this).getHandler(),
                    info.groupOwnerAddress);
            handler.start();
        }
        connected.setText(Globals.CONNECTED);
        if (progressBar != null) {
            progressBar.dismiss();
        }

        Log.d(DevicesActivity.TAG, "Socket Streams have been established");
////        runOnUiThread(new Runnable() {
////            @Override
////            public void run() {
////                connected.setText(Globals.CONNECTED);
////                if (progressBar != null) {
////                    progressBar.dismiss();
////                }
////            }
////        });
    }



    //FORCE START SERVICE
    public void connect(View view){
        log("+++++++++: reconnect");
        //START MQ
        startService(new Intent(this, MpockMqService.class));
    }

    //RECEIVE NEW STUDENT
    @Subscribe//(sticky = true, threadMode = ThreadMode.MainThread)
    public void onEvent(final StudentCharge studentCharge){
        log("+++++++++++++: StudentCharge post received");

        log("studnet:" + studentCharge.lastname);
        this.studentCharge = studentCharge;

        //UPDATE UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //remove awaint layout
                awaiting_trans_amount_layout.setVisibility(View.GONE);

                //SHOW TRANS LAYOUT
                enter_amount_layout.setVisibility(View.VISIBLE);

                //BLANK AMOUNT
                student_charge_amount.setText("");

                //SET NAME
                student_name.setText(studentCharge.firstnmae + " " + studentCharge.lastname + ".." + studentCharge.transId);
            }
        });
    }

    //RECIVES REPLY OF TRANSACTION RESULT
    @Subscribe//(sticky = true, threadMode = ThreadMode.MainThread)
    public void onEvent(final StudentChargeFinished studentChargeFinished){
        log("+++++++++++++: studentChargeFinished post received");

        log("studentChargeFinished:" + studentChargeFinished.studentCharge.transResult);
        //this.studentCharge = studentCharge;

        //UPDATE UI
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //CHECK IF THE STUDENT CHARGE IS  NOT NULL
                if (studentCharge != null) {
                    //IF IT WAS SUCCESSFUL TRANSACTION

                    //CHECK IF ITS THE CURREN TRANSACTION
                    if (studentCharge.transId.equalsIgnoreCase(studentChargeFinished.studentCharge.transId)) {
                        if (studentChargeFinished.studentCharge.transResult) {
                            log("result was true;");
                            vars.alerter.alerterAny("Success", "Transaction Succcessfull");
                        } else if (!studentChargeFinished.studentCharge.transResult) {
                            log("result was false;");
                            vars.alerter.alerterAnyError("ERROR", "Transaction FAILED");
                        }

                        //SHOW AWAITING TRANS LAYOUT
                        awaiting_trans_amount_layout.setVisibility(View.VISIBLE);

                        //HIDE TRANS LAYOUT
                        enter_amount_layout.setVisibility(View.GONE);

                        //SET NAME
                        student_name.setText("");

                        //BLANK AMOUNT
                        student_charge_amount.setText("");

                        //reset studentcharge
                        studentCharge = null;

                    } else {
                        vars.alerter.alerterAnyError("Error", "Transactions do not match, restart");
                    }


                } else if (studentCharge == null) {
                    vars.alerter.alerterAny("Error", "Invalid Transaction Recieved, pls Restart Trans");
                }


            }
        });
    }

//    //CONNECTED EVENT BUS
//    @Subscribe
//    public void onEvent(MqConnected conn){
//        log("++++++++++++: MqConnected post received");
//
//        //UPDATE UI
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                //CHANGE TEXT
//                connected.setText("CONNECTED");
//
//                //REMOVE BUTTON
//                connectButton.setVisibility(View.INVISIBLE);
//
//                //dismiss connecting
//                progressBar.dismiss();
//
//            }
//        });
//
//
//    }

//    //DISCONNECTED EVENTBUS
//    @Subscribe
//    public void onEvent(MqDisonnected disConn){
//        log("++++++++++++: MqConnected post received");
//
//        //UPDATE UI
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                connected.setText("DISCONNECTED!!");
//
//                //SHOW CONNECT BUTTON
//                connectButton.setVisibility(View.VISIBLE);
//
//                //SHOW PROGRESS DIALOG
//                showProgress();
//            }
//        });
//
//
//    }

    public void sendAmount(View view){
        log("++++++++++++;sendAmount");

        String amount = student_charge_amount.getText().toString();

        if(amount.isEmpty() || amount.length() < 2){
            vars.alerter.alerterAny("Error", "Invalid Amount");
        }else{
            this.studentCharge.amount = Integer.valueOf(amount);
            log("amount set to:"+amount);

            //PUBLISH student charge
            StudentChargeReply scr = new StudentChargeReply(this.studentCharge);
//            EventBus.getDefault().post(scr); // No need, we can just send through our
//            Connection Manager
            if(chatManager != null) {
                chatManager.write((scr.toString() + "---" + scr.getClass().getSimpleName()).getBytes());
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        log("Main activity on Resume");

        EventBus.getDefault().register(this);

//        if(appVar.connectionStatus.equalsIgnoreCase(Globals.CONNECTED)){
//            log("singleton connected is CONNECTED SO REMOVING PROGRESS BAR");
//            progressBar.dismiss();
//        }else{
//            log("not connected progress bar remains");
//        }
    }

    //CHANGE DEVICES
 public void changeDeviceName(View v) {
        log("++++++++++++++: changeDeviceName");

        LayoutInflater li = LayoutInflater.from(vars.context);
        View promptsView;
        promptsView = li.inflate(R.layout.canteen_success_simple, null);

        TextView headerTxt = (TextView) promptsView
                .findViewById(R.id.success_simple_header);
        TextView messageTxt = (TextView) promptsView
                .findViewById(R.id.success_simple_message);
        final EditText pinNumber = (EditText) promptsView.findViewById(R.id.canteen_pin);

        headerTxt.setText("Enter Pin");
        messageTxt.setText("Enter Pin");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                vars.context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        String pin = pinNumber.getText().toString();
                        log("edit text value:" + pin);
                        //  if (pin.equalsIgnoreCase("1234")) {
                        if (pin.equalsIgnoreCase(String.valueOf(vars.setPin))) {
                            startActivity(new Intent(vars.context, SetDeviceName.class));
                        } else {
                            //mMaterialDialog.dismiss();
                            // dialog.cancel();
                            dialog.cancel();
                        }

                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();


//        final EditText contentView = new EditText(this);
//        contentView.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
//
//        mMaterialDialog = new MaterialDialog(context)
//                .setTitle("Canteen Pin")
//                .setView(contentView)
//                .setMessage("Please enter your Pin:")
//                .setPositiveButton("OK", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        String pin = contentView.getText().toString();
//                        log("edit text value:" + pin);
//                        //  if (pin.equalsIgnoreCase("1234")) {
//                        if (pin.equalsIgnoreCase(String.valueOf(vars.setPin))) {
//                            startActivity(new Intent(vars.context, CanteenViewTrans.class));
//                        } else {
//                            mMaterialDialog.dismiss();
//                        }
//                    }
//                })
//                .setNegativeButton("CANCEL", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mMaterialDialog.dismiss();
//                    }
//                });
//        mMaterialDialog.show();

    }


    public void changePartnerDeviceName(View v) {
        log("++++++++++++++: changePartnerDeviceName");

        LayoutInflater li = LayoutInflater.from(vars.context);
        View promptsView;
        promptsView = li.inflate(R.layout.canteen_success_simple, null);

        TextView headerTxt = (TextView) promptsView
                .findViewById(R.id.success_simple_header);
        TextView messageTxt = (TextView) promptsView
                .findViewById(R.id.success_simple_message);
        final EditText pinNumber = (EditText) promptsView.findViewById(R.id.canteen_pin);

        headerTxt.setText("Enter Pin");
        messageTxt.setText("Enter Pin");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                vars.context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);
        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        String pin = pinNumber.getText().toString();
                        log("edit text value:" + pin);
                        //  if (pin.equalsIgnoreCase("1234")) {
                        if (pin.equalsIgnoreCase(String.valueOf(vars.setPin))) {
                            startActivity(new Intent(vars.context, SetPartnerDeviceName.class));
                        } else {
                            //mMaterialDialog.dismiss();
                            // dialog.cancel();
                            dialog.cancel();
                        }

                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();


//        final EditText contentView = new EditText(this);
//        contentView.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
//
//        mMaterialDialog = new MaterialDialog(context)
//                .setTitle("Canteen Pin")
//                .setView(contentView)
//                .setMessage("Please enter your Pin:")
//                .setPositiveButton("OK", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        String pin = contentView.getText().toString();
//                        log("edit text value:" + pin);
//                        //  if (pin.equalsIgnoreCase("1234")) {
//                        if (pin.equalsIgnoreCase(String.valueOf(vars.setPin))) {
//                            startActivity(new Intent(vars.context, CanteenViewTrans.class));
//                        } else {
//                            mMaterialDialog.dismiss();
//                        }
//                    }
//                })
//                .setNegativeButton("CANCEL", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mMaterialDialog.dismiss();
//                    }
//                });
//        mMaterialDialog.show();

    }

    @Override
    protected void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void log(String msg) {
        //if (log && Globals.log) {
        Log.v(this.getClass().getSimpleName(), msg);
        //}
    }

    public void setChatManager(ChatManager connManager) {
        this.chatManager = connManager;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case DevicesActivity.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(DevicesActivity.TAG, readMessage);
                String [] parts = readMessage.split("---");
                // receive json object
                Gson fromGson = new Gson();
                if(parts[1].equals("StudentCharge")) {
                    EventBus.getDefault()
                            .post(fromGson.fromJson(parts[0], StudentCharge.class));
                } else if(parts[1].equals("StudentChargeFinished")) {
                    EventBus.getDefault()
                            .post(fromGson.fromJson(parts[0], StudentChargeFinished.class));
                } else {
                    Log.d(DevicesActivity.TAG, "Received unidentified event message");
                }
                Log.d(DevicesActivity.TAG, "Posted SCR Event on the Bus");
                break;

            case DevicesActivity.MY_HANDLE:
                Object obj = msg.obj;
                this.setChatManager((ChatManager) obj);

        }
        return true;
    }



}
