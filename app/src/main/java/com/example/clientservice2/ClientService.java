package com.example.clientservice2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientService extends Service {

    private int code = this.hashCode();

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String ADDR = "70.12.60.99";
    private static final int PORT = 55566;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private BroadcastReceiver mBroadcastReceiver ;
    private BroadcastReceiver sBroadcastReceiver ;
    private ExecutorService executor;

    private static boolean keepConn = true;     // automatic
    private boolean connected = false;
//    private boolean connected = socket.isConnected() && ! socket.isClosed();

    // =================================================================
    public ClientService() {
    }

    // =================================================================
    @Override
    public void onCreate() {
        super.onCreate();

        start();    // Start automatic reconnection

        // notify
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        // =================================================================
        // Receiver (from MainActivity)
        mBroadcastReceiver = new BroadcastReceiver() {
            int code = this.hashCode();
            @Override
            public void onReceive(Context context, Intent intent) {
                String data = intent.getStringExtra("DATA");
                Log.i("TEST", "Service - LocalReceiver_" + code + " ] " + data);

//                if(data.equals("WIFI_STATE_CHANGED_ACTION") || data.equals("NETWORK_STATE_CHANGED_ACTION")) {
//                    close();
//                }
                // echo to MainActivity
//                Intent sender = new Intent("activityReceiver");
//                sender.putExtra("DATA", "서비스 켰다");
//                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(sender);
                send(data);
            }
        };

//        sBroadcastReceiver = new BroadcastReceiver() {
////            @Override
////            public void onReceive(Context context, Intent intent) {
////                switch (intent.getAction()) {
//////                    WifiManager
////                    case WifiManager.NETWORK_STATE_CHANGED_ACTION :
////                        Log.i("TEST", "Service - NETWORK_STATE_CHANGED_ACTION");
////                        close();
////                        break;
////                    case WifiManager.WIFI_STATE_CHANGED_ACTION :
////                        Log.i("TEST", "Service - WIFI_STATE_CHANGED_ACTION");
////                        close();
////                        break;
////                    default:
////                        break;
////                }
////            }
////        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, new IntentFilter("serviceReceiver"));

    }

    // =================================================================
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i("TEST", "SETVICE - onStartCommand " + code);

//        String data = intent.getExtras().getString("DATA");
//        send(data);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // =================================================================
    // Start automatic reconnection
    private void start() {
        executor = Executors.newFixedThreadPool(2);

        Runnable accept = new Runnable() {
            @Override
            public void run() {
                do {
                    if(socket == null || close()) {
                        if(connect()) {
                            while(socket.isConnected() && ! socket.isClosed()) {
                                try {
                                    String line = "";
                                    line = input.readLine();
                                    if(line == null) throw new IOException();
                                    else {
                                        Intent sender = new Intent("activityReceiver");
                                        sender.putExtra("DATA", line);
                                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(sender);
                                    }
                                } catch (IOException e) {
//                                  e.printStackTrace();
                                    Log.i("TEST", "Service - Runnable accept() : " + e);
                                    close();
                                }
                            } // while(connected)
                        }
                    }
                } while (keepConn);
                // do-while()
                stop();
            }
        };
        executor.submit(accept);
    } // start()

    // Stop automatic reconnection
    private void stop() {
        keepConn = false;
        close();
        if(executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    } // stop()

    // connection
    private boolean connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ADDR, PORT));
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream());

            connected = true;
//            send("" + this.code);
//            send("USER");

            Log.i("TEST", "Service - connect() : success");
        } catch (IOException e) {
//            e.printStackTrace();
            Log.i("TEST", "Service - connect() : " + e);
            return false;
        }
        return true;
    } // connect()

    // connection close
    private boolean close() {
        Log.i("TEST", "Service - close() : called");
        try {
            if(socket != null && !socket.isClosed()) {
                socket.close();
                if(input != null)   input.close();
                if(output != null)  output.close();
                if(connected == true) {
                    Log.i("TEST", "Service - close() : success");
                    connected = false;
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
            Log.i("TEST", "Service - close() : " + e);
            return false;
        }
        return true;
    } // close()

    private void send(String data) {
        Runnable sender = new Sender(data);
        executor.submit(sender);
    }

    // create Notify channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    } // createNotificationChannel

    //
    class Sender implements Runnable {

        String data = "";

        Sender(String data) {
            this.data = data;
        }

        @Override
        public void run() {
            if(socket.isConnected() && ! socket.isClosed()) {
                try {
                    output.println(data);
                    output.flush();
                } catch (Exception e) {
                    Log.i("TEST", "Service - send() : " + e);
                }
            }
        }
    } // Sender.class

}
