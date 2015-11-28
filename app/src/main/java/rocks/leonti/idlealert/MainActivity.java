package rocks.leonti.idlealert;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {

    private static String TAG = "MainActivity";

    CheckService checkService;
    boolean isServiceBound = false;

    public ServiceConnection checkServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d("ServiceConnection","connected");
            checkService = ((CheckService.CheckServiceBinder) binder).getService();

            isServiceBound = true;

            if (checkService.getStatus().isPresent()) {
                updateTextFields(checkService.getStatus().get());
            } else {
                Log.i("MAIN ACTIVITY", "status is not present");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("ServiceConnection","disconnected");
            checkService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonToggle = (Button) findViewById(R.id.button_stop);
        buttonToggle.setText("Stop");
        buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Stopping service and closing");

                unbindService(checkServiceConnection);
                isServiceBound = false;

                stopService(new Intent(MainActivity.this, CheckService.class));

                MainActivity.this.finish();
            }
        });

    }

    private void updateTextFields(CheckService.Status status) {
        Log.i("MAIN ACTIVITY", "Updating UI");

        ((TextView) findViewById(R.id.textView_reading)).setText("");
        ((TextView) findViewById(R.id.textView_battery)).setText(status.battery + "%");
        ((TextView) findViewById(R.id.textView_steps)).setText(status.steps + "");
        ((TextView) findViewById(R.id.textView_left)).setText(status.getStepsLeft() + "");
        ((TextView) findViewById(R.id.textView_when)).setText(new SimpleDateFormat("HH:mm").format(new Date(status.timestamp)));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
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
        LocalBroadcastManager.getInstance(this).registerReceiver(updatesReceiver, new IntentFilter("status-update"));
        super.onResume();
    }

    private BroadcastReceiver updatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "Received broadcast '" + intent.getStringExtra("type") + "'");

            if (checkService.getStatus().isPresent()) {
                updateTextFields(checkService.getStatus().get());
            } else {
                Log.i("MAIN ACTIVITY", "received broadcast, but status is not present");
            }
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, CheckService.class);
        startService(intent);
        bindService(intent, checkServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(checkServiceConnection);
            isServiceBound = false;
        }
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
