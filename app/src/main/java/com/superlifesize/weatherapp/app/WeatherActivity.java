package com.superlifesize.weatherapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


public class WeatherActivity extends Activity {

    public static final String TAG = WeatherActivity.class.getSimpleName();

    public static final String BASE_URL = "https://api.forecast.io/forecast";
    public static final String API_KEY = "78a898a4e77432239aa213d337238bed";
    public static final String FL_LAT = "28.4158";
    public static final String FL_LONG = "-81.2989";

    protected JSONObject forecastData;
    protected ProgressBar progressBar;

    private TextView clockDisplay;
    private DateFormat dateFormat;
    private Timer clock;
    private final Runnable drawClock = new Runnable() {
        @Override
        public void run() {
            onUpdateDisplay();
        }
    };



    //=========================================================================
    // Lifecycle
    //=========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        clockDisplay = (TextView) findViewById(R.id.display);
        Typeface robotThinFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
        clockDisplay.setTypeface(robotThinFont);

        dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        onUpdateDisplay();

        //Check if network if available
        if(isNetworkAvailable()) {
            progressBar.setVisibility(View.VISIBLE);
            GetForecastDataTask getForecastData = new GetForecastDataTask();
            getForecastData.execute();
        }
        else {
            Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        clock = new Timer();
        clock.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onClockTick();
            }
        }, 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        clock.cancel();
        clock = null;
    }



    //=========================================================================
    // Private Methods
    //=========================================================================

    private void LogException(Exception e) {
        Log.i(TAG, "Exception Caught!", e);
    }

    private void onUpdateDisplay() {
        clockDisplay.setText(dateFormat.format(new Date()));
    }

    private void onClockTick() {
        if (clockDisplay != null) {
            clockDisplay.post(drawClock);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;

    }


    public void handleForecastResponse() {
        progressBar.setVisibility(View.INVISIBLE);

        if(forecastData == null){
            //display error message
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.request_error_msg));
            builder.setMessage(getString(R.string.error_message));
            builder.setPositiveButton(android.R.string.ok, null);
            AlertDialog dialog = builder.create();
            dialog.show();

        }
        else {
            try {
                JSONObject jsonCurrForecast = forecastData.getJSONObject("currently");

                Typeface robotThinFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");

                //weather summary
                TextView tv_summary = (TextView)findViewById(R.id.weather_summary);
                tv_summary.setText(jsonCurrForecast.getString("summary"));
                tv_summary.setTypeface(robotThinFont);

                //weather temperature
                TextView tv_temp = (TextView)findViewById(R.id.weather_temp);
                Float temperature = Float.parseFloat(jsonCurrForecast.getString("temperature"));
                tv_temp.setText(String.valueOf(Math.round(temperature)) + "°F");
                tv_temp.setTypeface(robotThinFont);

                //weather feels-like temperature
                TextView tv_app_temp = (TextView)findViewById(R.id.weather_app_temp);
                Float app_temperature = Float.parseFloat(jsonCurrForecast.getString("apparentTemperature"));
                tv_app_temp.setText(String.valueOf(Math.round(app_temperature)) + "°F");
                tv_app_temp.setTypeface(robotThinFont);

                //set weather icon
                ImageView tv_icon = (ImageView)findViewById(R.id.weather_icon);

                if (jsonCurrForecast.getString("icon").equals("clear-day")) {
                    tv_icon.setImageResource(R.drawable.clear);
                }else if (jsonCurrForecast.getString("icon").equals("clear-night")) {
                    tv_icon.setImageResource(R.drawable.n_clear);
                } else if (jsonCurrForecast.getString("icon").equals("rain")) {
                    tv_icon.setImageResource(R.drawable.showers);
                } else if (jsonCurrForecast.getString("icon").equals("snow")) {
                    tv_icon.setImageResource(R.drawable.snow);
                } else if (jsonCurrForecast.getString("icon").equals("sleet")) {
                    tv_icon.setImageResource(R.drawable.sleet);
                } else if (jsonCurrForecast.getString("icon").equals("wind")) {
                    tv_icon.setImageResource(R.drawable.windy);
                } else if (jsonCurrForecast.getString("icon").equals("fog")) {
                    tv_icon.setImageResource(R.drawable.foggy);
                } else if (jsonCurrForecast.getString("icon").equals("cloudy")) {
                    tv_icon.setImageResource(R.drawable.cloudy);
                } else if (jsonCurrForecast.getString("icon").equals("partly-cloudy-day")) {
                    tv_icon.setImageResource(R.drawable.partly_cloudy);
                } else if (jsonCurrForecast.getString("icon").equals("partly-cloudy-night")) {
                    tv_icon.setImageResource(R.drawable.n_partly_cloudy);
                } else if (jsonCurrForecast.getString("icon").equals("thunderstorm")) {
                    tv_icon.setImageResource(R.drawable.thunderstorms);
                } else {
                    tv_icon.setImageResource(R.drawable.partly_cloudy);
                }
            }
            catch (JSONException e) {
                LogException(e);
            }
        }
    }


    //=========================================================================
    // AsyncTask
    //=========================================================================

    private class GetForecastDataTask extends AsyncTask<Objects, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Objects... params) {
            int responseCode = -1;
            JSONObject jsonResponse = null;
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            HttpGet forecastUrl = new HttpGet(BASE_URL + "/" + API_KEY + "/" + FL_LAT + "," + FL_LONG);

            try {
                HttpResponse response = client.execute(forecastUrl);
                StatusLine statusLine = response.getStatusLine();
                responseCode = statusLine.getStatusCode();

                if(responseCode == HttpURLConnection.HTTP_OK) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    jsonResponse = new JSONObject(builder.toString());

                    Log.i(TAG, "Request Code: " + responseCode);
                }
                else {
                    Log.i(TAG, "Unresponsive HTTP Request Code: " + responseCode);
                }

            } catch (MalformedURLException e) {
                LogException(e);
            } catch (IOException e) {
                LogException(e);
            }
            catch (JSONException e) {
                LogException(e);
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            forecastData = result;
            handleForecastResponse();
        }
    }




    //=========================================================================
    // Menu
    //=========================================================================

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.weather, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
