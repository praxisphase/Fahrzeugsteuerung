package fh.praxisphase.fahrzeugsteuerung.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Diese Klasse lädt und speichert die Fahrzeugliste auf dem Handy.
 */
public class MySharedPreferences{
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "MySharedPrefs";
    private SharedPreferences sharedPreferences;


    private final String VEHICLE_CONTAINER = "vehicleContainer";


    public MySharedPreferences(Context context){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Lädt die Fahrzeugliste vom Handy.
     *
     * @return gibt die Fahrzeugliste als einen JSON-Objekt String zurück
     * */
    public String getVehicleContainer(){
        return sharedPreferences.getString(VEHICLE_CONTAINER, null);
    }

    /**
     * Speichert die Fahrzeugliste auf dem Handy
     *
     * @param vehicleContainer die zu speichernde Fahrzeugliste als JSON-Objekt String
     * */
    public void saveVehicleContainer(String vehicleContainer){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VEHICLE_CONTAINER, vehicleContainer);
        editor.apply();
    }
}