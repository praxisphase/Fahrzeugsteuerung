package fh.praxisphase.fahrzeugsteuerung.daten;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import fh.praxisphase.fahrzeugsteuerung.R;
//import fh.praxisphase.fahrzeugsteuerung.utility.Network;
import fh.praxisphase.fahrzeugsteuerung.utility.VehicleCommunication;


/**
 * VehicleAdapter ist ein RecyclerView.Adapter zur Ausgabe der vorhandenen Fahrzeuge
 * */
public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static final String TAG = "VehicleAdapter";
    private VehicleContainer vehicles;
    private Context context;
    private String onMenuClickId = null;
    private VehicleAdapterAction vehicleAdapterAction;

    /**
     * Erstellt einen neuen VehicleAdapter
     *
     * @param vehicleContainer Die Fahrzeuglsite
     * @param context Der zugehörige Context
     * */
    public VehicleAdapter(VehicleContainer vehicleContainer, Context context){
        this.vehicles = vehicleContainer;
        this.context = context;
    }

    @Override
    public void onClick(View v) {
//        Zeigt das Auswahlmenü zum Bearbeiten und Löschen von Fahrzeugen an
        if(R.id.recyclerViewButtonItemContext == v.getId()){
            onMenuClickId = String.valueOf(v.getTag());
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.setOnMenuItemClickListener(this);
            popup.setOnDismissListener(this);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.actions, popup.getMenu());
            popup.show();
        } else{
            if(vehicleAdapterAction != null){
                vehicleAdapterAction.onItemClickListener((String)v.getTag());
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(onMenuClickId != null && vehicleAdapterAction != null) {
            switch (item.getItemId()) {
                case R.id.edit:
                    vehicleAdapterAction.onEditItemClickListener(onMenuClickId);
                    break;

                case R.id.delete:
                    vehicleAdapterAction.onDeleteItemClickListener(onMenuClickId);
                    break;
            }
        }
        onMenuClickId = null;
        return false;
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        onMenuClickId = null;
    }

    /**
     * Löscht Fahrzeuge aus der RecyclerView und/oder fügt Fahrzeuge zur RecyclerView hinzu.
     *
     * @param filteredVehicleContainer Eine Fahrzeugliste die durch ein Kriterium gefiltert wurde
     * */
    public void filter(VehicleContainer filteredVehicleContainer){
        removeItem(filteredVehicleContainer);
        addItem(filteredVehicleContainer);
        move(filteredVehicleContainer);
    }

    /**
     * Löscht Fahrzeuge aus dem RecyclerView, die nicht mehr in der gefilterten Fahrzeugliste enthalten sind
     *
     * @param filteredVehicleContainer Die gefilterte Fahrzeugliste*/
    private void removeItem(VehicleContainer filteredVehicleContainer){
        for(int i = vehicles.size()-1; i >= 0; i--){
            if(filteredVehicleContainer.getPosition(vehicles.getVehicle(i).getName()) == -1){
                vehicles.delete(i);
                notifyItemRemoved(i);
            }
        }
    }

    /**
     * Fügt Fahrzeuge zum RecyclerView hinzu, die durch das Filtern der Fahzeugliste hinzugefügt wurden
     *
     * @param filteredVehicleContainer Die gefilterte Fahrzeugliste*/
    private void addItem(VehicleContainer filteredVehicleContainer){
        for(int i = 0; i < filteredVehicleContainer.size(); i++){
            if(!vehicles.contains(filteredVehicleContainer.getVehicle(i).getName())){
                vehicles.add(filteredVehicleContainer.getVehicle(i));
                notifyItemInserted(vehicles.getPosition(filteredVehicleContainer.getVehicle(i).getName()));
            }
        }
    }

    /**
     * Ordnet die Auflistung im RecyclerView anhand der gefilterten Fahrzeugliste neu
     *
     * @param filteredVehicleContainer Die gefilterte Fahrzeugliste*/
    private void move(VehicleContainer filteredVehicleContainer){
        for(int i=0; i < filteredVehicleContainer.size(); i++){
            if(!filteredVehicleContainer.getVehicle(i).getName().equals(vehicles.getVehicle(i).getName())){
                int oldPosition = vehicles.getPosition(filteredVehicleContainer.getVehicle(i).getName());
                vehicles.delete(oldPosition);
                vehicles.add(i, filteredVehicleContainer.getVehicle(i));
                notifyItemMoved(oldPosition, i);
            }
        }
    }

    @Override
    public VehicleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if(vehicles.size() > position) {
            Vehicle vehicle = vehicles.getVehicle(position);

            holder.vehicleName.setText(vehicle.getName());
            holder.vehicleUrl.setText(vehicle.getUrl());


            final VehicleCommunication vehicleCommunication = new VehicleCommunication(context, vehicle.getUrl(), vehicle.getKey());

            vehicleCommunication.setCheckVehicleConnectionListener(new VehicleCommunication.CheckVehicleConnectionListener() {
                @Override
                public void checkVehicleConnection(int connection) {
                    switch(connection){
                        case VehicleCommunication.OK:
                            Log.d(TAG, "ok");
                            holder.vehicleUrl.setTextColor(Color.GREEN);
                            break;

                        case VehicleCommunication.CONNECTION_ERROR:
                            Log.d(TAG, "No Connection");
                            holder.vehicleUrl.setTextColor(Color.RED);
                            break;

                        case VehicleCommunication.NOT_AUTHORIZED:
                            Log.d(TAG, "Not Authorized");
                            holder.vehicleUrl.setTextColor(Color.YELLOW);
                            break;
                    }

                    vehicleCommunication.disconnectFromVehicle();
                }
            });

            vehicleCommunication.checkVehicleConnection();

            holder.imageButton.setTag(vehicle.getName());
            holder.imageButton.setOnClickListener(this);
            holder.imageButton.setVisibility(View.VISIBLE);

            holder.item.setTag(vehicle.getName());
            holder.item.setOnClickListener(this);
        } else{
//            Füge ein leeres Item am Ende vom RecyclerView ein, damit der FloatingButton nicht den
//            letzten Eintrag verdeckt.
            holder.vehicleName.setText("");
            holder.vehicleUrl.setText("");
            holder.address.setText("");
            holder.imageButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
//        Füge eine leere Zeile ein, damit der FloatingButton den letzten Eintrag nicht überdeckt
        return vehicles.size()+1;
    }

    /**
     * Setzt einen Callback für Aktionen die an einem Item passieren.
     *
     * @param vehicleAdapterAction Die aufzurufende Methode
     * */
    public void setOnVehicleAdapterAction(VehicleAdapterAction vehicleAdapterAction){
        this.vehicleAdapterAction = vehicleAdapterAction;
    }

    /**
     * VehicleAdapterAction beinhaltet Listener für Item Click Aktionen
     * */
    public interface VehicleAdapterAction {

        void onItemClickListener(String vehicleName);
        void onEditItemClickListener(String vehicleName);
        void onDeleteItemClickListener(String vehicleName);
    }


    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView vehicleName;
        TextView vehicleUrl;
        TextView address;
        ImageButton imageButton;
        View item;

        ViewHolder(View itemView) {
            super(itemView);

            item = itemView;

            vehicleName = (TextView) itemView.findViewById(R.id.recyclerViewItemTextViewVehicleName);
            vehicleUrl = (TextView) itemView.findViewById(R.id.recyclerViewItemTextViewVehicleUrl);
            address = (TextView) itemView.findViewById(R.id.recyclerViewItemTextViewAddress);
            imageButton = (ImageButton) itemView.findViewById(R.id.recyclerViewButtonItemContext);
        }
    }
}
