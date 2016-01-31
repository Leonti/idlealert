package rocks.leonti.idlealert;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rocks.leonti.idlealert.model.Check;
import rocks.leonti.idlealert.model.MiBand;

public class CheckService extends Service {

    public static String TAG = "CHECK SERVICE";

    public static int REQUIRED_STEPS = 100;
    public static long CHECK_INTERVAL = 30 * 60 * 1000;
    public static long RECHECK_INTERVAL = 60 * 1000;

    private IBinder mBinder = new CheckServiceBinder();
/*
    public static int REQUIRED_STEPS = 10;
    public static long CHECK_INTERVAL = 60 * 1000;
    public static long RECHECK_INTERVAL = 30 * 1000;
*/
    private Optional<Check> lastCheck = Optional.absent();

    public static class Status {
        public final int steps;
        public final int previousCheckSteps;
        public final int battery;
        public final long timestamp;

        public Status(int steps, int previousCheckSteps, int battery, long timestamp) {
            this.steps = steps;
            this.previousCheckSteps = previousCheckSteps;
            this.battery = battery;
            this.timestamp = timestamp;
        }

        public int getStepsLeft() {
            if (previousCheckSteps == 0) {
                return 0;
            }

            int stepsLeft = REQUIRED_STEPS - (steps - previousCheckSteps);

            return stepsLeft < 0 ? 0 : stepsLeft;
        }

    }

    public Optional<Status> status = Optional.absent();

    public Optional<Status> getStatus() {
        return status;
    }

    @Override
    public void onCreate() {
        startForeground(42, createNotification(this));

        Log.i(TAG, "ON CREATE COMMAND");

        performCheckAsync();
    }

    private void performCheckAsync() {
        new AsyncTask<Void, Void, Void> () {

            @Override
            protected Void doInBackground(Void... params) {
                check();
                return null;
            }
        }.execute();
    }

    @Override
    public void onDestroy() {
        hideNotification(42);
        hideNotification(43);

        cancel(this);
    }

    private static Notification createNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentTitle("Idle Alert is running")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_stat_action_movement)
            .setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), 0));

        return builder.build();
    }

    private void check() {
        Optional<Check> check = performCheck();

        if (check.isPresent()) {

            if (lastCheck.isPresent()) {
                Log.i(TAG, "Last status is there: " + check.get());

                this.status = Optional.of(new Status(check.get().steps, lastCheck.get().steps, check.get().batteryLevel, check.get().timestamp));

                // give a timer 2 seconds because it's not always precise
                if (now() - lastCheck.get().timestamp >= CHECK_INTERVAL) {

                    if (this.status.get().getStepsLeft() > 0) {
                        Log.i(TAG, "Steps done after last check (" + (check.get().steps - lastCheck.get().steps)
                                + ") are less than required amount (" + REQUIRED_STEPS + "), rescheduling");

                        showNotification();
                        scheduleRecheck();

                    } else {
                        Log.i(TAG, "Steps done after last check (" + (check.get().steps - lastCheck.get().steps)
                                + ") are more than required amount (" + REQUIRED_STEPS + "), scheduling next check");

                        lastCheck = check;
                        hideNotification(43);
                        scheduleCheck();
                    }

                    broadcastUpdate();
                } else {
                    Log.i(TAG, "Less than check interval " + (now() - lastCheck.get().timestamp));
                }
            } else {
                lastCheck = check;
                scheduleCheck();
                this.status = Optional.of(new Status(check.get().steps, 0, check.get().batteryLevel, check.get().timestamp));
                broadcastUpdate();
            }

        } else {
            Log.i(TAG, "Failed to read steps, rescheduling the check");
            showNotification();
            scheduleRecheck();
        }
    }

    private void broadcastUpdate() {
        Log.d(TAG, "Broadcasting status update");
        Intent intent = new Intent("status-update");

        LocalBroadcastManager.getInstance(CheckService.this).sendBroadcast(intent);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private Optional<Check> performCheck() {
        try {
            return Optional.of(tryCheck().get(20, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return Optional.absent();
    }

    private SettableFuture<Check> tryCheck() {
        Log.i(TAG, "Trying to read steps");
        final SettableFuture<Check> checkFuture = SettableFuture.create();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                MiBandReader miBandReader = new MiBandReader(CheckService.this,
                        new MiBandReader.OnDataUpdateCallback() {
                            @Override
                            public void onDataUpdate(MiBand miBand) {
                                Log.i(TAG, "Steps from band: " + miBand.mSteps);
                                checkFuture.set(new Check(System.currentTimeMillis(), miBand.mSteps, miBand.mBattery.mBatteryLevel));
                            }
                        },
                        new MiBandReader.OnErrorCallback() {
                            @Override
                            public void onError(String message) {
                                Log.i(TAG, "ERROR: " + message);
                                checkFuture.setException(new RuntimeException(message));
                            }
                        });

                Log.i(TAG, "Refreshing miReader");
                miBandReader.refresh();
                Log.i(TAG, "MiReader refreshed");
            }
        };
        new Thread(runnable).start();

        return checkFuture;
    }

    private void showNotification() {
        Log.i(TAG, "SHOWING NOTIFICATION");

        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_action_movement)
                .setAutoCancel(true)
                .setContentTitle("Idle Alert") // title for notification
                .setContentText("Move your ass!")
                .setContentIntent(PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class), 0))
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setLights(Color.MAGENTA, 400, 400);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(43, mBuilder.build());
    }

    private void hideNotification(int id) {
        Log.i(TAG, "HIDING NOTIFICATION");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }

    private void scheduleRecheck() {
        Log.i(TAG, "SCHEDULING RECHECK");
        schedule(this, RECHECK_INTERVAL);
    }

    private void scheduleCheck() {
        Log.i(TAG, "SCHEDULING CHECK");
        schedule(this, CHECK_INTERVAL);
    }

    private static String IS_SCHEDULED = "IS_SCHEDULED";

    private static void schedule(Context context, long timeFromNow) {
        Log.i(TAG, "scheduling check for " + (timeFromNow/1000) + "s from now");

        Intent intent = new Intent(context, CheckService.class);
        intent.putExtra(IS_SCHEDULED, true);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarm.setExact(AlarmManager.RTC, System.currentTimeMillis() + timeFromNow, pIntent);
    }

    private static void cancel(Context context) {
        Intent intent = new Intent(context, CheckService.class);
        intent.putExtra(IS_SCHEDULED, true);
        PendingIntent pIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "ON START COMMAND");

        if (intent.getBooleanExtra(IS_SCHEDULED, false)) {
            Log.i("TAG", "DOING CHECK FROM ALARM");
            performCheckAsync();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(TAG, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "in onUnbind");
        return true;
    }


    public class CheckServiceBinder extends Binder {
        CheckService getService() {
            return CheckService.this;
        }
    }
}
