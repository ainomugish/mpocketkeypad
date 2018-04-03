package xanthan.mpocketkeypad;

import android.app.Application;
import android.util.Log;

import com.orm.SugarContext;

import Mpock.Globals;

/**
 * Created by Mac on 2016/03/20.
 */
public class GlobalState extends Application {

    Boolean log = true;

    public String connectionStatus = Globals.DISCONNECTED;

    @Override
    public void onCreate() {
        super.onCreate();

        //INIT SUGAR
        SugarContext.init(getApplicationContext());
    }


    @Override
    public void onTerminate() {
        super.onTerminate();

        SugarContext.terminate();
        // Initialize the singletons so their instances
        // are bound to the application process.
        //initSingletons();
    }

    void log(String msg) {
        if (log && Globals.log) {
            Log.v(this.getClass().getSimpleName(), msg);
        }
    }
}
