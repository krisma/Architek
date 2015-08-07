package com.example.krisma.architek.asynctasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.example.krisma.architek.LoginActivity;
import com.example.krisma.architek.MapsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by smp on 07/08/15.
 */
public class AsyncSkipSignup extends AsyncTask<Void, Void, Void> {

    private final Context context;

    public AsyncSkipSignup(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... parameters) {
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        URL url = null;
        HttpURLConnection conn = null;
        try {
//                    url = new URL("http://10.0.2.2:8080/skipsignup");
            url = new URL("https://architek-server.herokuapp.com/skipsignup");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            StringBuffer params = new StringBuffer();
            JSONObject e = new JSONObject();
            try {
                e.put("secret", "igottafeeling");
            } catch (JSONException e1) {
                e1.printStackTrace();
            };
            params.append(e.toString());
            out.writeBytes(params.toString());
            out.flush();
            out.close();
            InputStream in = conn.getInputStream();
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(LoginActivity.convertStreamToString(in));
            } catch (JSONException f) {
                f.printStackTrace();
            }
            if (jObject != null) try {
                if (jObject.getBoolean("success") == true) {


                    SharedPreferences.Editor editor = getPrefs.edit();
                    editor.putString("token",jObject.getString("token"));
                    editor.apply();
                    Intent intent = new Intent(context, MapsActivity.class);
                    context.startActivity(intent);

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("Skip registration failed. Please try again.")
                            .setCancelable(true)
                            .setPositiveButton("Gotcha", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                };
                            });

                    AlertDialog alert = builder.create();
                    alert.setTitle("Error");
                    alert.show();
                };
            } catch (JSONException g) {
                g.printStackTrace();
            }
            ;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace(); //If you want further info on failure...
            }
        }
        return null;
    }
}
