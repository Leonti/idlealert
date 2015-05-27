package rocks.leonti.idlealert;

import java.util.UUID;

import rocks.leonti.idlealert.model.GattAttributes;

public class MiBandConstants {

    public final static UUID SERVICE_MILI = UUID.fromString(GattAttributes.MILI_SERVICE);
    public final static UUID UUID_INFO = UUID.fromString(GattAttributes.CHAR_INFO);
    public final static UUID UUID_DEVICE_NAME = UUID.fromString(GattAttributes.CHAR_DEVICE_NAME);
    public final static UUID UUID_USER_INFO = UUID.fromString(GattAttributes.CHAR_USER_INFO);
    public final static UUID UUID_CONTROL_POINT = UUID.fromString(GattAttributes.CHAR_CONTROL_POINT);
    public final static UUID UUID_REALTIME_STEPS = UUID.fromString(GattAttributes.CHAR_REALTIME_STEPS);
    public final static UUID UUID_ACTIVITY = UUID.fromString(GattAttributes.CHAR_ACTIVITY);
    public final static UUID UUID_LE_PARAMS = UUID.fromString(GattAttributes.CHAR_LE_PARAMS);
    public final static UUID UUID_BATTERY = UUID.fromString(GattAttributes.CHAR_BATTERY);
    public final static UUID UUID_PAIR = UUID.fromString(GattAttributes.CHAR_PAIR);

    public final static UUID SERVICE_ALERT = UUID.fromString(GattAttributes.ALERT_SERVICE);
    public final static UUID UUID_ALERT = UUID.fromString(GattAttributes.CHAR_ALERT);

}