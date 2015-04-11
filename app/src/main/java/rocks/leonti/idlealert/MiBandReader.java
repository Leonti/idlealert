package rocks.leonti.idlealert;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.leonti.idlealert.model.Battery;
import rocks.leonti.idlealert.model.LeParams;
import rocks.leonti.idlealert.model.MiBand;

public class MiBandReader {

    interface OnDataUpdateCallback {
        void onDataUpdate(MiBand miBand);
    }

    interface OnErrorCallback {
        void onError(String message);
    }

    private final Context context;
    private final Handler handler;
    private final OnDataUpdateCallback onDataUpdateCallback;
    private final OnErrorCallback onErrorCallback;
    private MiBand mMiBand = new MiBand();

    private BluetoothLeService mBluetoothLeService;

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private String DEVICE_ADDRESS = "88:0F:10:64:62:9E";
    private int count = 0;

    public MiBandReader(Context context, Handler handler, OnDataUpdateCallback onDataUpdateCallback, OnErrorCallback onErrorCallback) {
        this.context = context;
        this.handler = handler;
        this.onDataUpdateCallback = onDataUpdateCallback;
        this.onErrorCallback = onErrorCallback;
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    void refresh() {

        if (mBluetoothLeService == null) {
            Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
            context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);

        } else if (!mBluetoothLeService.isConnected()) {
            mBluetoothLeService.connect(DEVICE_ADDRESS);
        } else {
            getGattService(mBluetoothLeService.getMiliService());
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onErrorCallback.onError("Unable to initialize Bluetooth");
                    }
                });
            }
            mBluetoothLeService.connect(DEVICE_ADDRESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                getGattService(mBluetoothLeService.getMiliService());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                BluetoothGattCharacteristic characteristic;
                byte[] val = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (val != null && val.length > 0) {
                    switch (count) {
                        case 0:
                            byte[] version = Arrays.copyOfRange(val, val.length - 4, val.length);
                            mMiBand.setFirmware(version);
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_DEVICE_NAME);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 1:
                            mMiBand.setName(new String(val));
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_REALTIME_STEPS);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 2:
                            mMiBand.setSteps(0xff & val[0] | (0xff & val[1]) << 8);
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_LE_PARAMS);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 3:
                            LeParams params = LeParams.fromByte(val);
                            mMiBand.setLeParams(params);
                            count++;
                            characteristic = map.get(BluetoothLeService.UUID_BATTERY);
                            mBluetoothLeService.readCharacteristic(characteristic);
                            break;
                        case 4:
                            Battery battery = Battery.fromByte(val);
                            mMiBand.setBattery(battery);
                            mBluetoothLeService.disconnect();
                            count = 0;

                            onDataUpdateCallback.onDataUpdate(mMiBand);
                            //handler.post(new Runnable() {
                                //@Override
                               // public void run() {
                               //     onDataUpdateCallback.onDataUpdate(mMiBand);
                             //   }
                           // });
                            break;
                    }
                }
            }
        }
    };

    private void getGattService(BluetoothGattService service) {
        if (service == null) return;

        BluetoothGattCharacteristic characteristic;

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_DEVICE_NAME);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_USER_INFO);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_CONTROL_POINT);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_REALTIME_STEPS);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_ACTIVITY);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_LE_PARAMS);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_BATTERY);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_PAIR);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(BluetoothLeService.UUID_INFO);
        map.put(characteristic.getUuid(), characteristic);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}
