package rocks.leonti.idlealert;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import rocks.leonti.idlealert.model.Check;

public class CheckDao {

    private static String APP = "rocks.leonti.idlealert";
    private static String CHECK_KEY = "check";

    private static String TIMESTAMP = "timestamp";
    private static String STEPS = "steps";

    private final Context context;

    public CheckDao(Context context) {
        this.context = context;
    }

    void saveCheck(Check check) {
        Log.i("CHECK DAO", "saving check " + toString(check));
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(CHECK_KEY, toString(check));
        editor.commit();
    }

    Check readCheck() {
        String checkAsString = getSharedPreferences().getString(CHECK_KEY, null);
        Log.i("CHECK DAO", "reading check " + checkAsString);
        return checkAsString != null ? toCheck(checkAsString) : null;
    }

    void resetCheck() {
        Log.i("CHECK DAO", "reset check");
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(CHECK_KEY);
        editor.commit();
    }

    String toString(Check check) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(TIMESTAMP, check.timestamp);
            jsonObject.put(STEPS, check.steps);

            return jsonObject.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    Check toCheck(String jsonString) {
        try {
        JSONObject jsonObject = new JSONObject(jsonString);

            return new Check(jsonObject.getLong(TIMESTAMP), jsonObject.getInt(STEPS));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(APP, Context.MODE_PRIVATE);
    }
}
