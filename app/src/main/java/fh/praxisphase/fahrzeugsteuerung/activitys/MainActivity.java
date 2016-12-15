package fh.praxisphase.fahrzeugsteuerung.activitys;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import fh.praxisphase.fahrzeugsteuerung.R;
import fh.praxisphase.fahrzeugsteuerung.daten.VehicleContainer;
import fh.praxisphase.fahrzeugsteuerung.fragments.AddVehicleFragment;
import fh.praxisphase.fahrzeugsteuerung.fragments.ChoseVehicleFragment;
import fh.praxisphase.fahrzeugsteuerung.utility.BundleConstants;
import fh.praxisphase.fahrzeugsteuerung.utility.MySharedPreferences;

/**
 * Die Activity beinhaltet die Fragmente zum hinzufügen und bearbeiten von Fahrzeugen, sowie die Auswahlliste
 * der vorhandenen Fahrzeuge. Sie ist außerdem im Manifest als Start Activity eingetragen.
 * */
public class MainActivity extends AppCompatActivity {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "MainActivity";

    private String snackbarMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity_layout);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            snackbarMessage = bundle.getString(BundleConstants.SNACKBAR_MESSAGE, "");
        }

        if(savedInstanceState == null) {
            VehicleContainer vehicleContainer = new VehicleContainer(new MySharedPreferences(this).getVehicleContainer());
            if (vehicleContainer.size() == 0) {
                startCreateNewVehicle();
            } else {
                startChoseVehicleFragment();
            }
        }
    }


    /**
     * Lädt das Fragment mit der Fahrzeugliste.
     * */
    private void startChoseVehicleFragment(){
        ChoseVehicleFragment choseVehicleFragment = new ChoseVehicleFragment();

        if(!snackbarMessage.equals("")){
            Bundle bundle = new Bundle();
            bundle.putString(BundleConstants.SNACKBAR_MESSAGE, snackbarMessage);
            choseVehicleFragment.setArguments(bundle);
            snackbarMessage = "";
        }

        getSupportFragmentManager().beginTransaction().add(R.id.mainActivityLayoutFragmentContainer, choseVehicleFragment).commit();
    }

    /**
     * Lädt das AddVehicleFragment um ein neuese Fahrzeug zu erstellen.
     **/
    private void startCreateNewVehicle(){
        AddVehicleFragment addVehicleFragment = new AddVehicleFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(BundleConstants.NEW_VEHICLE, true);
        addVehicleFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.mainActivityLayoutFragmentContainer, addVehicleFragment).commit();
    }
}