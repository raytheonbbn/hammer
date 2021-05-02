package audiomodem;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.atakmap.android.cot_utility.plugin.PluginLifecycle;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import audiomodem.jmodem.InputSampleStream;
import audiomodem.jmodem.Main;
import utils.ModemCotUtility;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by roman on 2/10/15.
 */
public class Receiver extends AsyncTask<Void, Double, Result> {

    final static int sampleRate = Config.sampleRate;

    // Hoping this will disable AGC and other pre-processing on the incoming audio...
    //final static int sourceId = MediaRecorder.AudioSource.VOICE_RECOGNITION;

    // for TRSS plug
    final static int sourceId = MediaRecorder.AudioSource.DEFAULT;

    final static String TAG = "Receiver";

    private ArrayList<short[]> buffers = new ArrayList<>();
    private final boolean debug = false;
    private double peak = 0;
    
    private volatile boolean stopFlag = false;
    protected AtomicBoolean cotReceived;
    private AudioRecord src;
    private ModemCotUtility modemCotUtility;

    public Receiver(AtomicBoolean cotReceived, ModemCotUtility modemCotUtility){

        this.cotReceived = cotReceived;
        this.modemCotUtility = modemCotUtility;
    }

    class InputStreamWrapper implements InputSampleStream {

        AudioRecord input;
        short[] buf;
        int offset;  // next short to be read from buf
        int size;  // number of shorts in buf

        public InputStreamWrapper(AudioRecord src, int bufSize) {
            input = src;
            buf = new short[bufSize];
        }

        @Override
        public double read() throws IOException {
            if (stopFlag) {
                throw new IOException("stopped");
            }
            while (offset >= size) {
                offset = 0;
                size = input.read(buf, 0, buf.length);
                updatePeak(buf, size);
                if (debug) {
                    buffers.add(buf.clone());
                }
            }
            double sample = buf[offset] / Config.scalingFactor;
            offset++;
            return sample;
        }
    }

    public void stop() {
        stopFlag = true;

        /*
        while(src == null){

        }

        src.stop();*/
    }

    private void updatePeak(short[] buf, int size) {
        if (size <= 0) {
            return;
        }
        int result = 0;
        for (int i = 0; i < size; i++) {
            result = Math.max(result, Math.abs(buf[i]));
        }
        publishProgress( result / Config.scalingFactor );
    }

    @Override
    protected Result doInBackground(Void... params) {

        int chanFormat = AudioFormat.CHANNEL_IN_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int bufSize = AudioRecord.getMinBufferSize(sampleRate, chanFormat, encoding);
        bufSize = Math.max(bufSize, 1024);

        Log.d(TAG, "bufSize: " + bufSize);

        src = new AudioRecord(sourceId, sampleRate, chanFormat, encoding, bufSize * 8);

        Log.d(TAG, "doInBackground: " );

        InputStreamWrapper input = new InputStreamWrapper(src, bufSize);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DataOutputStream os = null;
        if (debug) {
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/audio.raw";
            Log.d(TAG, "file: " + filePath);

            try {
                os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "file open failed", e);
                os = null;
            }
        }

        src.startRecording();
        try {
            Main.receive(input, output);
        } catch (Exception e) {
            if(e != null && e.getMessage() != null && e.getMessage().equalsIgnoreCase("stopped")) {
                Log.e(TAG, "receiver stopped");
            } else {
                Log.e(TAG, "receiver failed", e);
            }
            return new Result(null, e.getMessage());
        } finally {
            if(src != null) {
                src.stop();
                src.release();
            }
            publishProgress(0.0);
        }

        if (os != null) {
            try {
                for (short[] b : buffers) {
                    for (short s : b) {
                        os.writeShort(s);
                    }
                }
                os.flush();
                os.close();
            } catch (IOException e) {
                Log.e(TAG, "audio save failed", e);
                return new Result(null, e.getMessage());
            }
        }

        Log.i(TAG, "Received " + output.toByteArray().length + " bytes");

        try {
            if (modemCotUtility.usePSK) {
                Log.i(TAG, "PSK enabled");
                byte[] PSKhash;
                SharedPreferences sharedPref = PluginLifecycle.activity.getSharedPreferences("hammer-prefs", Context.MODE_PRIVATE);
                String psk = sharedPref.getString("PSKText", "");
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    PSKhash = digest.digest(psk.getBytes());
                } catch (Exception e) {
                    Log.d(TAG, "Decrypt PSK Hashing problem: " + e);
                    return null;
                }
                try {
                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    SecretKeySpec key = new SecretKeySpec(PSKhash, "AES");
                    // first 16 bytes are IV
                    byte[] iv = Arrays.copyOf(output.toByteArray(),16);
                    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                    // don't decrypt the IV
                    String str = new String(cipher.doFinal(Arrays.copyOfRange(output.toByteArray(), 16, output.toByteArray().length)), "UTF-8");
                    if (!str.contains("chat@@@")) {
                        // COT have problems
                        str = str.substring(str.indexOf("<?xml"),str.length());
                    }
                    return new Result(str, null);
                } catch (Exception e) {
                    Log.d(TAG, "Decrypt PSK problem: " + e);
                    return null;
                }
            } else {
                String str = new String(output.toByteArray(), "UTF-8");
                return new Result(str, null);
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "unicode decoding failed", e);
            return new Result(null, e.getMessage());
        }
    }
}

