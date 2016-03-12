package net.steamcrafted.flowpilotsfacerecognition.model;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Wannes2 on 12/03/2016.
 *
 * Model containing the rotation needed to correctly display an image taken by the camera.
 */
public class CalibrateModel {

    private static final String PREF_ROTATE = "PREF_ROTATE";

    /**
     * Saves the rotation value to disk.
     * @param c The context needed for SharedPrefs
     * @param rotation The value to save.
     */
    public static void store(Context c, int rotation){
        PreferenceManager.getDefaultSharedPreferences(c).edit().putInt(PREF_ROTATE, rotation % 360).apply();
    }

    /**
     * Get the saved rotation value.
     * @param c The context needed for SharedPrefs
     * @return The saved value.
     */
    public static int get(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c).getInt(PREF_ROTATE, 0);
    }

    /**
     * Check if a rotation value has been saved.
     * @param c The context needed for SharedPrefs
     * @return True if it exists on disk.
     */
    public static boolean exists(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c).contains(PREF_ROTATE);
    }

}
