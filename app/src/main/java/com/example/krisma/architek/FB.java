package com.example.krisma.architek;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;


/**
 * Created by smp on 20/07/15.
 */
public class FB {


    /***
     * Get the user's friends who are ALSO USING THE APP and have GRANTED PERMISSION (requires facebook login)
     * @return
     */
    public static JSONArray getMyFriends(){
        final JSONArray friends = new JSONArray();

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/friends",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        /* handle the result */
                        JSONArray res = response.getJSONArray();
                        if(res != null) {
                            for (int i = 0; i < res.length(); i++) {
                                try {
                                    friends.put(i, res.get(i));
                                } catch (JSONException e) {
                                    continue;
                                }
                            }
                        }
                    }
                }
        ).executeAsync();
        return friends;
    }

}
