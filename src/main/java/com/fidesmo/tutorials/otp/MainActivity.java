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
public class MainActivity extends AppCompatActivity implements OnDiscoveredTagListener {

    private static final String TAG = "MainActivity";

    //Hardcoded ID of the OTP applet
    final static String OTP_AID = "A000000527210101";

    //Hardcoded TOTP key put onto the OTP applet beforehand
    private static final String TOTP_CODE_NAME = "FidesmoOTPTutorial:tutorial@fidesmo.com";

    // The TagDispatcher is responsible for managing the NFC for the activity
    private TagDispatcher tagDispatcher;

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
        // The first argument is the activity for which the NFC is managed
        // The second argument is the OnDiscoveredTagListener which is also
        // implemented by this activity
        // This means that the method tagDiscovered will be called whenever a new tag appears
        tagDispatcher = TagDispatcher.get(this, this);
        // Start listening on the NFC interface when the app gains focus.
        tagDispatcher.enableExclusiveNfc();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop listening on the NFC interface if the app loses focus
        tagDispatcher.disableExclusiveNfc();
    }

    /**
     * This method is called when a contactless device is detected at the NFC interface
     * @param intent the PendingIntent declared in onResume
     */
    @Override
    protected void onNewIntent(Intent intent) {
        tagDispatcher.interceptIntent(intent);
    }

    //Called whenever a tag is discovered on the NFC interface
    @Override
    public void tagDiscovered(Tag tag) {
        setMainMessage("Reading card...");
        /**
         * As the user might remove the card before we are done communicating with it we have to
         * make sure to catch that exception if it happens. We do this with a
         * try { ... } catch { ... }
         */
        try {
            /**
             * Create an IsoCard that we can communicate with. Nordpols version of IsoCard
             * AndroidCard has a get method to create an IsoCard from a tag.
             */
            IsoCard isoCard = AndroidCard.get(tag);
            //Send on the IsoCard
            communicateWithCard(isoCard);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    protected void communicateWithCard(IsoCard isoCard) {
        /**
         * The user might still remove the card before we are done with it, remember? Try catch
         * that functionality!
         */
        try {
            //Open a connection to the card
            isoCard.connect();
            /**
             * Send the first command to the card. This command has to be a select command to ensure
             * that we are communicating with the correct applet on the card. The OTP_AID is
             * the identifier of the OTP applet installed on the card. The Nordpol method
             * transceiveAndRequireOk() ensures that the command specified returned
             * success (OK_APDU) otherwise we continue to catch {}.
             */
            Apdu.transceiveAndRequireOk(Apdu.select(OTP_AID), isoCard);
            setMainMessage("Select OK");

            //Get a time stamp for creating a TOTP code APDU from
            long timeStamp = getTimeStamp();

            /**
             * Get the APDU command copied from Yubicos open source app for generating TOTP codes.
             * Specify that the TOTP code with the name TOTP_CODE_NAME is fetched. This "name"
             * should ideally by generated and not hardcoded and supplied when the TOTP token is
             * input into the OTP applet. Knowing this, let's hardcode it for the sake of this
             * tutorial with the value that we scanned/input into the applet before using some
             * other app like Yubico Authenticator.
             */
            byte[] totpCodeApdu = Otp.totpCodeApdu(TOTP_CODE_NAME, timeStamp);

            /**
             * Send the command to the IsoCard using Nordpol method transceiveAndGetResponse.
             * This method returns the response and automatically batches long messages.
             * The first parameter is the APDU command. The second parameter is the IsoCard to
             * communicate with. The third parameter is an optional and applet specific APDU
             * command for requesting more content from the applet. I found this on in the open
             * source code of Yubico Authenticator
             */
            byte[] rawTotpCode  = Apdu.transceiveAndGetResponse(totpCodeApdu, isoCard, Otp.SEND_REMAINING_APDU);
            /**
             * Check the result of the communication with the Nordpol method hasStatus. Supply the
             * the response APDU and the expected APDU as parameters. If the ending of the first
             * parameter matches the second parameter it is considered a match.
             */
            if(Apdu.hasStatus(rawTotpCode, Apdu.OK_APDU)){
                /**
                 * Decipher the TOTP code from the gibberish the response consists of (using
                 * Yubicos decipher methods).
                 */
                String totpCode = Otp.decipherTotpCode(rawTotpCode);
                //Append the TOTP code to the TextView
                setMainMessage("TOTP code is: " + totpCode);
            }

            //Close the card communication.
            isoCard.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long getTimeStamp() {
        //Get the current time + 10s to ensure that our generated code is valid at least for that long.
        return (System.currentTimeMillis() / 1000 + 10) / 30;
    }
}
