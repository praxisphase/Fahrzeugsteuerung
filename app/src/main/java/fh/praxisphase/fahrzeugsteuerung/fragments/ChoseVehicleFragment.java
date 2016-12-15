package fh.praxisphase.fahrzeugsteuerung.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import fh.praxisphase.fahrzeugsteuerung.daten.Vehicle;
import fh.praxisphase.fahrzeugsteuerung.daten.VehicleAdapter;
import fh.praxisphase.fahrzeugsteuerung.daten.VehicleContainer;
import fh.praxisphase.fahrzeugsteuerung.utility.BundleConstants;
import fh.praxisphase.fahrzeugsteuerung.utility.ItemDivider;
import fh.praxisphase.fahrzeugsteuerung.utility.MySharedPreferences;
import fh.praxisphase.fahrzeugsteuerung.R;
import fh.praxisphase.fahrzeugsteuerung.activitys.VehicleControlActivity;
import fh.praxisphase.fahrzeugsteuerung.utility.VehicleCommunication;

/**
 * Das Fragment zeigt die Fahrzeugliste an.
 * Ab einer bestimmten Anzahl an Fahrzeugen wird eine Suche angezeigt.
 * */
public class ChoseVehicleFragment extends Fragment implements View.OnClickListener, VehicleAdapter.VehicleAdapterAction, TextView.OnEditorActionListener {
    private static final int FRAGMENT_CODE = 123456;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "ChoseVehicleFrag";
    @SuppressWarnings("FieldCanBeLocal")
    private final int numberOfVehicleToShowSearch = 10;
    private VehicleContainer vehicleContainer;
    private RecyclerView recyclerView;
    private VehicleAdapter adapter;
    private View view;
    private int oldFilterCount;
    private VehicleContainer filteredVehicle;
    private ProgressDialog progressDialog;
    private static boolean showSnackbar = true;
    private String snackbarMessage = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.chose_vehicle_fragment_layout, container, false);

        if(getArguments() != null){
            snackbarMessage = getArguments().getString(BundleConstants.SNACKBAR_MESSAGE, "");
        }

        init();
        checkShowSearchView();

        return view;
    }

    private void init() {
        vehicleContainer = getVehicleContainer();
        vehicleContainer.sortByName();
        filteredVehicle = getVehicleContainer();
        filteredVehicle.sortByName();
        oldFilterCount = 0;

        initRecyclerView();
        initFloatingButton();

        if(!snackbarMessage.equals("") && showSnackbar){
            Snackbar.make(view, snackbarMessage, Snackbar.LENGTH_SHORT).show();
            snackbarMessage = "";
            showSnackbar = false;
        }
    }

    private void initFloatingButton() {
        FloatingActionButton addButton = (FloatingActionButton) view.findViewById(R.id.choseVehicleFragmentLayoutFloatingButton);
        addButton.setOnClickListener(this);
    }

    private void initRecyclerView() {
        recyclerView = (RecyclerView) view.findViewById(R.id.choseVehicleFragmentLayoutRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RecyclerView.ItemDecoration itemDecoration;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            itemDecoration = new ItemDivider(getResources().getDrawable(R.drawable.divider, null));
        } else{
            //noinspection deprecation
            itemDecoration = new ItemDivider(getResources().getDrawable(R.drawable.divider));
        }

        recyclerView.addItemDecoration(itemDecoration);

        adapter = new VehicleAdapter(vehicleContainer, getContext());
        adapter.setOnVehicleAdapterAction(this);

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
//        Zeigt Fragment für neues Fahrzeug bei Klick auf FloatingButton an
        AddVehicleFragment addVehicleFragment = new AddVehicleFragment();

        addVehicleFragment.setTargetFragment(this, FRAGMENT_CODE);
        
        Bundle bundle = new Bundle();
        bundle.putBoolean(BundleConstants.FLOATING_BUTTON_CALL, true);

        addVehicleFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(R.id.mainActivityLayoutFragmentContainer, addVehicleFragment).addToBackStack("choseVehicleFragment").commit();
    }

    @Override
    public void onItemClickListener(String vehicleName) {
/*        Prüft, ob eine Verbindung zum ausgewähltem Fahrzeug besteht und startet bei bestehender Verbindung
        die VehicleControlActivity. Besteht keine Verbindung zum Fahrzeug, so wird der Nutzer darüber informiert.*/

        showStartVehicleDialog();
        showSnackbar = true;

        int vehiclePosition = vehicleContainer.getPosition(vehicleName);
        progressDialog.dismiss();
        Bundle bundle = new Bundle();
        bundle.putString(BundleConstants.VEHICLE_NAME, vehicleContainer.getVehicle(vehiclePosition).getName());
        bundle.putString(BundleConstants.VEHICLE_URL, vehicleContainer.getVehicle(vehiclePosition).getUrl());
        bundle.putString(BundleConstants.VEHICLE_KEY, vehicleContainer.getVehicle(vehiclePosition).getKey());

        bundle.putInt(BundleConstants.VEHICLE_SPEED_RANGE, vehicleContainer.getVehicle(vehiclePosition).getSpeedSliderRange());
        bundle.putBoolean(BundleConstants.VEHICLE_INVERT_SPEED, vehicleContainer.getVehicle(vehiclePosition).isInvertSpeedSlider());

        bundle.putInt(BundleConstants.VEHICLE_STEERING_RANGE, vehicleContainer.getVehicle(vehiclePosition).getSteeringSliderRange());
        bundle.putBoolean(BundleConstants.VEHICLE_INVERT_STEERING, vehicleContainer.getVehicle(vehiclePosition).isInvertSteeringSlider());

        Intent intent = new Intent(getContext(), VehicleControlActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void showStartVehicleDialog() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getResources().getString(R.string.verbindung_zum_fahrzeug_wird_hergestellt));
        progressDialog.show();
    }

    @Override
    public void onEditItemClickListener(String vehicleName) {
//        Wechselt zum AddVehicleFragment um das ausgewählte Fahrzeug zu bearbeiten
        Bundle bundle = new Bundle();
        bundle.putString(BundleConstants.VEHICLE_NAME, vehicleName);
        bundle.putBoolean(BundleConstants.EDIT_VEHICLE, true);

        AddVehicleFragment addVehicleFragment = new AddVehicleFragment();

        addVehicleFragment.setTargetFragment(this, FRAGMENT_CODE);

        addVehicleFragment.setArguments(bundle);
        getFragmentManager().beginTransaction().replace(R.id.mainActivityLayoutFragmentContainer, addVehicleFragment).addToBackStack(null).commit();
    }

    @Override
    public void onDeleteItemClickListener(String vehicleName) {
        int position = vehicleContainer.getPosition(vehicleName);

        showSnackbarDeleteVehicle(vehicleContainer.getVehicle(position));

        vehicleContainer.delete(position);

        MySharedPreferences mySharedPreferences = new MySharedPreferences(getContext());
        mySharedPreferences.saveVehicleContainer(vehicleContainer.toJsonString());

        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, adapter.getItemCount()-(position+1));

        checkShowSearchView();
    }

    /**
     * Zeigt die Snackbar beim löschen von einem Fahrzeug an.
     *
     *  @param deletedVehicle das gelöschte Fahrzeug
     * */
    private void showSnackbarDeleteVehicle(final Vehicle deletedVehicle) {
        Snackbar.make(view, R.string.fahrzeug_geloescht, Snackbar.LENGTH_SHORT)
                .setAction(R.string.rueckgaengig, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undo(deletedVehicle);
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        Log.d(TAG, "dismiss Snackbar");
                        if(vehicleContainer.size() == 0 && getFragmentManager() != null) {
                            AddVehicleFragment addVehicleFragment = new AddVehicleFragment();
                            Bundle bundle = new Bundle();
                            bundle.putBoolean(BundleConstants.NEW_VEHICLE, true);
                            addVehicleFragment.setArguments(bundle);
                            getFragmentManager().beginTransaction().replace(R.id.mainActivityLayoutFragmentContainer, addVehicleFragment).commit();
                        }
                    }
                })
                .show();
    }

    /**
     * Stellt das gelöschte Fahrzeug wieder her
     *
     * @param deletedVehicle das gelöschte Fahrzeug
     * */
    private void undo(Vehicle deletedVehicle) {
        vehicleContainer.add(deletedVehicle);
        vehicleContainer.sortByName();

        MySharedPreferences mySharedPreferences = new MySharedPreferences(getContext());
        mySharedPreferences.saveVehicleContainer(vehicleContainer.toJsonString());

        int position = vehicleContainer.getPosition(deletedVehicle.getName());

        adapter.notifyItemInserted(position);
        adapter.notifyItemRangeChanged(position, adapter.getItemCount()-(position+1));

        checkShowSearchView();
    }

    /**
     * Prüft, ob genung Fahrzeuge in der Liste sind um die Suche anzuzeigen und blendet diese bei Bedarf ein.
     * */
    private void checkShowSearchView() {
        EditText editText = (EditText) view.findViewById(R.id.choseVehicleFragmentLayoutEditTextVehicleSearch);
        if(vehicleContainer.size() > numberOfVehicleToShowSearch){
            editText.setVisibility(View.VISIBLE);
            hideKeyboard(editText);

            editText.addTextChangedListener(new myTextWatcher());
            editText.setOnEditorActionListener(this);
        } else{
            editText.setVisibility(View.GONE);
        }
    }

    /**
     * Blendet das Keyboard aus
     *
     * @param editText der View auf das sich das Keyboard bezieht
     * */
    private void hideKeyboard(View editText) {
        editText.clearFocus();
        recyclerView.requestFocus();
        InputMethodManager in = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_ACTION_SEARCH){
            hideKeyboard(v);
            return true;
        }

        return false;
    }

    /**
     * Lädt die gespeicherte Fahrzeugliste
     *
     * @return die gespeicherte Fahrzeugliste
     * */
    private VehicleContainer getVehicleContainer(){
        MySharedPreferences mySharedPreferences = new MySharedPreferences(getContext());
        return new VehicleContainer(mySharedPreferences.getVehicleContainer());
    }

    /**
     * Diese Klasse beobachtet die Eingaben ins Suchfeld und gibt eine entsprechend gefilterte
     * Fahrzeugliste im RecyclerView aus.
     * */
    private class myTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(count < oldFilterCount){
                oldFilterCount = count;
                filteredVehicle = getVehicleContainer().filter(s.toString());
            } else{
                oldFilterCount = count;
                filteredVehicle = filteredVehicle.filter(s.toString());
            }

            filteredVehicle.sortByName();

            recyclerView.scrollToPosition(0);
            adapter.filter(filteredVehicle);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}