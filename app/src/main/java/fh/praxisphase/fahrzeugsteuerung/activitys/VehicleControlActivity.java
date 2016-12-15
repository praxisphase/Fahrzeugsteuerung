package fh.praxisphase.fahrzeugsteuerung.activitys;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegInputStream;
import com.github.niqdev.mjpeg.MjpegView;

import fh.praxisphase.fahrzeugsteuerung.R;
import fh.praxisphase.fahrzeugsteuerung.utility.BundleConstants;
import fh.praxisphase.fahrzeugsteuerung.utility.SlideControl;
import fh.praxisphase.fahrzeugsteuerung.utility.VehicleCommunication;
import rx.functions.Action1;

/**
* Diese Activity enthält das Layout zum Steuern des Fahrzeuges
*/
public class VehicleControlActivity extends Activity {
    private final String TAG = "VehicleControlActivity";
    private String vehicleName;
    private String vehicleUrl;
    private String vehicleKey;
    private Handler sendValueHandler;

    private SlideControl steeringSlideControl;
    private SlideControl speedSlideControl;
    private ProgressDialog progressDialog;
    private boolean snackbarIsShown = false;
    private String snackbarMessage = "";
    private boolean connected;

    private int speedRange;
    private boolean invertSpeed;

    private int steeringRange;
    private boolean invertSteering;

    private int oldVehicleNameTextViewColor;

    TextView speedTextView;

    MjpegView mjpegView;

