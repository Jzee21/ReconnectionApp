package com.example.clientservice2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView main_textview;
    private Button main_button;
    private boolean onService = false;
    private BroadcastReceiver mBroadcastReceiver ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main_textview = (TextView) findViewById(R.id.main_textview);
        main_button = (Button) findViewById(R.id.main_button);

        Log.i("TEST", "Main - before service start");

        Intent intent = new Intent(MainActivity.this, ClientService.class);
//        intent.putExtra("DATA", "데이터 좀 가라");
//        startService(intent);

        if(Build.VERSION.SDK_INT>=26) {
            getApplicationContext().startForegroundService(intent);
        } else {
            getApplicationContext().startService(intent);
        }

        Log.i("TEST", "Main - success service start");

        mBroadcastReceiver = new BroadcastReceiver() {
            int code = this.hashCode();
            @Override
            public void onReceive(Context context, Intent intent) {
                String data = intent.getStringExtra("DATA");
                Log.i("TEST", "Activity - LocalReceiver_" + code + " ] " + data);
                main_textview.append(data);
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, new IntentFilter("activityReceiver"));

        main_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("serviceReceiver");
                intent.putExtra("DATA", "서비스 켜라");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i("TEST", "Main - onNewIntent");
        String data = intent.getExtras().getString("RESULT");
        Log.i("TEST", "Main - receive data : " + data);
        main_textview.append(data + "\n");
    }

}
