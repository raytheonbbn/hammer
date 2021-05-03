package audiomodem;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.util.Base64;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;

import utils.ModemCotUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import audiomodem.jmodem.Main;
import audiomodem.jmodem.OutputSampleStream;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;
import com.atakmap.android.maps.MapView;

public class Sender extends AsyncTask<String, Double, Void> {
    private ModemCotUtility modemCotUtility;
    public Sender(ModemCotUtility modemCotUtility){
        this.modemCotUtility = modemCotUtility;
    }

    static class OutputBuffer implements OutputSampleStream {

        final ByteArrayOutputStream out;
        final DataOutputStream stream;

        public OutputBuffer() {
            out = new ByteArrayOutputStream();
            stream = new DataOutputStream(out);
        }

        @Override
        public void write(double value) throws IOException {
            short sample = (short) (Config.scalingFactor * value);
            stream.writeShort(sample);
        }

        public short[] samples() {
            Log.i(TAG, "TEST: " + out.toByteArray().length + " : " + ByteBuffer.wrap(out.toByteArray()).limit() + " : " +  ByteBuffer.wrap(out.toByteArray()).asShortBuffer().limit() + " : " +  ByteBuffer.wrap(out.toByteArray()).asShortBuffer().capacity());
            ShortBuffer b = ByteBuffer.wrap(out.toByteArray()).asShortBuffer();
            short[] result = new short[b.capacity()];
            b.get(result);
            return result;
        }
    }

    final static int sampleRate = Config.sampleRate;

    final static int streamType = AudioManager.STREAM_MUSIC;
    final static String TAG = "Sender";

    @Override
    protected Void doInBackground(String... params) {
        final int chanFormat = AudioFormat.CHANNEL_OUT_MONO;
        final int encoding = AudioFormat.ENCODING_PCM_16BIT;
        final int mode = AudioTrack.MODE_STATIC;
        final int bufSize = AudioTrack.getMinBufferSize(sampleRate, chanFormat, encoding);

        OutputBuffer buf = new OutputBuffer();
        byte[] data = params[0].getBytes();
        // strip the padding
        String encodedString = params[0].substring(modemCotUtility.padding.length());
        ByteBuffer payload = ByteBuffer.allocate(0);

        if (modemCotUtility.usePSK) {
            Log.i(TAG, "PSK enabled");
            byte[] PSKhash, cipherText;
            SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
            String psk = sharedPref.getString("PSKText", "atakatak");
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                PSKhash = digest.digest(psk.getBytes("UTF-8"));
            } catch (Exception e) {
                Log.d(TAG, "Encrypt PSK Hashing problem: " + e);
                return null;
            }
            try {
                byte[] iv = new byte[16];
                new SecureRandom().nextBytes(iv);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec key = new SecretKeySpec(PSKhash, "AES");
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
                cipherText = cipher.doFinal(encodedString.getBytes("UTF-8"));

                // set the iv+cipherText as the payload
                payload = ByteBuffer.allocate(iv.length + cipherText.length);
                payload.put(iv);
                payload.put(cipherText);

                // if using PSK but not TNC, setup for the audiomodem
                if (!modemCotUtility.useTNC) {
                    data = payload.array();
                }
            } catch (Exception e) {
                Log.d(TAG, "Encrypt PSK problem: " + e);
                return null;
            }
        }

        if (modemCotUtility.useTNC) {

            if (!modemCotUtility.aprsdroid_running) {
                // make sure APRSDroid is running
                Intent i = new Intent("org.aprsdroid.app.SERVICE").setPackage("org.aprsdroid.app");
                PluginLifecycle.activity.getApplicationContext().startForegroundService(i);
            }

            if (modemCotUtility.usePSK) {
                encodedString = Base64.encodeToString(payload.array(), Base64.NO_WRAP);
            }

            modemCotUtility.stopListener();

            // send off to TNC
            Intent i = new Intent("org.aprsdroid.app.SEND_PACKET").setPackage("org.aprsdroid.app");
            // ")" == APRSTypes.T_ITEM
            // "CALLSIGN!" to make it through javAPRSlib parseBody()
            i.putExtra("data", ")" + MapView.getMapView().getDeviceCallsign() + "!".concat(encodedString));
            PluginLifecycle.activity.getApplicationContext().startForegroundService(i);

            modemCotUtility.startListener();

            return null;
        }

        Log.i(TAG, "Sending " + data.length + " bytes");
        Log.i(TAG, "Buffer size: " + bufSize);

        try {
            Main.send(new ByteArrayInputStream(data), buf);
        } catch (IOException e) {
            Log.e(TAG, "sending data failed", e);
            return null;
        }

        short[] samples = buf.samples();

        Log.i(TAG, "Effective buffer size in bytes: " + (Math.max(samples.length, bufSize) * 2));
        AudioTrack dst = new AudioTrack(
                streamType,
                sampleRate,
                chanFormat,
                encoding,
                Math.max(samples.length, bufSize) * 2,
                mode
        );
        int n = dst.write(samples, 0, samples.length);
        double duration = ((double) n) / sampleRate;
        Log.d(TAG, String.format("playing %d samples (%f seconds)", n, duration));

        if(modemCotUtility.useSlowVox) {
            Log.i(TAG, String.format("slowVox: Enabled"));
            SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
            byte[] beep_bytes = Base64.decode(sharedPref.getString("b64_beep_bytes", ""), Base64.DEFAULT);
            Log.d(TAG, String.format("beep_bytes.length = %d", beep_bytes.length));
            AudioTrack beep = new AudioTrack(
                    streamType,
                    sampleRate,
                    chanFormat,
                    encoding,
                    beep_bytes.length,
                    mode
            );
            if (beep_bytes.length != beep.write(beep_bytes, 0, beep_bytes.length)) {
                Log.w(TAG, "beep lengths don't match");
            }
            beep.play();
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                Log.e(TAG, String.format("slowVox: sleep failed"));
            } finally {
                beep.stop();
                beep.release();
            }
        }

        dst.play();
        try {
            final double dt = 0.1;
            for (double t = 0; t < duration; t += dt) {
                publishProgress(t / duration);
                Thread.sleep((long) (dt * 1000));
            }
        } catch (InterruptedException e) {
            // nothing to do
        } finally {
            publishProgress(1.0);
        }
        dst.stop();
        dst.release();

        modemCotUtility.startListener();
        return null;

    }

}
