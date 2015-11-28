package rocks.leonti.idlealert.model;

public class Check {

    public final long timestamp;
    public final int steps;
    public final int batteryLevel;

    public Check(long timestamp, int steps, int batteryLevel) {
        this.timestamp = timestamp;
        this.steps = steps;
        this.batteryLevel = batteryLevel;
    }

    @Override
    public String toString() {
       return "Steps: " + steps + ", timestamp: " + timestamp + ", batteryLevel: " + batteryLevel;
    }
}
