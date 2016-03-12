package net.steamcrafted.flowpilotsfacerecognition;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wannes2 on 5/03/2016.
 *
 * Helper class to simplify some of the Kairos network calls.
 */
public class Networking {

    /**
     * Clear all the training data
     * @param c
     * @param cb
     */
    public static void clearTrainingData(Context c, Kairos.KairosListener cb){
        Kairos k = initKairos(c);
        k.deleteGallery(Utils.getDeviceId(c), cb);
    }

    /**
     * Clear the training data of one subject
     * @param c
     * @param subject The subject to delete the data of
     * @param cb
     */
    public static void clearTrainingSubject(Context c, String subject, Kairos.KairosListener cb){
        initKairos(c).deleteSubject(subject, Utils.getDeviceId(c), cb);
    }

    /**
     * Get a list of all trained subjects, list is of type List<String> for easy usage.
     * @param c
     * @param cb
     */
    public static void getAllTrainedSubjects(Context c, final SimpleDataCallback<List<String>> cb){
        initKairos(c).listSubjectsForGallery(Utils.getDeviceId(c), new Kairos.KairosListener() {
            @Override
            public void onSuccess(String s) {
//                    Utils.log(s);
                try {
                    List<String> out = new ArrayList<>();
                    JSONArray subject_ids = new JSONObject(s).getJSONArray("subject_ids");
                    for (int i = 0; i < subject_ids.length(); i++) {
                        String entry = subject_ids.getString(i);
                        if (!out.contains(entry))
                            out.add(subject_ids.getString(i));
                    }
                    cb.onSuccess(out);
                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                cb.onFail();
            }

            @Override
            public void onFail(String s) {
                cb.onFail();
            }
        });
    }

    /**
     * Initialize a new Kairos object using the provided Context.
     *
     * @param c The context to use for the Kairos object.
     * @return
     */
    public static Kairos initKairos(Context c){
        Kairos k = new Kairos();
        k.setAuthentication(c, BuildConfig.APP_ID, BuildConfig.API_KEY);
        return k;
    }

    /**
     * Callback interface that can be used for a single data type.
     * @param <T>
     */
    public interface SimpleDataCallback<T> {
        void onSuccess(T data);
        void onFail();
    }
}