    private VehicleCommunication vehicleCommunication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vehicle_control_activity_layout);

        snackbarMessage = "";

        Log.d(TAG, "onCreate()");

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            parseBundle(bundle);
            initView();
            initVehicleCommunication();
        }
    }

    private void initVehicleCommunication(){
        vehicleCommunication = new VehicleCommunication(this, vehicleUrl, vehicleKey);
        checkVehicleConnection();
        vehicleCommunication.setReceiveSpeedFromVehicleListener(new VehicleCommunication.ReceiveSpeedFromVehicleListener() {
            @Override
            public void receivedSpeed(int speed) {
                Log.d(TAG, "receivedSpeed. "+speed);
                TextView speedTextView = (TextView) findViewById(R.id.vehicleControlActivityLayoutTextViewSpeed);

                speedTextView.setText(getApplicationContext().getString(R.string.geschwindigkeit)+" "
                        +String.valueOf(speed)+" "
                        +getApplicationContext().getString(R.string.kmh));
            }
        });

//        Reagiert auf Verbingunsprobleme und ändert bei Problemen die Schriftfarbe vom Fahrzeugnamen
        vehicleCommunication.setReceiveConnectionIssuesListener(new VehicleCommunication.ReceiveConnectionIssuesListener() {
            @Override
            public void receiveConnectionIssues(boolean issue) {
                Log.d(TAG, "receivedConnectionIssues: "+issue);
                if(issue){
                    changeVehicleNameTextViewColor(Color.YELLOW);
                } else if(!issue){
                    changeVehicleNameTextViewColor(Color.GREEN);
                }
            }
        });
        initSendData();
    }

    private void parseBundle(Bundle bundle){
        vehicleName = bundle.getString(BundleConstants.VEHICLE_NAME);
        vehicleUrl = bundle.getString(BundleConstants.VEHICLE_URL);
        vehicleKey = bundle.getString(BundleConstants.VEHICLE_KEY);

        speedRange = bundle.getInt(BundleConstants.VEHICLE_SPEED_RANGE);
        invertSpeed = bundle.getBoolean(BundleConstants.VEHICLE_INVERT_SPEED);

        steeringRange = bundle.getInt(BundleConstants.VEHICLE_STEERING_RANGE);
        invertSteering = bundle.getBoolean(BundleConstants.VEHICLE_INVERT_STEERING);
    }

    private void initView(){
        sendValueHandler = new Handler();
        oldVehicleNameTextViewColor = 0;

        TextView textViewVehicleName = (TextView) findViewById(R.id.vehicleControlActivityLayoutTextViewVehicleName);
        textViewVehicleName.setText(vehicleName);
        changeVehicleNameTextViewColor(Color.GREEN);

        speedTextView = (TextView) findViewById(R.id.vehicleControlActivityLayoutTextViewSpeed);
        speedTextView.setText(R.string.geschwindigkeit+"0"+R.string.kmh);

        initSpeedView();
        initSteeringView();
        initFloatingButton();
    }

    private void checkVehicleConnection(){
        showStartVehicleDialog();

        vehicleCommunication.setCheckVehicleConnectionListener(new VehicleCommunication.CheckVehicleConnectionListener() {
            @Override
            public void checkVehicleConnection(int connection) {
                switch (connection){
                    case VehicleCommunication.OK:
                        Log.d(TAG, "Alles Ok");
                        connected = true;
                        vehicleCommunication.setReceiveVideoURLListener(new VehicleCommunication.ReceiveVideoURLListener() {
                            @Override
                            public void receivedVideoURL(String videoURL) {
                                Log.d(TAG, "VideoURL: "+videoURL);
                                initMjpeg(videoURL);
                                sendData(0, 0);
                                sendValueHandler.post(sendValueRunnable);
                            }
                        });
                        vehicleCommunication.startVehicle();
                        vehicleCommunication.getVideoURL();
                        progressDialog.dismiss();
                        break;

                    case VehicleCommunication.CONNECTION_ERROR:
                        Log.d(TAG, "Keine Verbindung möglich");
                        connected = false;
                        snackbarMessage = "Keine Verbindung möglich.";
                        progressDialog.dismiss();
                        break;

                    case VehicleCommunication.NOT_AUTHORIZED:
                        Log.d(TAG, "Keine Berechtigung");
                        connected = false;
                        snackbarMessage = "Sie haben keine Berechtigung dieses Fahrzeug zu steuern.";
                        progressDialog.dismiss();
                        break;
                    default:
                        Log.d(TAG, "default");
                        progressDialog.dismiss();
                }
            }
        });

        vehicleCommunication.checkVehicleConnection();

    }

    /**
     * Informiert den Nutzer mit einem ProgressDialog darüber, dass eine Verbindung zum Fahrzeug hergestellt wird.
     * */
    private void showStartVehicleDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.verbindung_zum_fahrzeug_wird_hergestellt));
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "Progress Dialog dismiss");
                if(!connected) {
                    noConnection();
                }
            }
        });
        progressDialog.show();
    }

    /**
     * Initialisiert den FloatingButton.
     * Über den FloatingButton kann man ein neues Fahrzeug eintragen.
     * */
    private void initFloatingButton(){
        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.vehicleControlActivityLayoutFloatingButton);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);

                finish();
            }
        });
    }

    /**
     * Beendet die Activity und kehrt zur Fahrzeugauswahl zurück.
     * */
    private void noConnection(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if(!snackbarMessage.equals("")) {
            Bundle bundle = new Bundle();
            bundle.putString(BundleConstants.SNACKBAR_MESSAGE, snackbarMessage);
            intent.putExtras(bundle);
        }
        startActivity(intent);
        finish();
    }

    private void initSpeedView(){
        speedSlideControl = (SlideControl) findViewById(R.id.vehicleControlActivityLayoutSlideControlSpeed);
        speedSlideControl.setPositions(speedRange);
        speedSlideControl.invertSliderOutput(invertSpeed);
        speedSlideControl.resetSlider();
    }

    private void initSteeringView(){
        steeringSlideControl = (SlideControl) findViewById(R.id.vehicleControlActivityLayoutSlideControlSteering);
        steeringSlideControl.setPositions(steeringRange);
        steeringSlideControl.invertSliderOutput(invertSteering);
        steeringSlideControl.resetSlider();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopSendingData(false);
        vehicleCommunication.stopVehicle();
        vehicleCommunication.disconnectFromVehicle();
        vehicleCommunication.unregisterAllListener();

        if(mjpegView != null) {
            mjpegView.stopPlayback();
        }

        finish();
    }

    /**
     * Unterbindet das senden weiterer Datenpakete.
     *
     * @param showSnackbarNotification Gibt an ob ein Hinweis in der Snackbar angezeigt werden soll.
     * */
    private void stopSendingData(boolean showSnackbarNotification){
        sendValueHandler.removeCallbacks(sendValueRunnable);

        if(showSnackbarNotification && !snackbarIsShown && getCurrentFocus() != null) {
            snackbarIsShown = true;
            Snackbar.make(getCurrentFocus(), R.string.verbindung_zum_fahrzeug_abgebrochen, Snackbar.LENGTH_SHORT).show();
        }

        changeVehicleNameTextViewColor(Color.RED);
    }

    /**
     * Färbt den Fahrzeugnamen entweder in grün, gelb oder rot
     *
     * @param color die entsprechende Farbe.
     * */
    private void changeVehicleNameTextViewColor(int color){
        if(color != oldVehicleNameTextViewColor) {
            TextView textViewVehicleName = (TextView) findViewById(R.id.vehicleControlActivityLayoutTextViewVehicleName);
            textViewVehicleName.setTextColor(color);
            oldVehicleNameTextViewColor = color;
        }
    }

    /**
     * Sendet alle 20ms ein Datenpaket an das Fahrzeug.
     * */
    private Runnable sendValueRunnable = new Runnable() {
        @Override
        public void run() {
            int speed = speedSlideControl.getPosition();
            int steering = steeringSlideControl.getPosition();

            sendData(speed, steering);
            sendValueHandler.postDelayed(this, 20);
        }
    };

    private void initSendData(){
        vehicleCommunication.setSendDataToVehicleListener(new VehicleCommunication.SendDataToVehicleListener() {
            @Override
            public void sendDataToVehicle(int result) {
                switch (result){
                    case VehicleCommunication.OK:
                        Log.d(TAG, "Daten gesendet");

                        changeVehicleNameTextViewColor(Color.GREEN);
                        break;

                    case VehicleCommunication.CONNECTION_ISSUE:
                        Log.d(TAG, "Verbindungsprobleme");

                        changeVehicleNameTextViewColor(Color.YELLOW);
                        break;

                    case VehicleCommunication.NOT_AUTHORIZED:
                        stopSendingData(true);
                        Log.d(TAG, "Keine berechtigung");
                        break;
                }
            }
        });
    }

    /**
     * Sendet die Datenpakete an das Fahrzeug
     *
     *  @param speed Der Parameter für die Geschwindigkeit
     *  @param steering Der Parameter für die Lenkung
     **/
    private void sendData(int speed, int steering) {
        if(vehicleCommunication.isConnected()) {
            vehicleCommunication.sendDataToVehicle(String.valueOf(speed), String.valueOf(steering));
        } else{
            Log.d(TAG, "Verbindung zum Fahrzeug verloren");
            snackbarMessage = "Verbindung zum Fahrzeug verloren";

            noConnection();
        }
    }


    /**
     * Initialisiert den Kamera Stream
     * */
    private void initMjpeg(String videoURL) {
        mjpegView = (MjpegView) findViewById(R.id.vehicleControlActivityLayoutMjpegView);

        Mjpeg.newInstance()
                .open(videoURL, 5)
                .subscribe(new Action1<MjpegInputStream>() {
                               @Override
                               public void call(MjpegInputStream mjpegInputStream) {
                                   mjpegView.setSource(mjpegInputStream);
                                   mjpegView.setDisplayMode(DisplayMode.FULLSCREEN);
                               }
                           },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e(getClass().getSimpleName(), "mjpeg error", throwable);

                                snackbarMessage = "Konnte keine Verbindung zur Kamera herstellen";

                                noConnection();
                            }
                        });
    }
}