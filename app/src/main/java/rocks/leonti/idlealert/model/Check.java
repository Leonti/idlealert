package rocks.leonti.idlealert.model;

public class Check {

    public final long timestamp;
    public final int steps;

    public Check(long timestamp, int steps) {
        this.timestamp = timestamp;
        this.steps = steps;
    }

    @Override
    public String toString() {
       return "Steps: " + steps + ", timestamp: " + timestamp;
    }
}
