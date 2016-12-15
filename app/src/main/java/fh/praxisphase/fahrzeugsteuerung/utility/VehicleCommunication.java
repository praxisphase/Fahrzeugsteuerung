package fh.praxisphase.fahrzeugsteuerung.utility;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Diese Klasse bietet Methoden für den Verbindungsaufbau und Datenaustausch zwischen Fahrzeug und App.
 * */
public class VehicleCommunication {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "VehicleCommunication";
    private static final int CHECK = 0;
    private static final int DATA = 1;
    private static final int SPEED = 2;
    private static final int VIDEO = 3;
    private static final int CONNECTION_ISSUES = 4;

    public static final int OK = 200;
    public static final int NOT_AUTHORIZED = 401;
    public static final int CONNECTION_ERROR = 400;
    public static final int CONNECTION_ISSUE = 404;

    private static final int CONNECT_TIMEOUT = 600;

    private Socket socket;
    private Handler timeoutHandler;
    private CheckVehicleConnectionListener checkVehicleConnectionListener;
    private SendDataToVehicleListener sendDataToVehicleListener;
    private ReceiveSpeedFromVehicleListener receiveSpeedFromVehicleListener;
    private ReceiveVideoURLListener receiveVideoURLListener;
    private ReceiveConnectionIssuesListener receiveConnectionIssuesListener;
    private boolean unregisterListener;
    private Context context;
    private String vehicleURL;
    private String vehicleKey;

    public VehicleCommunication(Context context, String vehicleURL, String vehicleKey){
        if(!vehicleURL.startsWith("http://")){
            vehicleURL = "http://"+vehicleURL;
        }

        this.vehicleURL = vehicleURL;
        this.vehicleKey = vehicleKey;
        this.context = context;
        unregisterListener = false;
    }

    /**
     * Prüft, ob eine Verbindung zum Fahrzeug besteht und ob der Nutzer das Fahrzeug steuern darf.
     * Das Ergebniss erhält man über den CheckVehicleConnectionListener
     * */
    public  void checkVehicleConnection(){
        if(socket == null){
            connectToVehicle();
        } else{
            if(!socket.connected()){
                connectToVehicle();
            } else{
                checkAuthorization();
            }
        }
    }

    /**
     * Ruft die registrierten Listener nicht mehr auf.
     * */
    public void unregisterAllListener(){
        unregisterListener = true;
    }

    public void setSendDataToVehicleListener(SendDataToVehicleListener sendDataToVehicleListener){
        this.sendDataToVehicleListener = sendDataToVehicleListener;
    }

    public void setCheckVehicleConnectionListener(CheckVehicleConnectionListener checkVehicleConnectionListener){
        this.checkVehicleConnectionListener = checkVehicleConnectionListener;
    }

    public void setReceiveSpeedFromVehicleListener(ReceiveSpeedFromVehicleListener receiveSpeedFromVehicleListener){
        this.receiveSpeedFromVehicleListener = receiveSpeedFromVehicleListener;
    }

    public void setReceiveVideoURLListener(ReceiveVideoURLListener receiveVideoURLListener){
        this.receiveVideoURLListener = receiveVideoURLListener;
    }

    public void setReceiveConnectionIssuesListener(ReceiveConnectionIssuesListener receiveConnectionIssuesListener){
        this.receiveConnectionIssuesListener = receiveConnectionIssuesListener;
    }

    /**
     * Prüft, ob der Nutzer berechtigt ist dieses Fahrzeug zu steuern.
     * */
    private void checkAuthorization() {
        String message = buildMessage(String.valueOf(0), String.valueOf(0));

        socket.emit("checkAuthorization", new String[]{message, hashMessage(message, vehicleKey)}, checkAuthorizationAck);
    }

    /**
     * Sendet Steuerdaten an das Fahrzeug.
     * */
    public void sendDataToVehicle(String speed, String steering){
        String message = buildMessage(speed, steering);

        socket.emit("data", new String[]{message, hashMessage(message, vehicleKey)}, dataToVehicleAck);
    }

    public void getVideoURL(){
        socket.emit("getVideoURL", null, getVideoURLAck);
    }

    /**
     * Stellt eine Verbindung zum Fahrzeug her.
     * */
    private void connectToVehicle(){
        timeoutHandler = new Handler();
        try {
            socket = IO.socket(vehicleURL);
            socket.on(Socket.EVENT_ERROR, errorListener);
            socket.on(Socket.EVENT_CONNECT, connectedListener);
            socket.on("speed", speedChange);
            socket.on("connectionIssue", connectionIssueListener);
            socket.connect();
            timeoutHandler.postDelayed(timeoutRunnable, CONNECT_TIMEOUT);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gibt an ob die App mit dem Fahrzeug verbunden ist.
     * */
    public boolean isConnected(){
        return socket.connected();
    }

    /**
     * Trennt die Verbindung zum Fahrzeug.
     * */
    public void disconnectFromVehicle(){
        socket.disconnect();
        socket.off();
    }

    /**
     * Stellt erneut eine Verbindung zum Fahrzeug her.
     * */
    public void reconnectToVehicle(){
        if(socket != null){
            if(!socket.connected()) {
                socket.connect();
            }
        } else{
            connectToVehicle();
        }
    }

    public void startVehicle(){
        if(socket != null){
            socket.emit("startVehicle");
        }
    }

    /**
     * Runnable wird ausgeführt, wenn der Verbindungsaufbau zu lange gedauert hat.
     * */
    private Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("VehicleCommunication", "timeout");
            socket.off();
            socket.close();
            onPostExecute(400, CHECK);
        }
    };

