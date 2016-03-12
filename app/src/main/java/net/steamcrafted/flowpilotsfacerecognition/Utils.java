package net.steamcrafted.flowpilotsfacerecognition;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by Wannes2 on 5/03/2016.
 *
 * Some utility methods used in this application.
 */
public class Utils {

    private static String uuid_cache;

    /**
     * Log a message.
     * @param message
     */
    public static void log(String message){
        Log.d("FLOW_FACE", message);
    }

    /**
     * Get a unique device specific ID.
     * @param c
     * @return String containing the unique ID.
     */
    public static String getDeviceId(Context c){
        if(uuid_cache == null){
            try {
                WifiManager wm = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
                // Use the Mac address as a unique identifier
                uuid_cache = UUID.nameUUIDFromBytes(wm.getConnectionInfo().getMacAddress().getBytes("utf8")).toString();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return uuid_cache;
    }

}
