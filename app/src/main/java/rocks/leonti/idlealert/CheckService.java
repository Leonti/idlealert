package rocks.leonti.idlealert;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.SettableFuture;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import rocks.leonti.idlealert.model.Check;
import rocks.leonti.idlealert.model.MiBand;

public class CheckService extends Service {

    public static String TAG = "CHECK SERVICE";

    public static int REQUIRED_STEPS = 100;
    public static long CHECK_INTERVAL = 25 * 60 * 1000;
    public static long RECHECK_INTERVAL = 60 * 1000;

/*
    public static int REQUIRED_STEPS = 10;
    public static long CHECK_INTERVAL = 2 * 60 * 1000;
    public static long RECHECK_INTERVAL = 30 * 1000;
*/
    @Override
    public void onCreate() {


    }

    private void check() {
        Optional<Integer> steps = readSteps();

        if (steps.isPresent()) {

            Log.i(TAG, "Successfully read steps: " + steps.get());
            CheckDao checkDao = new CheckDao(CheckService.this);
            Check lastCheck = checkDao.readCheck();

            if (lastCheck != null) {
                Log.i(TAG, "Last check is there: " + lastCheck);
                if (now() - lastCheck.timestamp >= CHECK_INTERVAL) {
                    if (steps.get() - lastCheck.steps < REQUIRED_STEPS) {
                        Log.i(TAG, "Steps done after last check (" + (steps.get() - lastCheck.steps)
                                + ") are less than required amount (" + REQUIRED_STEPS + "), rescheduling");

                        showNotification();
                        scheduleRecheck();
                        broadcastStepsLeft(REQUIRED_STEPS - (steps.get() - lastCheck.steps));
                    } else {
                        Log.i(TAG, "Steps done after last check (" + (steps.get() - lastCheck.steps)
                                + ") are more than required amount (" + REQUIRED_STEPS + "), scheduling next check");

                        checkDao.saveCheck(new Check(now(), steps.get()));
                        hideNotification();
                        scheduleCheck();
                        broadcastStepsLeft(0);
                    }
                }
            } else {
                Log.i(TAG, "Last check is empty, rescheduling");
                broadcastStepsLeft(0);
                checkDao.saveCheck(new Check(now(), steps.get()));
                scheduleCheck();
            }

        } else {
            Log.i(TAG, "Failed to read steps, rescheduling the check");
            broadcastStepsLeft(0);
            showNotification();
            scheduleRecheck();
        }
    }

    private void broadcastStepsLeft(int left) {
        Log.d(TAG, "Broadcasting left steps");
        Intent intent = new Intent("custom-event-name");
        intent.putExtra("type", "check");
        intent.putExtra("left", String.valueOf(left));
        LocalBroadcastManager.getInstance(CheckService.this).sendBroadcast(intent);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private Optional<Integer> readSteps() {
        try {
            return Optional.of(readStepsTry().get(20, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return Optional.absent();
    }

    private SettableFuture<Integer> readStepsTry() {
        Log.i(TAG, "Trying to read steps");
        final SettableFuture<Integer> stepsFuture = SettableFuture.create();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                MiBandReader miBandReader = new MiBandReader(CheckService.this,
                        new MiBandReader.OnDataUpdateCallback() {
                            @Override
                            public void onDataUpdate(MiBand miBand) {
                                Log.i(TAG, "Steps from band: " + miBand.mSteps);
                                stepsFuture.set(miBand.mSteps);

                                Log.d(TAG, "Broadcasting message");
                                Intent intent = new Intent("custom-event-name");
                                intent.putExtra("type", "hardware");
                                intent.putExtra("battery", String.valueOf(miBand.mBattery.mBatteryLevel));
                                intent.putExtra("steps", String.valueOf(miBand.mSteps));
                                LocalBroadcastManager.getInstance(CheckService.this).sendBroadcast(intent);
                            }
                        },
                        new MiBandReader.OnErrorCallback() {
                            @Override
                            public void onError(String message) {
                                Log.i(TAG, "ERROR: " + message);
                                stepsFuture.setException(new RuntimeException(message));
                            }
                        });

                Log.i(TAG, "Refreshing miReader");
                miBandReader.refresh();
                Log.i(TAG, "MiReader refreshed");
            }
        };
        new Thread(runnable).start();

        return stepsFuture;
    }

    public void showNotification() {
        Log.i(TAG, "SHOWING NOTIFICATION");

        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_action_movement)
                .setContentTitle("Idle Alert") // title for notification
                .setContentText("Move your ass!");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

    public void hideNotification() {
        Log.i(TAG, "HIDING NOTIFICATION");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }

    public void scheduleRecheck() {
        Log.i(TAG, "SCHEDULING RECHECK");
        schedule(this, RECHECK_INTERVAL);
    }

    public void scheduleCheck() {
        Log.i(TAG, "SCHEDULING CHECK");
        schedule(this, CHECK_INTERVAL);
    }

    public static void schedule(Context context, long timeFromNow) {
        Log.i(TAG, "scheduling check for " + (timeFromNow/1000) + "s from now");

        Intent intent = new Intent(context, CheckService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarm.setExact(AlarmManager.RTC, System.currentTimeMillis() + timeFromNow, pIntent);
    }

    public static void cancel(Context context) {
        Intent intent = new Intent(context, CheckService.class);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new AsyncTask<Void, Void, Void> () {

            @Override
            protected Void doInBackground(Void... params) {
                check();
                return null;
            }
        }.execute();


        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
