package in.nka.covid19;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public final String TAG = this.getClass().getSimpleName();

    TextView textView;
    RecyclerView recyclerView;
    MainRecyclerAdapter adapter;
    RequestQueue rQueue;

    LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading latest data ... Please Wait !", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                refreshData();
            }
        });

        startService(new Intent(MainActivity.this,MainService.class));

        textView = findViewById(R.id.lastupdatetime);
        recyclerView = findViewById(R.id.recyclerview);
        adapter = new MainRecyclerAdapter();
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        rQueue = App.getInstance().getRequestQueue();

        getLocalData();

        refreshData();

        analytics();
    }

    private void analytics() {
        String id = id(App.getInstance());
        RequestQueue requestQueue = App.getInstance().getRequestQueue();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://script.google.com/macros/s/AKfycbwSVR0kBRSI1PZ1QJPZf0lp-TBIHx2PqzPshF_FJLUIM_w9oZE/exec?from=Activity&id="+id, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "onResponse: Analytics");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: "+ error.toString());
            }
        });
        requestQueue.add(stringRequest);
    }

    void getLocalData(){
        SharedPreferences sharedPreferences = getSharedPreferences("DATA_ID",Context.MODE_PRIVATE);
        String dataString = sharedPreferences.getString("DATA_ID","[]");
        Log.d(TAG, "getLocalData: "+dataString);
        try {
            JSONArray data = new JSONArray(dataString);
            adapter.setNewData(data);
            textView.setText("Loading latest data ... Please Wait!");
            linearLayoutManager.scrollToPositionWithOffset(0,0);
            Log.d(TAG, "getLocalData: Local Data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void refreshData(){
        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, "https://spreadsheets.google.com/feeds/list/1Tk4lBkFpJj_eqCiiz9iR9ac2bCNTvNyDux-B0985mPs/od6/public/values?alt=json", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, "onResponse: "+response.toString());
                    JSONArray statewise = response.getJSONObject("feed").getJSONArray("entry");
                    SharedPreferences sharedPreferences = getSharedPreferences("DATA_ID",Context.MODE_PRIVATE);
                    sharedPreferences.edit().putString("DATA_ID",statewise.toString()).commit();
                    adapter.setNewData(statewise);
                    textView.setText("Updated On : "+statewise.getJSONObject(0).getJSONObject("gsx$lastupdatedtime").getString("$t"));
                    linearLayoutManager.scrollToPositionWithOffset(0,0);
                    Log.d(TAG, "onResponse: Server Data");
                } catch (JSONException e) {
                    textView.setText("App outdated please Update.");
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: "+error.toString());
                textView.setText("Network Error. Check Internet Connection.");
            }
        });
        rQueue.add(jsonRequest);
    }

    class MainRecyclerAdapter extends RecyclerView.Adapter<MainViewHolder>{
        JSONArray data = new JSONArray();

        public void setNewData(JSONArray data){
            this.data = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_main, parent, false);
            return new MainViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
            try {
                holder.setItem((JSONObject) data.get(position));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return data.length();
        }


    }
    class MainViewHolder extends RecyclerView.ViewHolder{
        TextView state, confirmed, active, deaths, recovered;
        MainViewHolder(@NonNull View itemView){
            super(itemView);
            state = itemView.findViewById(R.id.state);
            confirmed = itemView.findViewById(R.id.confirmed);
            active = itemView.findViewById(R.id.active);
            deaths = itemView.findViewById(R.id.deaths);
            recovered = itemView.findViewById(R.id.recovered);

        }
        void setItem(JSONObject item){
            try{
            state.setText(item.getJSONObject("gsx$state").getString("$t"));
            confirmed.setText(item.getJSONObject("gsx$confirmed").getString("$t"));
            active.setText(item.getJSONObject("gsx$active").getString("$t"));
            deaths.setText(item.getJSONObject("gsx$deaths").getString("$t"));
            recovered.setText(item.getJSONObject("gsx$recovered").getString("$t"));
            }catch (JSONException e){
                Log.d(TAG, "setItem: "+e.toString());
            }
        }
    }

    private void enableAutoStart() {
        try {
            Intent intent = new Intent();
            String manufacturer = android.os.Build.MANUFACTURER;
            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            }

            List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if  (list.size() > 0) {
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.d(TAG, "enableAutoStart: "+e.toString());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            enableAutoStart();
            return true;
        }

        if (id == R.id.action_info) {
            alertMessage(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    final String alertMessageID = "Alert_Message_ID";
    SharedPreferences sharedPrefs;
    void alertMessage(Context context) {
        sharedPrefs = context.getSharedPreferences(
                    alertMessageID, Context.MODE_PRIVATE);
        String alertMsg = sharedPrefs.getString(alertMessageID, "Loading...");

        showInfoDialog("App Info",alertMsg);
        getALertFromServer();

    }

    void getALertFromServer(){
        String url = "https://spreadsheets.google.com/feeds/list/1Tk4lBkFpJj_eqCiiz9iR9ac2bCNTvNyDux-B0985mPs/ofw0h1q/public/values?alt=json";
        JsonRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(alertMessageID, "onResponse:alert "+response.toString());
                    JSONArray infos = response.getJSONObject("feed").getJSONArray("entry");
                    String alertMsgServer ="";
                    for(int i=0;i<infos.length();i++){
                        alertMsgServer += infos.getJSONObject(i).getJSONObject("gsx$title").getString("$t")+"\t" +
                                infos.getJSONObject(i).getJSONObject("gsx$content").getString("$t")+"\n";
                    }
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(alertMessageID, alertMsgServer);
                    editor.commit();
                    showInfoDialog("App Info",alertMsgServer);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(alertMessageID, "onErrorResponse: "+error.toString());
            }
        });
        App.getInstance().getRequestQueue().add(jsonRequest);
    }

    AlertDialog.Builder alert =  null;
    AlertDialog alertDialog = null;
    void showInfoDialog(String title,String content){
        Log.d(TAG, "showInfoDialog: "+content);
        if (alert == null) {
            alert = new AlertDialog.Builder(this);
        }
        if(alertDialog == null){
            alertDialog = alert.create();
        }else{
            alertDialog.dismiss();
        }
        alertDialog.setTitle(title);
        alertDialog.setMessage(content);
        alertDialog.show();
    }


    //UUID
    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }
}
