package rocks.leonti.idlealert;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.leonti.idlealert.model.Battery;
import rocks.leonti.idlealert.model.LeParams;
import rocks.leonti.idlealert.model.MiBand;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        println("Initializing ...");

        final MiBandReader miBandReader = new MiBandReader(this, new Handler(), new MiBandReader.OnDataUpdateCallback() {
            @Override
            public void onDataUpdate(MiBand miBand) {
                println("Steps: " + miBand.mSteps);
            }
        },
                new MiBandReader.OnErrorCallback() {
                    @Override
                    public void onError(String message) {
                        println("ERROR: " + message);
                    }
                });

        miBandReader.refresh();

        Button buttonRefresh = (Button) findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                println("Refreshing ...");
                miBandReader.refresh();
            }
        });
    }

    private void println(String line) {
        TextView debug_log = (TextView) findViewById(R.id.debug_log);
        debug_log.setText(debug_log.getText() + line + "\n");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
