package net.steamcrafted.flowpilotsfacerecognition.model;

import android.content.Context;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wannes2 on 12/03/2016.
 *
 * Model containing the list of users that can be used to train.
 */
public class UserModel {

    private static final String PREF_USERS = "PREF_USERS";

    /**
     * Saves the model to disk using the passed list.
     * @param c The context needed for SharedPrefs
     * @param users The list of users to save.
     */
    public static void store(Context c, List<String> users){
        JSONArray arr = new JSONArray(users);
        PreferenceManager.getDefaultSharedPreferences(c).edit().putString(PREF_USERS, arr.toString()).apply();
    }

    /**
     * Get a list of all available users.
     *
     * @param c The context needed for SharedPrefs
     * @return A list of users inside the model.
     */
    public static List<String> get(Context c){
        String jsonArr = PreferenceManager.getDefaultSharedPreferences(c).getString(PREF_USERS, "['John', 'Mary']");
        List<String> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(jsonArr);
            for(int i = 0; i < arr.length(); i++){
                result.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Check if there is a list of users saved.
     *
     * @param c The context needed for SharedPrefs
     * @return True if there is a list.
     */
    public static boolean exists(Context c){
        return PreferenceManager.getDefaultSharedPreferences(c).contains(PREF_USERS);
    }

}
