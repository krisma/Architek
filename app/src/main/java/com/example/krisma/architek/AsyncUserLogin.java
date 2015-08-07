package com.example.krisma.architek;

import android.content.Intent;
import android.os.AsyncTask;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class AsyncUserLogin extends AsyncTask<String, Void, Boolean> {
    private final LoginActivity loginActivity;

    public AsyncUserLogin(LoginActivity loginActivity) {
        this.loginActivity = loginActivity;
    }


    private String mEmail;
    private String mPassword;


    @Override
    protected Boolean doInBackground(String... params) {
        // TODO: attempt authentication against a network service

        mEmail = params[0];
        mPassword = params[1];

        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        loginActivity.setmAuthTask(null);
        loginActivity.showProgress(false);

        if (success) {
            Intent i = new Intent(loginActivity.getApplicationContext(), MapsActivity.class);
            loginActivity.startActivity(i);
        } else {
            loginActivity.getmPasswordView().setError(loginActivity.getString(R.string.error_incorrect_password));
            loginActivity.getmPasswordView().requestFocus();
        }
    }

    @Override
    protected void onCancelled() {
        loginActivity.setmAuthTask(null);
        loginActivity.showProgress(false);
    }
}
