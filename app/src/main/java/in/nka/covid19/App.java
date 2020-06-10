package in.nka.covid19;

import android.app.Application;
import android.content.Intent;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

public class App extends Application {
    private static App mInstance;
    private RequestQueue mRequestQueue;
    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        startService(new Intent(App.this,MainService.class).setAction("Login"));
    }
    public static synchronized App getInstance() {
        return mInstance;
    }
    public RequestQueue getRequestQueue() {
        // lazy initialize the request queue, the queue instance will be
        // created when it is accessed for the first time
        if (mRequestQueue == null) {
            // Instantiate the cache
            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
            // Set up the network to use HttpURLConnection as the HTTP client.
            Network network = new BasicNetwork(new HurlStack());

            //mRequestQueue = Volley.newRequestQueue(getApplicationContext());
            mRequestQueue= new RequestQueue(cache,network);
            mRequestQueue.start();
        }
        return mRequestQueue;
    }

}
