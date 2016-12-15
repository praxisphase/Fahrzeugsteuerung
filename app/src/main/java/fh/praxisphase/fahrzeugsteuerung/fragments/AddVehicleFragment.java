package fh.praxisphase.fahrzeugsteuerung.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import fh.praxisphase.fahrzeugsteuerung.daten.Vehicle;
import fh.praxisphase.fahrzeugsteuerung.daten.VehicleContainer;
import fh.praxisphase.fahrzeugsteuerung.utility.BundleConstants;
import fh.praxisphase.fahrzeugsteuerung.utility.IpCheck;
import fh.praxisphase.fahrzeugsteuerung.utility.MySharedPreferences;
import fh.praxisphase.fahrzeugsteuerung.R;
import fh.praxisphase.fahrzeugsteuerung.utility.VehicleCommunication;

/**
 * AddVehicleFragment dient zum hinzufügen eines neuen Fahrzeuges oder zum bearbeiten eines vorhandenen.
 * */
public class AddVehicleFragment extends Fragment implements View.OnFocusChangeListener, TextView.OnEditorActionListener, View.OnClickListener {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "AddVehicleFragment";

    private VehicleContainer vehicleContainer;
    private boolean editVehicle;
    private boolean newVehicle;

    private ProgressDialog progressDialog;
    private Vehicle oldVehicle;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_vehicle_fragment_layout, container, false);

        MySharedPreferences mySharedPreferences = new MySharedPreferences(getContext());
        vehicleContainer = new VehicleContainer(mySharedPreferences.getVehicleContainer());

        Bundle bundle = getArguments();

        if(bundle != null) {
            checkBundle(bundle);
        }

        initLayout(view);

        return view;
    }

    /**
     * Initialisiert das Layout vom Fragment
     */
    private void initLayout(View view) {
        if(!newVehicle){
//            Blende Hinweise "Es ist kein Fahrzeug vorhanden..." aus
            TextView textView = (TextView) view.findViewById(R.id.addVehicleFragmentLayoutTextView);
            textView.setVisibility(View.GONE);
        }

        EditText vehicleName = (EditText) view.findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleName);
        EditText vehicleUrl = (EditText) view.findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleUrl);
        EditText vehicleKey = (EditText) view.findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleKey);
        Button button = (Button) view.findViewById(R.id.addVehicleFragmentLayoutButtonInsertVehicle);

        TextView weitereOptionen = (TextView) view.findViewById(R.id.addVehicleFragmentLayoutTextViewWeitereOptionen);
        weitereOptionen.setOnClickListener(this);
        TextView wenigerOptionen = (TextView) view.findViewById(R.id.addVehicleFragmentLayoutTextViewWenigerOptionen);
        wenigerOptionen.setOnClickListener(this);

        if(editVehicle){
            vehicleName.setText(oldVehicle.getName());
            vehicleUrl.setText(oldVehicle.getUrl());
            vehicleKey.setHint(R.string.fahrzeug_key_hint);

            ((EditText) view.findViewById(R.id.addVehicleFragmentLayoutEditTextSpeedControllerValue)).setText(String.valueOf(oldVehicle.getSpeedSliderRange()));
            ((CheckBox)view.findViewById(R.id.addVehicleFragmentLayoutCheckBoxSpeedControllerInvert)).setChecked(oldVehicle.isInvertSpeedSlider());

            ((EditText)view.findViewById(R.id.addVehicleFragmentLayoutEditTextSteeringControllerValue)).setText(String.valueOf(oldVehicle.getSteeringSliderRange()));
            ((CheckBox)view.findViewById(R.id.addVehicleFragmentLayoutCheckBoxSteeringControllerInvert)).setChecked(oldVehicle.isInvertSteeringSlider());

            button.setText(R.string.speichern);
        }

        button.setOnClickListener(this);
        vehicleName.setOnFocusChangeListener(this);
        vehicleKey.setOnEditorActionListener(this);
        vehicleUrl.setOnFocusChangeListener(this);
    }

    /**
     * Prüft die übergebenen Parameter.
     *
     * @param bundle Das Bundle mit den Parametern aus dem Intent
     * */
    private void checkBundle(Bundle bundle) {
        if(editVehicle = bundle.getBoolean(BundleConstants.EDIT_VEHICLE, false)) {
            oldVehicle = vehicleContainer.getVehicle(vehicleContainer.getPosition(bundle.getString(BundleConstants.VEHICLE_NAME)));
        }
        newVehicle = bundle.getBoolean(BundleConstants.NEW_VEHICLE, false);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch(v.getId()) {
            case R.id.addVehicleFragmentLayoutEditTextVehicleName:
                if (!hasFocus) {
//                    Prüft, ob der Fahrzeugname schon verwendet wird.
                    String vehicleName = ((EditText) v).getText().toString();
                    if (vehicleName.length() > 0) {
                        if (isVehicleNameAlreadyInUse(vehicleName)) {
                            vehicleNameAlreadyExist();
                        } else {
                            changeVehicleNameTextViewBottomColor(false);
                        }
                    }
                }
                break;
            case R.id.addVehicleFragmentLayoutEditTextVehicleUrl:
                if (!hasFocus) {
//                    Prüft, ob es sich bei der Eingabe um eine richtige URL handelt.
                    checkVehicleUrlTextField(((EditText) v).getText().toString());
                }
                break;
        }
    }

    /**
     * Dialog wird angezeigt, wenn es schon ein Fahrzeug mit der URL gibt.
     * Wenn der User den Dialog bestätigt wird das vorhandene Fahrzeug durch das neue Fahrzeug ersetzt.
     *
     * @param position position vom Fahrzeug in der Fahrzeugliste
     *
     * @return Der fertige AlertDialog
     * */
    private AlertDialog replaceVehicleDialog(final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        closeInsertProgressDialog();
        builder.setView(R.layout.dialog);
        builder.setPositiveButton(R.string.loeschen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                vehicleContainer.delete(position);
                insertVehicle();
            }
        });

        builder.setNegativeButton(R.string.abbrechen, null);

        return  builder.create();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_ACTION_DONE){
            insertVehicle();
        }

        return false;
    }


    private void showInsertProgressDialog(){
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Fahrzeug wird gespeichert.");
        progressDialog.show();
    }

    private void closeInsertProgressDialog(){
        if(progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }


    /**
     * Beendet das Fragment und kehrt zum ChoseVehicleFragment zurück.
     *
     * @param vehicleInserted Gibt an, ob ein Fahrzeug gespeichert wurde
     * */
    private void closeFragment(boolean vehicleInserted){
        if (newVehicle) {
            ChoseVehicleFragment choseVehicleFragment = new ChoseVehicleFragment();
            if(vehicleInserted) {
                Bundle bundle = new Bundle();
                bundle.putString(BundleConstants.SNACKBAR_MESSAGE, getResources().getString(R.string.fahrzeug_eingetragen));
                choseVehicleFragment.setArguments(bundle);
            }
            getFragmentManager().beginTransaction().replace(R.id.mainActivityLayoutFragmentContainer, choseVehicleFragment).commit();
        } else{
            Intent intent = new Intent();
            if(vehicleInserted) {
                if (editVehicle) {
                    intent.putExtra(BundleConstants.SNACKBAR_MESSAGE, R.string.fahrzeug_bearbeitet);
                } else {
                    intent.putExtra(BundleConstants.SNACKBAR_MESSAGE, getResources().getString(R.string.fahrzeug_eingetragen));
                }
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
            getFragmentManager().popBackStack();
        }
    }

    /**
     * Prüft, ob eine URL bereits von einem anderen Fahrzeug verwendet wird.
     *
     * @param url Die zu prüfende URL.
     *
     * @return -1 wenn die URL noch nicht verwendet wird, ansonsten die Position vom Fahrzeug in der Fahrzeugliste
     * */
    public int isVehicleUrlAlreadyInUse(String url){
        if(oldVehicle != null) {
            if (!url.equals(oldVehicle.getUrl())) {
                return -1;
            }
        }
        for (int i = 0; i < vehicleContainer.size(); i++) {
            if (vehicleContainer.getVehicle(i).getUrl().equals(url))
                return i;
        }

        return -1;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick()");
        if(v.getId() == R.id.addVehicleFragmentLayoutTextViewWeitereOptionen) {

            EditText temp = (EditText) v.getRootView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleKey);
            temp.setImeOptions(EditorInfo.IME_ACTION_NEXT);

            TextView weitereOptionen = (TextView) v.getRootView().findViewById(R.id.addVehicleFragmentLayoutTextViewWeitereOptionen);
            weitereOptionen.setVisibility(View.GONE);
            TextView wenigerOptionen = (TextView) v.getRootView().findViewById(R.id.addVehicleFragmentLayoutTextViewWenigerOptionen);
            wenigerOptionen.setVisibility(View.VISIBLE);
            (v.getRootView().findViewById(R.id.addVehicleFragmentLayoutRelativeLayoutSpeedController)).setVisibility(View.VISIBLE);
            (v.getRootView().findViewById(R.id.addVehicleFragmentLayoutRelativeLayoutSteeringController)).setVisibility(View.VISIBLE);
        } else if(v.getId() == R.id.addVehicleFragmentLayoutTextViewWenigerOptionen){

            EditText temp = (EditText) v.getRootView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleKey);
            temp.setImeOptions(EditorInfo.IME_ACTION_DONE);

            TextView weitereOptionen = (TextView) v.getRootView().findViewById(R.id.addVehicleFragmentLayoutTextViewWeitereOptionen);
            weitereOptionen.setVisibility(View.VISIBLE);
            TextView wenigerOptionen = (TextView) v.getRootView().findViewById(R.id.addVehicleFragmentLayoutTextViewWenigerOptionen);
            wenigerOptionen.setVisibility(View.GONE);
            (v.getRootView().findViewById(R.id.addVehicleFragmentLayoutRelativeLayoutSpeedController)).setVisibility(View.GONE);
            (v.getRootView().findViewById(R.id.addVehicleFragmentLayoutRelativeLayoutSteeringController)).setVisibility(View.GONE);
        } else{
            insertVehicle();
        }
    }

    /**
     * Prüft, ob die Eingabe eine URL ist.
     * Sollte es sich nicht um eine URL handeln, so wird das Textfeld rot markiert und der Nutzer
     * über die Snackbar informiert.
     *
     * @param vehicleUrl Inhalt vom VehicleUrl Textfeld
     * */
    private void checkVehicleUrlTextField(String vehicleUrl){
        if(vehicleUrl.length() > 0) {
            IpCheck ipCheck = new IpCheck();
            if (!ipCheck.validateIp(vehicleUrl)) {
                noValidUrl();
            } else {
                changeVehicleUrlTextViewBottomColor(true);
            }
        }
    }

    /**
     * Prüft, ob beim eintragen des Fahrzeuges eine Verbindung zum Farhzeug besteht.
     * Bei bestehender Verbindung wird das Fahrzeug gespeichert, ansonsten wird ein Dialog angezeigt.
     *
     * @param vehicle Das einzutragene Fahrzeug
     * */
    private void checkVehicleConnection(final Vehicle vehicle){
        VehicleCommunication vehicleCommunication = new VehicleCommunication(getContext(), vehicle.getUrl(), vehicle.getKey());

        vehicleCommunication.setCheckVehicleConnectionListener(new VehicleCommunication.CheckVehicleConnectionListener() {
            @Override
            public void checkVehicleConnection(int connection) {
                switch (connection){
                    case VehicleCommunication.CONNECTION_ERROR:
                        closeInsertProgressDialog();
                        checkVehicleConnectionErrorDialog().show();
                        break;

                    case VehicleCommunication.NOT_AUTHORIZED:
                        closeInsertProgressDialog();
                        vehicleAuthorizationFailedDialog().show();
                        break;

                    case VehicleCommunication.OK:
                        new MySharedPreferences(getContext()).saveVehicleContainer(vehicleContainer.toJsonString());
                        closeInsertProgressDialog();
                        closeFragment(true);
                        break;
                }
            }
        });
        vehicleCommunication.checkVehicleConnection();
    }

    /**
     * Dialog wird angezeigt, wenn keine Verbindung zum Fahrzeug hergestellt werden konnte.
     *
     * @return Der fertige AlertDialog
     * */
    private AlertDialog checkVehicleConnectionErrorDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        closeInsertProgressDialog();

        String message = getResources().getString(R.string.keine_verbindung_zum_fahrzeug) +
                "\n" +
                getResources().getString(R.string.fahrzeug_dennoch_eintragen);

        builder.setMessage(message);
        builder.setNegativeButton(R.string.abbrechen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MySharedPreferences mySharedPreferences = new MySharedPreferences(getContext());
                vehicleContainer = new VehicleContainer(mySharedPreferences.getVehicleContainer());
            }
        });
        builder.setPositiveButton(R.string.eintragen, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new MySharedPreferences(getContext()).saveVehicleContainer(vehicleContainer.toJsonString());
                closeFragment(true);
            }
        });

        return builder.create();
    }

    /**
     * Dialog wird angezeigt, wenn der Nutzer keine Berechtigung hat das Fahrzeug zu steuern.
     *
     * @return Der fertige AlertDialog
     * */
    private AlertDialog vehicleAuthorizationFailedDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        closeInsertProgressDialog();

        String message = getResources().getString(R.string.keine_berechtigung);

        builder.setMessage(message);
        if(editVehicle){
            builder.setNegativeButton(R.string.bearbeitung_verwerfen, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeFragment(false);
                }
            });
        } else {
            builder.setNegativeButton(R.string.verwerfen, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeFragment(false);
                }
            });
        }
        builder.setPositiveButton(R.string.anderes_passwort, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MySharedPreferences mySharedPreferences = new MySharedPreferences(getContext());
                vehicleContainer = new VehicleContainer(mySharedPreferences.getVehicleContainer());
            }
        });

        return builder.create();
    }

    /**
     * Fügt der Fahrzeugliste ein Fahrzeug hinzu.
     * WICHTIG! Das Fahrzeug wird erst gespeichert, wenn eine Verbindung zum Fahrzeug besteht, oder
     * der Nutzer im Dialog angibt, dass das Fahrzeug trotzdem gespeichert werden soll
     * */
    private void insertVehicle() {
        if(getView() != null) {
            showInsertProgressDialog();
            String vehicleName = ((EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleName)).getText().toString();
            String vehicleUrl = ((EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleUrl)).getText().toString();
            String vehicleKey = ((EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleKey)).getText().toString();

            int steeringSliderRange = Integer.parseInt(((EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextSteeringControllerValue)).getText().toString());
            boolean invertSteeringSlider = ((CheckBox) getView().findViewById(R.id.addVehicleFragmentLayoutCheckBoxSteeringControllerInvert)).isChecked();
            int speedSliderRange = Integer.parseInt(((EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextSpeedControllerValue)).getText().toString());
            boolean invertSpeedSlider = ((CheckBox) getView().findViewById(R.id.addVehicleFragmentLayoutCheckBoxSpeedControllerInvert)).isChecked();

            IpCheck ipCheck = new IpCheck();

//            Trage Fahrzeug ein, oder bearbeite ein bestehendes
            if ((!isVehicleNameAlreadyInUse(vehicleName) || editVehicle)
                    && ipCheck.validateIp(vehicleUrl)
                    && vehicleName.length() > 0
                    && vehicleUrl.length() > 0
                    && (isVehicleUrlAlreadyInUse(vehicleUrl) == -1 || editVehicle)) {

                if (editVehicle) {
                    editVehicle(vehicleName, vehicleUrl, vehicleKey, steeringSliderRange, invertSteeringSlider, speedSliderRange, invertSpeedSlider);
                } else {
                    vehicleContainer.add(new Vehicle(vehicleName, vehicleUrl, vehicleKey));
                }

                checkVehicleConnection(vehicleContainer.getVehicle(vehicleContainer.getPosition(vehicleName)));
            }
//            Fahrzeug mit dieser URL ist schon vorhanden, zeige Dialog zum ersetzen an.
            else if (!isVehicleNameAlreadyInUse(vehicleName)
                    && ipCheck.validateIp(vehicleUrl)
                    && vehicleName.length() > 0
                    && vehicleUrl.length() > 0
                    && isVehicleUrlAlreadyInUse(vehicleUrl) != -1) {
                replaceVehicleDialog(isVehicleUrlAlreadyInUse(vehicleUrl)).show();
            }
//            Informiere den Nutzer über die Snackbar und durch markierung der betreffenden Textfelder
//            über fehlerhafte Angaben.
            else {
                if (isVehicleNameAlreadyInUse(vehicleName)) {
                    closeInsertProgressDialog();
                    vehicleNameAlreadyExist();
                }

                if (vehicleName.length() == 0) {
                    closeInsertProgressDialog();
                    changeVehicleNameTextViewBottomColor(true);
                    Snackbar.make(getView(), R.string.geben_sie_einen_namen_ein, Snackbar.LENGTH_SHORT).show();
                }

                if (!ipCheck.validateIp(vehicleUrl) || vehicleUrl.length() == 0) {
                    closeInsertProgressDialog();
                    noValidUrl();
                }
            }
        }
    }

    /**
     * Bearbeitet ein Fahrzeug.
     *
     * @param vehicleName Neuer Fahrzeugname
     * @param vehicleUrl Neue Fahrzeug-URL
     * @param vehicleKey Neuer Fahrzeug-Key als klartext
     * @param steeringSliderRange Die neue Anzahl an möglichen Reglerpositionen.
     * @param invertSteeringSlider Ob der Regler invertiert werden soll.
     * @param speedSliderRange Die neue Anzahl an möglichen Reglerpositionen.
     * @param invertSpeedSlider Ob der Regler invertiert werden soll.
     * */
    private void editVehicle(String vehicleName, String vehicleUrl, String vehicleKey, int steeringSliderRange, boolean invertSteeringSlider, int speedSliderRange, boolean invertSpeedSlider) {
        int position  = vehicleContainer.getPosition(oldVehicle.getName());

        vehicleContainer.getVehicle(position).setName(vehicleName);
        vehicleContainer.getVehicle(position).setUrl(vehicleUrl);
        if(!vehicleKey.equals("")) {
            vehicleContainer.getVehicle(position).setKey(vehicleKey);
        }

        vehicleContainer.getVehicle(position).setSteeringSliderRange(steeringSliderRange);
        vehicleContainer.getVehicle(position).setInvertSteeringSlider(invertSteeringSlider);

        vehicleContainer.getVehicle(position).setSpeedSliderRange(speedSliderRange);
        vehicleContainer.getVehicle(position).setInvertSpeedSlider(invertSpeedSlider);
    }

        /**
         * Informiert den Nutzer in der Snackbar darüber, dass schon ein Fahrzeug mit dem Namen existiert
         * und unterstreicht das TextView vom Namen rot.
         * */
    private void vehicleNameAlreadyExist() {
        if(getView() != null) {
            Snackbar.make(getView(), R.string.fahrzeugname_vorhanden, Snackbar.LENGTH_SHORT).show();
            changeVehicleNameTextViewBottomColor(true);
        }
    }

    /**
     * Ändert den Hintergrund vom TextView vom Fahrzeugnamen.
     * Bei True wird das TextView Rot unterstrichen, bei False wird es normal angezeigt.
     *
     * @param vehicleNameExists Gibt an, ob es ein Fahrzeug mit diesem Namen gibt.
     * */
    private void changeVehicleNameTextViewBottomColor(boolean vehicleNameExists){
        if(getView() != null) {
            EditText vehicleName = (EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleName);

            if (vehicleNameExists) {
                Drawable drawable = vehicleName.getBackground().mutate();
                drawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            } else {
                vehicleName.getBackground().mutate().clearColorFilter();
            }
        }
    }

    /**
     * Prüft, ob es schon ein Fahrzeug mit dem Namen gibt
     *
     * @param vehicleName Name vom neuen Fahrzeug
     *
     * @return true wenn es schon ein Fahrzeug mit diesem Namen gibt, ansonsten false
     * */
    private boolean isVehicleNameAlreadyInUse(String vehicleName){
        return vehicleName.length() > 0 && vehicleContainer.contains(vehicleName);
    }

    /**
     * Ändert den Hintergrund von der Fahrzeug-URL TextView.
     * Bei False wird das TextView Rot unterstrichen, bei True wird es normal angezeigt.
     *
     * @param validUrl Das Resultat der URL Prüfung
     * */
    private void changeVehicleUrlTextViewBottomColor(boolean validUrl) {
        if(getView() != null) {
            EditText vehicleUrlEditText = (EditText) getView().findViewById(R.id.addVehicleFragmentLayoutEditTextVehicleUrl);
            if (!validUrl) {
                Drawable drawable = vehicleUrlEditText.getBackground().mutate();
                drawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            } else {
                vehicleUrlEditText.getBackground().mutate().clearColorFilter();
            }
        }
    }

    /**
     * Informiert den Nutzer, dass es sich bei seiner URL Eingabe nicht um eine gültige URL handelt.
     * Zeigt ein Hinweis in der Snackbar an und unterstreicht das TextFeld rot.
     * */
    private void noValidUrl() {
        if(getView() != null) {
            Snackbar.make(getView(), R.string.bitte_richtige_url, Snackbar.LENGTH_SHORT).show();
            changeVehicleUrlTextViewBottomColor(false);
        }
    }
}