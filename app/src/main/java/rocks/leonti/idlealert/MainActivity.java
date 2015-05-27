package rocks.leonti.idlealert;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import rocks.leonti.idlealert.model.Check;
import rocks.leonti.idlealert.model.MiBand;


public class MainActivity extends Activity {

    private static String TAG = "MainActivity";
    private static String STARTED = "started";
    private static String BATTERY = "battery";
    private static String STEPS = "steps";
    private static String LEFT = "left";
    private static String WHEN = "when";

    private boolean started = false;
    private String batteryLabel = "";
    private String stepsLabel = "";
    private String leftLabel = "";
    private String whenLabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            started = savedInstanceState.getBoolean(STARTED);
            batteryLabel = savedInstanceState.getString(BATTERY);
            stepsLabel = savedInstanceState.getString(STEPS);
            leftLabel = savedInstanceState.getString(LEFT);
            whenLabel = savedInstanceState.getString(WHEN);
        }

        final Button buttonToggle = (Button) findViewById(R.id.button_toggle);
        buttonToggle.setText(started ? "Stop" : "Start");
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (started) {
                    CheckService.cancel(MainActivity.this);
                    buttonToggle.setText("Start");
                    started = false;
                } else {
                    CheckDao checkDao = new CheckDao(MainActivity.this);
                    checkDao.resetCheck();
                    CheckService.schedule(MainActivity.this, 0);
                    buttonToggle.setText("Stop");
                    started = true;
                }

            }
        });

        updateTextFields();
    }

    private void updateTextFields() {
        ((TextView) findViewById(R.id.textView_battery)).setText(batteryLabel);
        ((TextView) findViewById(R.id.textView_steps)).setText(stepsLabel);
        ((TextView) findViewById(R.id.textView_left)).setText(leftLabel);
        ((TextView) findViewById(R.id.textView_when)).setText(whenLabel);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(STARTED, started);
        savedInstanceState.putString(BATTERY, batteryLabel);
        savedInstanceState.putString(STEPS, stepsLabel);
        savedInstanceState.putString(LEFT, leftLabel);
        savedInstanceState.putString(WHEN, whenLabel);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is paused.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updatesReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(updatesReceiver, new IntentFilter("custom-event-name"));
        super.onResume();
    }

    private BroadcastReceiver updatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "Received broadcast '" + intent.getStringExtra("type") + "'");

            if (intent.getStringExtra("type").equals("hardware")) {
                batteryLabel = intent.getStringExtra("battery") + "%";
                stepsLabel = intent.getStringExtra("steps");
            } else if (intent.getStringExtra("type").equals("check")) {
                leftLabel = intent.getStringExtra("left");
            }

            whenLabel = new SimpleDateFormat("HH:mm").format(new Date());
            updateTextFields();
        }
    };

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
