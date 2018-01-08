package com.fidesmo.tutorials.otp;

import android.content.Intent;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.UiThread;

import java.io.IOException;

import nordpol.Apdu;
import nordpol.IsoCard;
import nordpol.android.AndroidCard;
import nordpol.android.TagDispatcher;
import nordpol.android.OnDiscoveredTagListener;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Hardcoded ID of the OTP applet
    final static String OTP_AID = "A000000527210101";

    //Hardcoded TOTP key put onto the OTP applet beforehand
    private static final String TOTP_CODE_NAME = "FidesmoOTPTutorial:tutorial@fidesmo.com";

    // UI elements
    @ViewById
    TextView mainText;

    //Two methods for setting the UI (on UI thread, because, threading...)
    @UiThread
    void setMainMessage(int resource) {
        setMainMessage(getString(resource));
    }

    @UiThread
    void setMainMessage(String text) {
        String oldString = mainText.getText().toString();
        mainText.setText(oldString + "\n" + text);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {

    }

    private static long getTimeStamp() {
        //Get the current time + 10s to ensure that our generated code is valid at least for that long.
        return (System.currentTimeMillis() / 1000 + 10) / 30;
    }
}
