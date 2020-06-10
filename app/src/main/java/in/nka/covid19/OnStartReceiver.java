package in.nka.covid19;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OnStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Receiver",intent.getAction() );
        context.startService(new Intent(context, MainService.class));
    }
}
