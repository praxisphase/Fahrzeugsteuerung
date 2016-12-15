package fh.praxisphase.fahrzeugsteuerung.daten;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;


/**
 * VehicleContainer beinhaltet eine Liste mit allen Fahrzeugen
 */
public class VehicleContainer {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static final String TAG = "vehicleContainer";
    private ArrayList<Vehicle> vehicles;

    /**
     * Erstellt eine leere Fahrzeugliste
     * */
    public VehicleContainer(){
        vehicles = new ArrayList<>();
    }

    /**
     * Erstellt eine neue Fahrzeugliste aus einem Json String
     *
     * @param vehicleStringList ein Json Object als String
     * */
    public VehicleContainer(String vehicleStringList){
        if(vehicleStringList != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Vehicle>>() {
            }.getType();
            vehicles = gson.fromJson(vehicleStringList, type);
        } else{
            vehicles = new ArrayList<>();
        }
    }

    /**
     * Gibt ein bestimmtes Fahrzeug aus.
     *
     * @param position Position vom Fahrzeug in der Fahrzeugliste
     *
     * @return Das Fahrzeug oder null, wenn kein Fahrzeug gefunden wurde
     * */
    public Vehicle getVehicle(int position){
        if(position < vehicles.size() && position >= 0){
            return vehicles.get(position);
        }

        return null;
    }

    /**
     * Erstellt aus der Fahrzeugliste ein Json Object und gibt dieses als String aus
     *
     * @return Fahrzeugliste als Json String
     * */
    public String toJsonString(){
        Gson gson = new Gson();

        return gson.toJson(vehicles);
    }

    /**
     * Gibt die Anzahl an Fahrzeugen in der Fahrzeugliste aus.
     *
     * @return Anzahl Fahrzeuge in der Fahrzeugliste
     * */
    public int size(){
        return vehicles.size();
    }

    /**
     * Fügt ein Fahrzeug zu fahrzeugliste hinzu.
     *
     * @param vehicle Das neue Fahrzeug
     * */
    public void add(Vehicle vehicle){
        vehicles.add(vehicle);
    }

    /**
     * Prüft, ob es ein Fahrzeug mit dem Namen in der Fahrzeugliste gibt.
     *
     * @param vehicleName Der zu suchende Fahrzeugname
     *
     * @return  true wenn es den Namen schon gibt, ansonsten false
     * */
    public boolean contains(String vehicleName) {
        for (Vehicle vehicle : vehicles) {
            if(vehicle.getName().equals(vehicleName))
                return true;
        }
        return false;
    }

    /**
    * Gibt die Position von einem Fahrzeug innerhalb der Fahrzeugliste an.
     *
     * @param vehicleName Der fahrzeugname
     *
     * @return Die Position, oder -1 wenn es kein Fahrzeug mit diesem Namen gibt.
     * */
    public int getPosition(String vehicleName){
        for(int i = 0; i < vehicles.size(); i++) {
            if(vehicles.get(i).getName().equals(vehicleName)){
                return i;
            }
        }

        return -1;
    }

    /**
     * Gibt eine Fahrzeugliste mit Fahrzeugen aus, die die Zeichenkette enthalten
     *
     * @param filter zu suchende Zeichenkette
     *
     * @return gibt die gefundenen Fahrzeuge als Fahrzeugliste zurück
     * */
    public VehicleContainer filter(String filter){
        VehicleContainer newVehicleContainer = new VehicleContainer();

        for (Vehicle vehicle : vehicles) {
            if(vehicle.getName().toLowerCase().contains(filter.toLowerCase())){
                newVehicleContainer.add(vehicle);
            }
        }

        return newVehicleContainer;
    }

    /**
     * Fügt ein Fahrzeug an einer bestimmten Position zur Liste hinzu.
     *
     * @param position Die Stelle an der das Fahrzeug eingefügt werden soll.
     * @param vehicle Das einzufügende Fahrzeug.*/
    public void add(int position, Vehicle vehicle){
        vehicles.add(position, vehicle);
    }

    /**
     * Sortiert die Fahrzeugliste anhand der Fahrzeugnamen.
     * */
    public void sortByName(){
        Collections.sort(vehicles);
    }

    /**
     * Löscht ein bestimmtes Fahrzeug aus der Fahrzeugliste.
     *
     * @param position Die Position vom Fahrzeug
     * */
    public void delete(int position){
        vehicles.remove(position);
    }
}