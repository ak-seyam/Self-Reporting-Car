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
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.src.controller.ISpeedAPI;
import com.example.src.controller.LocationController;
import com.example.src.controller.NetworkClient;
import com.example.src.model.SpeedPojo;

import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
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
    private static final int CAR_ID = 101 ;
    LocationController locationController;
    double speedLimit = 1000.0;
    double longitude,latitude;
    double endingLongitude,endingLatitude;
    private boolean sentViolationOneTime;
    private SensorManager accelerationSensorManager;
    private Sensor accelerationSensor;
    private float[] accelerationValues;
    private boolean acsLong,acsLat;
    private TextView txt;
    private ScrollView scrollView;
    private String serverBaseURL = "https://evilcar.herokuapp.com";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt = findViewById(R.id.log);
        scrollView = findViewById(R.id.sv);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            //permissions are not granted ask for them
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }else{
            initLocation();
        }
        fetchSpeedData();

        accelerationSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        accelerationSensor = accelerationSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerationSensorManager.registerListener(this, accelerationSensor, accelerationSensorManager.SENSOR_DELAY_NORMAL);

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
        Toast.makeText(this, "speed limit"+ speedLimit, Toast.LENGTH_LONG).show();
        locationController = new LocationController(location);
        double currentSpeed = locationController.getSpeed();
        System.out.println("Current speed: "+currentSpeed);
        txt.append("Current speed: "+currentSpeed+"\n");
        latitude = locationController.getLatitude();
        longitude = locationController.getLongitude();
        System.out.println("current speed "+currentSpeed);
        if (currentSpeed > speedLimit && !sentViolationOneTime){
            sendViolationData();
            Toast.makeText(this, "Fire a violation", Toast.LENGTH_SHORT).show();
            sentViolationOneTime = true;
            txt.append("NOTE: we did it once, don't send two exact same violations"+"\n");
            fetchSpeedData();
            acsLong = longitude < endingLongitude;
            acsLat = latitude < endingLatitude;
        }

//        sentViolationOneTime = speedLimit > currentSpeed;

        if(hasExceeded(latitude,longitude,endingLatitude,endingLongitude)){
            sentViolationOneTime = false;
            fetchSpeedData();
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
        Retrofit retrofit = NetworkClient.getRetrofitClient(serverBaseURL);
        ISpeedAPI speedAPI = retrofit.create(ISpeedAPI.class);
        Call call = speedAPI.getSpeed(longitude,latitude);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                SpeedPojo speedPojo = (SpeedPojo) response.body();
                speedLimit = speedPojo.getSpeed();
                endingLatitude = speedPojo.getEndingLat();
                endingLongitude = speedPojo.getEndingLong();
                txt.append("sl: "+speedLimit+" lon:"+endingLongitude+" lat: "+endingLatitude+"\n");
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
        Retrofit retrofit = NetworkClient.getRetrofitClient("http://192.168.1.28:5000");
        ISpeedAPI speedAPI = retrofit.create(ISpeedAPI.class);
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        accelerationValues = event.values;
        System.out.println("Acceleration in x: "+accelerationValues[0]+"\n"
                  +"Acceleration in y: "+accelerationValues[1]+"\n"
                  +"Acceleration in z: "+accelerationValues[2]+"\n");

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
