package com.example.krisma.architek;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;

import java.util.Arrays;
import java.util.Map;


public class SplashActivity extends Activity{


    private CallbackManager callbackManager;


    private boolean DEBUGGING = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_splash);

        final Activity thisActivity = this;

        if(AccessToken.getCurrentAccessToken() != null){
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "user_friends"));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ImageView image = (ImageView) findViewById(R.id.imageView);
                    image.animate().alpha(0).setDuration(3000).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if(DEBUGGING) {
                                Intent i = new Intent(thisActivity, DebugActivity.class);
                                thisActivity.startActivity(i);
                            } else {
                                Intent i = new Intent(thisActivity, MapsActivity.class);
                                thisActivity.startActivity(i);
                            }
                        }
                    }).start();
                }
            }, 1500);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ImageView image = (ImageView) findViewById(R.id.imageView);
                    image.animate().alpha(0).setDuration(1500).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(thisActivity, LoginActivity.class);
                            thisActivity.startActivity(i);
                        }
                    }).start();
                }
            }, 1500);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onBackPressed() {
        if(!this.getClass().equals(SplashActivity.class)) {
            Intent setIntent = new Intent(this, SplashActivity.class);
            startActivity(setIntent);
        } else {
            super.onBackPressed();
        }
    }
}
