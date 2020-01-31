package com.example.src;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.src.controller.EvilCarAPI;
import com.example.src.controller.LocationController;
import com.example.src.controller.NetworkClient;
import com.example.src.model.SpeedPojo;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {
    private static int CAR_ID = 101 ;
    private static double ACCELERATION_THRESHOLD = 0.05*9.8;
    LocationController locationController;
    private Double speedLimit;
    double longitude,latitude;
    double endingLongitude,endingLatitude;
    private boolean sentViolationOneTime = false;
    private SensorManager sensorManager;
    private Sensor accelerationSensor;
    private float[] accelerationValues;
    private float[] gravityValues;
    private SensorEventListener sensorEventListener;
    private boolean acsLong,acsLat;
    private TextView txt;
    private ScrollView scrollView;
    private String serverBaseURL = "http://192.168.1.8:5000";
    private int accelerationCount = 0;
    private static final int ACCELERATION_COUNT_THRESHOLD = 3;
    private TextView speedLimitTextView;
    private TextView violationPostedTextView;
    private Boolean insideViolationFunction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = findViewById(R.id.log);
        scrollView = findViewById(R.id.sv);
        speedLimitTextView = findViewById(R.id.sl);
        violationPostedTextView = findViewById(R.id.dpost);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            //permissions are not granted ask for them
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }else{
            initLocation();
        }
        fetchSpeedData();
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerationSensor, sensorManager.SENSOR_DELAY_NORMAL);
//    sendViolationData_Delete_this_();
    }

    @SuppressLint("MissingPermission")
    private void initLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager != null){
            locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER,
                    0,
                    0,
                    this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        locationController = new LocationController(location);
        double currentSpeed = locationController.getSpeed();
        txt.append("Current speed: "+currentSpeed+"\n");
        latitude = locationController.getLatitude();
        longitude = locationController.getLongitude();
        if (currentSpeed > speedLimit && !sentViolationOneTime){
            sendViolationData();
            sentViolationOneTime = true;
//            txt.append("NOTE: we did it once, don't send two exact same violations"+"\n");
        }

        if(hasExceeded(latitude,longitude,endingLatitude,endingLongitude)){
            sentViolationOneTime = false;
            fetchSpeedData();
            acsLong = longitude < endingLongitude;
            acsLat = latitude < endingLatitude;
        }
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private boolean hasExceeded(double latitude,
                                double longitude,
                                double endingLatitude,
                                double endingLongitude) {
        txt.append("acslong:"+acsLong+"\n"
                   +"acsLat" +acsLat+"\n"
                   +"longitude"+longitude+"\n"
                +"ending longitude"+endingLongitude+"\n"
                +"ending latitude" + endingLatitude+ "\n"
                   +"latitude"+latitude+"\n");
        //TODO: fix that!!!!!!!!!!!!!!!!!
//        return (acsLong == longitude > endingLongitude) || (acsLat == latitude > endingLatitude);
    return  false;
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    private void fetchSpeedData(){
        System.out.println("inside fetch speed data");
        Retrofit retrofit = NetworkClient.getRetrofitClient(serverBaseURL);
        EvilCarAPI speedAPI = retrofit.create(EvilCarAPI.class);
        Call call = speedAPI.getSpeed(longitude,latitude);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                SpeedPojo speedPojo = (SpeedPojo) response.body();
                speedLimit = speedPojo.getSpeed();
                endingLatitude = speedPojo.getEndingLat();
                endingLongitude = speedPojo.getEndingLong();
                txt.append("sl: "+speedLimit+" lon:"+endingLongitude+" lat: "+endingLatitude+"\n");
                speedLimitTextView.setText(speedLimit.toString());
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void sendViolationData(){

        Map<String, Object> jsonParams = new HashMap<>();
                //put something inside the map, could be null
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String time = dateFormat.format(date);
        jsonParams.put("id",CAR_ID);
        jsonParams.put("time",time);
        jsonParams.put("longitude",locationController.getLongitude());
        jsonParams.put("latitude",locationController.getLatitude());
        jsonParams.put("speed",locationController.getSpeed());
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),(new JSONObject(jsonParams)).toString());
        Retrofit retrofit = NetworkClient.getRetrofitClient(serverBaseURL);
        EvilCarAPI speedAPI = retrofit.create(EvilCarAPI.class);
        Call<ResponseBody> call = speedAPI.sendViolation(body);
        insideViolationFunction = true;
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> rawResponse) {
                try {
                    Log.d("MainActivity", "RetroFit2.0 :RetroGetLogin: " + rawResponse.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        }
        );
        violationPostedTextView.setText(insideViolationFunction?"true":"false");
    }
/*
    private void sendViolationData_Delete_this_(){

        Map<String, Object> jsonParams = new HashMap<>();
        //put something inside the map, could be null
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String time = dateFormat.format(date);
        jsonParams.put("id",CAR_ID);
        jsonParams.put("time",time);
        jsonParams.put("longitude",111);
        jsonParams.put("latitude",111);
        jsonParams.put("speed",321);
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),(new JSONObject(jsonParams)).toString());
        Retrofit retrofit = NetworkClient.getRetrofitClient(serverBaseURL);
        EvilCarAPI speedAPI = retrofit.create(EvilCarAPI.class);
        Call<ResponseBody> call = speedAPI.sendViolation(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> rawResponse) {
                try {
                    Log.d("MainActivity", "RetroFit2.0 :RetroGetLogin: " + rawResponse.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }
*/
    @Override
    public void onSensorChanged(SensorEvent event) {
        accelerationCount++;
        if(accelerationCount > ACCELERATION_COUNT_THRESHOLD){
            accelerationCount = 0 ;
            if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
                gravityValues = event.values;

            }
            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                accelerationValues = event.values;
//                txt.append("Acceleration in x: "+accelerationValues[0]+"\n"
//                        +"Acceleration in y: "+accelerationValues[1]+"\n"
//                        +"Acceleration in z: "+accelerationValues[2]+"\n");
                if (accelerationValues[2] > 9.8+ ACCELERATION_THRESHOLD ||
                        accelerationValues[2] < 9.8 - ACCELERATION_THRESHOLD){
                    sendObstacle();
                }
            }
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

    }

    private void sendObstacle() {
        Retrofit retrofit = NetworkClient.getRetrofitClient(serverBaseURL);
        EvilCarAPI speedAPI = retrofit.create(EvilCarAPI.class);
        Call call = speedAPI.sendObstacle(longitude,latitude);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {

            }

            @Override
            public void onFailure(Call call, Throwable t) {

            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
