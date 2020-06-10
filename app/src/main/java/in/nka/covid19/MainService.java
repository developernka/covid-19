package in.nka.covid19;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


public class MainService extends Service {
    public final String TAG = this.getClass().getSimpleName();

    MyTimerTask myTask;
    Timer myTimer;

    RequestQueue rQueue;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myTask = new MyTimerTask();
        myTimer = new Timer();
        myTimer.schedule(myTask, 1000, 1*3600*1000);
        rQueue = App.getInstance().getRequestQueue();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            myTimer.cancel();
            myTask.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent("in.nka.covid19");
        sendBroadcast(intent);
    }

    class MyTimerTask extends TimerTask {
        public void run() {
            Log.d(TAG, "run: Timer Fired");
//            generateNotification(getApplicationContext(), "Hello");
            //showNotification("Hello","Hey there !");
            JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, "https://spreadsheets.google.com/feeds/list/1Tk4lBkFpJj_eqCiiz9iR9ac2bCNTvNyDux-B0985mPs/od6/public/values/cokwr?alt=json", null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {

                        Log.d(TAG, "onResponse: "+response.toString());

                        JSONObject total = response.getJSONObject("entry");

                        String title = total.getJSONObject("gsx$confirmed").getString("$t") + " Confirmed,  "+
                                total.getJSONObject("gsx$deaths").getString("$t")+" Deaths,  "+
                                total.getJSONObject("gsx$recovered").getString("$t")+" Recovered";

                        String message = "Updated on: "+ total.getJSONObject("gsx$lastupdatedtime").getString("$t");

                        showNotification(title,message);

                    } catch (JSONException e) {
                        showNotification("Error Occurred","Update App");
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "onErrorResponse: "+error.toString());
                }
            });
            rQueue.add(jsonRequest);
        }
    }
    void showNotification(String title, String message) {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("YOUR_CHANNEL_ID",
                    "YOUR_CHANNEL_NAME",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("YOUR_NOTIFICATION_CHANNEL_DISCRIPTION");
//            AudioAttributes attributes = new AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                    .build();
//            channel.setSound(Uri.parse("android.resource://"+getPackageName()+"/"+R.raw.tone),attributes);
            mNotificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "YOUR_CHANNEL_ID")
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // notification icon
                .setContentTitle(title) // title for notification
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                //.setContentText(message)// message for notification
                .setAutoCancel(true); // clear notification after click
        //mBuilder.setSound(Uri.parse("android.resource://in.nka.covid19/raw/tone.mp3"));
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mNotificationManager.notify(0, mBuilder.build());
//        MediaPlayer mp= MediaPlayer.create(getApplicationContext(), R.raw.tone);
//        mp.start();
    }
}
