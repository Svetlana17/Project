package fly.speedmeter.grub;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.gc.materialdesign.widgets.Dialog;
import com.google.gson.Gson;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class MainActivity extends ActionBarActivity implements LocationListener, GpsStatus.Listener {

    private static final String TAG ="gps" ;
    private SharedPreferences  sharedPreferences;
    private LocationManager mLocationManager;
    private static Data data;

    private Toolbar toolbar;
    private FloatingActionButton fab;

    private FloatingActionButton refresh;
    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    private TextView satellite;
    private TextView status;
    private TextView accuracy;

    private TextView currentSpeed;
    private TextView maxSpeed;
    private TextView averageSpeed;
    private TextView distance;
    private Chronometer time;
    private Data.OnGpsServiceUpdate onGpsServiceUpdate;

    private boolean firstfix;
    private ArrayList<Location> locations=new ArrayList<>();
    private TextView height;
    Button buttonStart ,buttonStop, buttonmessage;
    private FileWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        data = new Data(onGpsServiceUpdate);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //setTitle("");
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);

        refresh = (FloatingActionButton) findViewById(R.id.refresh);
        refresh.setVisibility(View.INVISIBLE);

        fab.setVisibility(View.INVISIBLE);
        height=(TextView) findViewById(R.id. height);

       buttonStart=(Button) findViewById(R.id.buttonStart);
       buttonStart.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               File file = new File(getStorageDir(), "sensors.csv");
               if(file.exists())
                   file.delete();

               Log.d(TAG, "Writing to " + getStorageDir());
               try {
                   writer = new FileWriter(file);
                   writer.write("TIME;  Latitude; Longitude;  Speed;  Distanse; Altitutude; Accurancy;  \n");
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
       });


      buttonStop=(Button) findViewById(R.id.buttonStop);

      buttonStop.setOnClickListener(new View.OnClickListener(){

          @Override
          public void onClick(View v) {
              if(writer!=null) {
                  try {
                      writer.close();
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }
          }
      });



      buttonmessage=(Button) findViewById(R.id.buttonMessage);
      buttonmessage.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              share();
          }
      });



        onGpsServiceUpdate = new Data.OnGpsServiceUpdate() {
            @Override
            public void update() {
                double maxSpeedTemp = data.getMaxSpeed();
                double distanceTemp = data.getDistance();
                double averageTemp;
                if (sharedPreferences.getBoolean("auto_average", false)){
                    averageTemp = data.getAverageSpeedMotion();
                }else{
                    averageTemp = data.getAverageSpeed();
                }

                String speedUnits;
                String distanceUnits;
                if (sharedPreferences.getBoolean("miles_per_hour", false)) {
                    maxSpeedTemp *= 0.62137119;
                    distanceTemp = distanceTemp / 1000.0 * 0.62137119;
                    averageTemp *= 0.62137119;
                    speedUnits = "mi/h";
                    distanceUnits = "mi";
                } else {
                    speedUnits = "km/h";
                    if (distanceTemp <= 1000.0) {
                        distanceUnits = "m";
                    } else {
                        distanceTemp /= 1000.0;
                        distanceUnits = "km";
                    }
                }

                SpannableString s = new SpannableString(String.format("%.0f %s", maxSpeedTemp, speedUnits));
                s.setSpan(new RelativeSizeSpan(0.5f), s.length() - speedUnits.length() - 1, s.length(), 0);
                maxSpeed.setText(s);

                s = new SpannableString(String.format("%.0f %s", averageTemp, speedUnits));
                s.setSpan(new RelativeSizeSpan(0.5f), s.length() - speedUnits.length() - 1, s.length(), 0);
                averageSpeed.setText(s);

                s = new SpannableString(String.format("%.3f %s", distanceTemp, distanceUnits));
                s.setSpan(new RelativeSizeSpan(0.5f), s.length() - distanceUnits.length() - 1, s.length(), 0);
                distance.setText(s);

                double altitude = data.getAltitude();
                String altitudeUnits="m";
                s=new SpannableString(String.format("%.3f %s", altitude, altitudeUnits));
                height.setText(s);
            }
        };

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        satellite = (TextView) findViewById(R.id.satellite);
        status = (TextView) findViewById(R.id.status);
        accuracy = (TextView) findViewById(R.id.accuracy);
        maxSpeed = (TextView) findViewById(R.id.maxSpeed);
        averageSpeed = (TextView) findViewById(R.id.averageSpeed);
        distance = (TextView) findViewById(R.id.distance);
        time = (Chronometer) findViewById(R.id.time);
        currentSpeed = (TextView) findViewById(R.id.currentSpeed);
        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBarCircularIndeterminate);

        time.setText("00:00:00");
        time.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            boolean isPair = true;
            @Override
            public void onChronometerTick(Chronometer chrono) {
                long time;
                if(data.isRunning()){
                    time= SystemClock.elapsedRealtime() - chrono.getBase();
                    data.setTime(time);
                }else{
                    time = data.getTime();
                }

                int h   = (int)(time /3600000);
                int m = (int)(time  - h*3600000)/60000;
                int s= (int)(time  - h*3600000 - m*60000)/1000 ;
                String hh = h < 10 ? "0"+h: h+"";
                String mm = m < 10 ? "0"+m: m+"";
                String ss = s < 10 ? "0"+s: s+"";
                chrono.setText(hh+":"+mm+":"+ss);

                if (data.isRunning()){
                    chrono.setText(hh+":"+mm+":"+ss);
                } else {
                    if (isPair) {
                        isPair = false;
                        chrono.setText(hh+":"+mm+":"+ss);
                    }else{
                        isPair = true;
                        chrono.setText("");
                    }
                }

            }
        });
    }

    private String getStorageDir() {

            return this.getExternalFilesDir(null).getAbsolutePath();

    }

    public void onFabClick(View v){
        if (!data.isRunning()) {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause));
            data.setRunning(true);
            time.setBase(SystemClock.elapsedRealtime() - data.getTime());
            time.start();
            data.setFirstTime(true);
            startService(new Intent(getBaseContext(), GpsServices.class));
            refresh.setVisibility(View.INVISIBLE);
        }else{
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
            data.setRunning(false);
            status.setText("");
            stopService(new Intent(getBaseContext(), GpsServices.class));
            refresh.setVisibility(View.VISIBLE);
        }
    }

    public void onRefreshClick(View v){
        resetData();
        stopService(new Intent(getBaseContext(), GpsServices.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        firstfix = true;
        if (!data.isRunning()){
            Gson gson = new Gson();
            String json = sharedPreferences.getString("data", "");
            data = gson.fromJson(json, Data.class);
        }
        if (data == null){
            data = new Data(onGpsServiceUpdate);
        }else{
            data.setOnGpsServiceUpdate(onGpsServiceUpdate);
        }

        if (mLocationManager.getAllProviders().indexOf(LocationManager.GPS_PROVIDER) >= 0) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        } else {
            Log.w("MainActivity", "No GPS location provider found. GPS data display will not be available.");
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog();
        }

        mLocationManager.addGpsStatusListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(data);
        prefsEditor.putString("data", json);
        prefsEditor.commit();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(new Intent(getBaseContext(), GpsServices.class));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            Intent intent = new Intent(this, Settings.class);
//            startActivity(intent);

            if(id==R.id.map){
                Intent intents = new Intent(this, MapsViewActivity.class);
                ArrayList<Location> locations=this.locations;
                int locationscount=locations.size();
                double[]lats=new double[locationscount];

                double[]lngs=new double[locationscount];
                for(int i=0; i<locationscount; i++){
                    lats[i]=locations.get(i).getLatitude();
                    lngs[i]=locations.get(i).getLongitude();
                }
                intents.putExtra("lats", lats);
                intents.putExtra("lngs", lngs);
                startActivity(intents);

            //}
        }
        if(id==R.id.sensor){
            Intent intent1=new Intent(this, SensorActivity.class);
            startActivity(intent1);
        }
        else {
                    Intent intent = new Intent(this, Settings.class);
                    startActivity(intent);
                  }

        return super.onOptionsItemSelected(item);
    }
  SimpleDateFormat  format= new SimpleDateFormat("HH:mm:ss");

    @Override
    public void onLocationChanged(Location location) {
        double distance=0;
        if(locations.size()>0){
           Location lastLocation=locations.get(locations.size()-1);
           distance=lastLocation.distanceTo(location);
        }
  if(writer!=null) {
      try {
          writer.write(String.format("%s; %f; %f; %f; %f;%f;%f; \n",
                  format.format(new Date(location.getTime())),
                  location.getLatitude(),
                  location.getLatitude(),
                  location.getSpeed(),
                  distance,
                  location.getAltitude(),
                  location.getAccuracy())
          );
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
        //  writer.write("TIME;  Latitude; Longitude;  Speed;  Distanse; Altitutude;  \n");

        locations.add(location);

        if (location.hasAccuracy()) {
            double acc = location.getAccuracy();
            String units;
            if (sharedPreferences.getBoolean("miles_per_hour", false)) {
                units = "ft";
                acc *= 3.28084;
            } else {
                units = "m";
            }
            SpannableString s = new SpannableString(String.format("%.0f %s", acc, units));
            s.setSpan(new RelativeSizeSpan(0.75f), s.length()-units.length()-1, s.length(), 0);
            accuracy.setText(s);

            if (firstfix){
                status.setText("");
                fab.setVisibility(View.VISIBLE);
                if (!data.isRunning() && !TextUtils.isEmpty(maxSpeed.getText())) {
                    refresh.setVisibility(View.VISIBLE);
                }
                firstfix = false;
            }
        }else{
            firstfix = true;
        }

        if (location.hasSpeed()) {
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            double speed = location.getSpeed() * 3.6;
            String units;
            if (sharedPreferences.getBoolean("miles_per_hour", false)) { // Convert to MPH
                // Преобразовать метры / секунды в мили / час
                speed *= 0.62137119;
                units = "mi/h";
            } else {
                units = "km/h";
            }
            SpannableString s = new SpannableString(String.format(Locale.ENGLISH, "%.0f %s", speed, units));
            s.setSpan(new RelativeSizeSpan(0.25f), s.length()-units.length()-1, s.length(), 0);
            currentSpeed.setText(s);
        }

    }

    @Override
    public void onGpsStatusChanged (int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                int satsInView = 0;
                int satsUsed = 0;
                Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
                for (GpsSatellite sat : sats) {
                    satsInView++;
                    if (sat.usedInFix()) {
                        satsUsed++;
                    }
                }
                satellite.setText(String.valueOf(satsUsed) + "/" + String.valueOf(satsInView));
                if (satsUsed == 0) {
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
                    data.setRunning(false);
                    status.setText("");
                    stopService(new Intent(getBaseContext(), GpsServices.class));
                    fab.setVisibility(View.INVISIBLE);
                    refresh.setVisibility(View.INVISIBLE);
                    accuracy.setText("");
                    status.setText(getResources().getString(R.string.waiting_for_fix));
                    firstfix = true;
                }
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showGpsDisabledDialog();
                }
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;
        }
    }

    public void showGpsDisabledDialog(){
        Dialog dialog = new Dialog(this, getResources().getString(R.string.gps_disabled), getResources().getString(R.string.please_enable_gps));

        dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            }
        });
        dialog.show();
    }

    public void resetData(){
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
        refresh.setVisibility(View.INVISIBLE);
        time.stop();
        maxSpeed.setText("");
        averageSpeed.setText("");
        distance.setText("");
        time.setText("00:00:00");
        data = new Data(onGpsServiceUpdate);
    }

    public static Data getData() {
        return data;
    }

    @Override
    public void onBackPressed(){
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) {}


    //LДля кнопки отправить
    private void share() {
        File dir = getExternalFilesDir(null);
        File zipFile = new File(dir, "accel.zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }
        File[] fileList = dir.listFiles();
        try {
            zipFile.createNewFile();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
            for (File file : fileList) {
                zipFile(out, file);
            }
            out.close();
            sendBundleInfo(zipFile);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Can't send file!", Toast.LENGTH_LONG).show();
        }
    }
    private static void zipFile(ZipOutputStream zos, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(file.getName()));
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[10000];
        int byteCount = 0;
        try {
            while ((byteCount = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, byteCount);
            }
        } finally {
            safeClose(fis);
        }
        zos.closeEntry();
    }

    private static void safeClose(FileInputStream fis) {
        try {
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendBundleInfo(File file) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file));
        startActivity(Intent.createChooser(emailIntent, "Send data"));
    }
}
