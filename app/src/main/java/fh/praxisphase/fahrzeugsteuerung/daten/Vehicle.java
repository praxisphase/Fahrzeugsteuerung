package fh.praxisphase.fahrzeugsteuerung.daten;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Vehicle beschreibt ein einzelnen Fahrzeug.
 */
public class Vehicle implements Comparable<Vehicle>{
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static final String TAG = "Vehicle";
    private String name;
    private String url;
    private String key;
    private int steeringSliderRange;
    private boolean invertSteeringSlider;
    private int speedSliderRange;
    private boolean invertSpeedSlider;


    /**
     * Erstellt ein neues Fahrzeug mit angepassten Werten für die Steurung.
     *
     * @param name Der Fahrzeugname
     * @param url Die URL vom Fahrzeug
     * @param key Der Key vom Fahrzeug als klartext
     * @param steeringSliderRange Wieviele Werte der Lenkungsregler in positive und negative
     *                            Richtung abdeckt
     * @param invertSteeringSlider Gibt an, ob der Lenkungsregler invertiert werden soll, also ein
     *                             schieben nach links eine Bewegung nach rechts auslöst.
     * @param speedSliderRange Wieviele Werte der Geschwindigkeitsregler in positive und negative
     *                            Richtung abdeckt
     * @param invertSpeedSlider Gibt an, ob der Geschwindigkeitsregler invertiert werden soll, also ein
     *                             schieben nach oben eine Rückwärtsbewegung beim Fahrzeug auslösen soll.
     * */
    public Vehicle(String name, String url, String key, int steeringSliderRange, boolean invertSteeringSlider, int speedSliderRange, boolean invertSpeedSlider){
        this.name = name;
        this.url = url;
        this.setKey(key);
        this.steeringSliderRange = steeringSliderRange;
        this.invertSteeringSlider = invertSteeringSlider;
        this.speedSliderRange = speedSliderRange;
        this.invertSpeedSlider = invertSpeedSlider;
    }

    /**
     * Erstellt ein neues Fahrzeug mit Default Werten für die Steuerung.
     *
     * @param name Der Fahrzeugname
     * @param url Die URL vom Fahrzeug
     * @param key Der Key vom Fahrzeug als klartext
     * */
    public Vehicle(String name, String url, String key){
        this(name, url, key, 25, false, 25, false);
    }

    /**
     * Ändert den Fahrzeugnamen
     *
     * @param name Neuer Fahrzeugname
     * */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Gibt den Fahrzeugnamen aus
     *
     * @return Fahrzeugname
     * */
    public String getName(){
        return this.name;
    }

    /**
     * Ändert die Adresse vom Fahrzeug
     *
     * @param url Die URL vom Fahrzeug
     * */
    public void setUrl(String url){
        this.url = url;
    }

    /**
     * Gibt die Adresse vom Fahrzeug aus.
     *
     * @return Die URL vom Fahrzeug
     * */
    public String getUrl(){
        return this.url;
    }

    /**
     * Setzt den Fahrzeug-Key.
     * Der Fahrzeug-Key muss beim Fahrzeug und in der App gleich sein.
     *
     * @param vehicleKey Fahrzeug-Key als klartext
     * */
    public void setKey(String vehicleKey){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] inputString = (vehicleKey).getBytes("UTF-8");
            byte[] output = messageDigest.digest(inputString);

            StringBuilder stringBuilder = new StringBuilder();
            for(byte b : output){
                stringBuilder.append(String.format("%02x", b));
            }

            key = stringBuilder.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Fahrzeug-Key als Hash
     * */
    public String getKey(){
        return this.key;
    }

    @Override
    public int compareTo(@NonNull Vehicle another) {
        return this.name.toLowerCase().compareTo(another.getName().toLowerCase());
    }

    public int getSteeringSliderRange() {
        return steeringSliderRange;
    }

    public void setSteeringSliderRange(int steeringSliderRange) {
        this.steeringSliderRange = steeringSliderRange;
    }

    public boolean isInvertSteeringSlider() {
        return invertSteeringSlider;
    }

    public void setInvertSteeringSlider(boolean invertSteeringSlider) {
        this.invertSteeringSlider = invertSteeringSlider;
    }

    public int getSpeedSliderRange() {
        return speedSliderRange;
    }

    public void setSpeedSliderRange(int speedSliderRange) {
        this.speedSliderRange = speedSliderRange;
    }

    public boolean isInvertSpeedSlider() {
        return invertSpeedSlider;
    }

    public void setInvertSpeedSlider(boolean invertSpeedSlider) {
        this.invertSpeedSlider = invertSpeedSlider;
    }
}