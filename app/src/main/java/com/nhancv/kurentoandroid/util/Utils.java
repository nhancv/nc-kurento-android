package com.nhancv.kurentoandroid.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by nhancao on 9/18/16.
 */
public class Utils {
    private static final String TAG = Utils.class.getName();


    /**
     * Check network is connected or not
     *
     * @param context
     * @return true if connected otherwise return false
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Validate input (EditText view)
     *
     * @param input EditText
     * @return true if input view not empty
     */
    public static boolean validateInput(EditText input) {
        return validateInput(input, false);
    }

    /**
     * Validate input with option isEmail (EditText view)
     *
     * @param input EditText
     * @return true if input view not empty
     */
    public static boolean validateInput(EditText input, boolean isEmail) {
        input.setError(null);
        String name = input.getText().toString().trim();
        boolean valid = true;
        EditText focusView = null;
        String errorMsg = null;

        if (TextUtils.isEmpty(name)) {
            valid = false;
            focusView = input;
            errorMsg = "This input is required";
        } else if (isEmail && !isEmailValid(name)) {
            valid = false;
            focusView = input;
            errorMsg = "Email is not valid";
        }

        if (!valid) {
            focusView.setError(errorMsg);
            focusView.requestFocus();
        }

        return valid;
    }

    /**
     * Check mail is valid
     *
     * @param email
     * @return true if mail is valid
     */
    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Hide soft keyboard with View
     *
     * @param editText
     */
    public static void hideKeyboard(final View editText) {
        if (editText != null) {
            // Delay some time to get focus(error occurs on HTC Android)
            editText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            }, 50);
        }
    }

    /**
     * Restart application from context
     *
     * @param context
     */
    public static void restartApplication(Context context) {
        try {
            //check if the context is given
            if (context != null) {
                //fetch the package manager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = context.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            context.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(context, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(TAG, "Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, PM null");
                }
            } else {
                Log.e(TAG, "Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Was not able to restart application");
        }
    }

    /**
     * Get density of screen from context
     *
     * @param context
     * @return float
     */
    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }
}