    private Emitter.Listener errorListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("VehicleCommunication", "error");
            socket.off();
            socket.close();
            onPostExecute(400, CHECK);
        }
    };

    private Emitter.Listener connectedListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d("VehicleCommunication", "connected");
            timeoutHandler.removeCallbacks(timeoutRunnable);
            checkAuthorization();
        }
    };

    private Ack checkAuthorizationAck = new Ack() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "checkAuthorizationAck: "+args[0]);
            onPostExecute(args[0], CHECK);
        }
    };

    private Ack dataToVehicleAck = new Ack() {
        @Override
        public void call(Object... args) {
            onPostExecute(args[0], DATA);
        }
    };

    private Ack getVideoURLAck = new Ack() {
        @Override
        public void call(Object... args) {
            onPostExecute(args[0], VIDEO);
        }
    };

    /**
     * Reagiert darauf, wenn das Fahrzeug eine neue Geschwindigkeit sendet.
     * */
    private Emitter.Listener speedChange = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "speedChange");
            onPostExecute(args[0], SPEED);
        }
    };

    /**
     * Signalisiert wie der Status der Kommunikation ist
     * */
    private Emitter.Listener connectionIssueListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(TAG, "connection issues");
            onPostExecute(args[0], CONNECTION_ISSUES);
        }
    };

    /**
     * Sendet das Resultat an den UI Thread.
     *
     * @param result Das Resultat der Anfrage
     * @param type Der Type der Aktion
     *             CHECK wenn es sich um die Prüfung der  Verbindung/Authorization handelt
     *             DATA wenn daten ans Fahrzeug gesendet wurden
     *             SPEED wenn das Fahrzeug eine neu gemessene Geschwindigkeit gesendet hat
     * */
    private void onPostExecute(final Object result, final int type){
        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable postToUiThread = new Runnable() {
            @Override
            public void run() {
                if(!unregisterListener) {
                    switch (type) {
                        case CHECK:
                            if (checkVehicleConnectionListener != null) {
                                checkVehicleConnectionListener.checkVehicleConnection((int) result);
                            }
                            break;

                        case DATA:
                            if (sendDataToVehicleListener != null) {
                                sendDataToVehicleListener.sendDataToVehicle((int) result);
                            }
                            break;

                        case SPEED:
                            if (receiveSpeedFromVehicleListener != null) {
                                Log.d(TAG, "speed: "+result);
                                receiveSpeedFromVehicleListener.receivedSpeed((int) result);
                            }
                            break;

                        case VIDEO:
                            if (receiveVideoURLListener != null) {
                                receiveVideoURLListener.receivedVideoURL((String) result);
                            }
                            break;

                        case CONNECTION_ISSUES:
                            if(receiveConnectionIssuesListener != null){
                                Log.d(TAG, "ConnectionIssue: "+result);
                                receiveConnectionIssuesListener.receiveConnectionIssues((Boolean) result);
                            }
                            break;
                    }
                }
            }
        };

        mainHandler.post(postToUiThread);
    }

    /**
     * Erstellt das Datenpaket mit den Steuerbefehlen
     *
     * @param speed Der Parameter für die Reglerstellung für die Geschwindgkeit
     * @param steering Der Parameter für die Reglerstellung für die Lenkung
     *
     * @return Das fertige Datenpaket
     * */
    private String buildMessage(String speed, String steering){
        JSONObject jsonObject = new JSONObject();
        Calendar timestamp = Calendar.getInstance();

        try {
            jsonObject.put("speed", speed);
            jsonObject.put("steering", steering);
            jsonObject.put("time", ""+timestamp.getTimeInMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }

    /**
     * Erstellt aus der kombination aus Steuerbefehlen und VehicleKey einen Hash
     *
     * @param dataMessage Die Steuerbefehle
     * @param vehicleKey Der Fahrzeug-Key
     *
     * @return Der fertige Hash
     * */
    private String hashMessage(String dataMessage, String vehicleKey){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] inputString = (dataMessage+vehicleKey).getBytes("UTF-8");
            byte[] output = messageDigest.digest(inputString);

            StringBuilder stringBuilder = new StringBuilder();
            for(byte b : output){
                stringBuilder.append(String.format("%02x", b));
            }

            return stringBuilder.toString();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Registriert die alten Listener erneut.
     * */
    public void reregisterAllListener() {
        unregisterListener = false;
    }

    public void stopVehicle() {
        if(socket != null){
            socket.emit("stopVehicle");
        }
    }

    public interface SendDataToVehicleListener{
        void sendDataToVehicle(int result);
    }

    public interface CheckVehicleConnectionListener{
        void checkVehicleConnection(int connection);
    }

    public interface ReceiveSpeedFromVehicleListener {
        void receivedSpeed(int speed);
    }

    public interface ReceiveVideoURLListener {
        void receivedVideoURL(String videoURL);
    }

    public interface ReceiveConnectionIssuesListener{
        void receiveConnectionIssues(boolean issue);
    }
}