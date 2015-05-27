package rocks.leonti.idlealert;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rocks.leonti.idlealert.model.Battery;
import rocks.leonti.idlealert.model.LeParams;
import rocks.leonti.idlealert.model.MiBand;

public class MiBandReader {

    private static String TAG = MiBandReader.class.getSimpleName();

    interface OnDataUpdateCallback {
        void onDataUpdate(MiBand miBand);
    }

    interface OnErrorCallback {
        void onError(String message);
    }

    private final Context context;
    private final OnDataUpdateCallback onDataUpdateCallback;
    private final OnErrorCallback onErrorCallback;
    private MiBand mMiBand = new MiBand();

    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private String DEVICE_ADDRESS = "88:0F:10:64:62:9E";
    private int count = 0;

    public MiBandReader(Context context, OnDataUpdateCallback onDataUpdateCallback, OnErrorCallback onErrorCallback) {
        this.context = context;
        this.onDataUpdateCallback = onDataUpdateCallback;
        this.onErrorCallback = onErrorCallback;
    }

    void refresh() {
        connect(DEVICE_ADDRESS);
    }

    public boolean connect(final String address) {

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.v(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.v(TAG, "Connected to GATT server.");
                    Log.v(TAG, "Attempting to start service discovery:" + gatt.discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.v(TAG, "Disconnected from GATT server.");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    startReading(gatt);
                } else {
                    Log.v(TAG, "onServicesDiscovered received: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    readData(gatt, characteristic.getValue());
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                Log.i(TAG, "Characteristic changed!");
            }

        });

        return true;
    }

    private void readData(BluetoothGatt gatt, byte[] val) {
        BluetoothGattCharacteristic characteristic;
        if (val != null && val.length > 0) {
            switch (count) {
                case 0:
                    byte[] version = Arrays.copyOfRange(val, val.length - 4, val.length);
                    mMiBand.setFirmware(version);
                    count++;
                    characteristic = map.get(MiBandConstants.UUID_DEVICE_NAME);
                    gatt.readCharacteristic(characteristic);
                    break;
                case 1:
                    mMiBand.setName(new String(val));
                    count++;
                    characteristic = map.get(MiBandConstants.UUID_REALTIME_STEPS);
                    gatt.readCharacteristic(characteristic);
                    break;
                case 2:
                    mMiBand.setSteps(0xff & val[0] | (0xff & val[1]) << 8);
                    count++;
                    characteristic = map.get(MiBandConstants.UUID_LE_PARAMS);
                    gatt.readCharacteristic(characteristic);
                    break;
                case 3:
                    LeParams params = LeParams.fromByte(val);
                    mMiBand.setLeParams(params);
                    count++;
                    characteristic = map.get(MiBandConstants.UUID_BATTERY);
                    gatt.readCharacteristic(characteristic);
                    break;
                case 4:
                    Battery battery = Battery.fromByte(val);
                    mMiBand.setBattery(battery);
                    gatt.disconnect();
                    count = 0;

                    onDataUpdateCallback.onDataUpdate(mMiBand);
                    gatt.disconnect();
                    gatt.close();
                    break;
            }
        }
    }

    private void startReading(BluetoothGatt gatt) {
        fillMap(gatt.getService(MiBandConstants.SERVICE_MILI));

        gatt.readCharacteristic(gatt.getService(MiBandConstants.SERVICE_MILI).getCharacteristic(MiBandConstants.UUID_INFO));
    }

    private void fillMap(BluetoothGattService service) {
        BluetoothGattCharacteristic characteristic;
        characteristic = service.getCharacteristic(MiBandConstants.UUID_DEVICE_NAME);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_USER_INFO);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_CONTROL_POINT);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_REALTIME_STEPS);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_ACTIVITY);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_LE_PARAMS);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_BATTERY);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_PAIR);
        map.put(characteristic.getUuid(), characteristic);

        characteristic = service.getCharacteristic(MiBandConstants.UUID_INFO);
        map.put(characteristic.getUuid(), characteristic);
    }

}
