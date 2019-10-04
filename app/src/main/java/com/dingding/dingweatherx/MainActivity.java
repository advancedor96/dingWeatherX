package com.dingding.dingweatherx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextView tv_location, tv_temp;
    ImageView iv_weatherIcon;
    static final int Ding_req_location = 1;
    private FusedLocationProviderClient fusedLocationClient;
    double Lat, Lng;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        Picasso.get().load("http://java.sogeti.nl/JavaBlog/wp-content/uploads/2009/04/android_icon_256.png").into(iv_weatherIcon);
//        getLocation();

        // show The Image in a ImageView
//        new DownloadImageTask(iv_weatherIcon)
//                .execute("http://java.sogeti.nl/JavaBlog/wp-content/uploads/2009/04/android_icon_256.png");
    }



    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            Log.d("ding", "1");
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            Log.d("ding", "2");
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            Log.d("ding", "3");
            bmImage.setImageBitmap(result);
        }
    }


    private void getLocation() {
        Log.d("ding", "開始 get location");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, Ding_req_location);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Lat = location.getLatitude();
                            Lng = location.getLongitude();
                            Log.d("ding", "經緯度d, Lat:" + Lat + ", lng:" + Lng);
                            
                            getWeather();

                        }else{
                            Log.d("ding", "發現location為空");
                        }
                    }
                });
    }

    private void getWeather() {

        Geocoder geoCoder = new Geocoder(this);
        try {
            List<Address> address = geoCoder.getFromLocation(Lat, Lng, 1);
            tv_location.setText(address.get(0).getAdminArea() + address.get(0).getLocality());
            Log.d("ding", String.format("設定區域為:%s %s", address.get(0).getAdminArea() ,address.get(0).getLocality() ));

            OkHttpClient client = new OkHttpClient();

            String url = "https://weather.cit.api.here.com/weather/1.0/report.json?product=observation&latitude=" + Lat + "&longitude=" + Lng + "&oneobservation=true&app_id=DemoAppId01082013GAL&app_code=AJKnXv84fjrb0KIHawS0Tg";
            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback(){
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if(response.isSuccessful()){
                        Log.d("ding", "讀api成功");
                    String jsonStr = response.body().string();
                    Log.d("ding", String.format("這是:%s", jsonStr));

                        try {
                            JSONObject obj = new JSONObject(jsonStr);
                            Log.d("ddd", "d");
                            JSONObject observation = obj.getJSONObject("observations").getJSONArray("location").getJSONObject(0).getJSONArray("observation").getJSONObject(0) ;
                            String temp = observation.getString("temperature");
                            String iconLink = observation.getString("iconLink");
                            updateUI(temp, iconLink);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }

                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.d("ding", "讀api失敗");
                }
            });



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUI(final String temp,final  String iconLink) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("ding", "設溫度:"+ temp);
                tv_temp.setText(String.format("%s℃", temp));
                Picasso.get().load(iconLink).into(iv_weatherIcon);

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Ding_req_location: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ding", "發現權限過了，重新呼叫 get location");
                    getLocation();
                } else {
                    Log.d("ding", "d發現還沒有權限");

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }
    private void init() {
        tv_location = findViewById(R.id.tv_location);
        tv_temp = findViewById(R.id.tv_temp);
        iv_weatherIcon = findViewById(R.id.iv_weatherIcon);
    }


    public void handleClick(View view) {
    }
}
