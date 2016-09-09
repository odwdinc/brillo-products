package com.intel.otc.brillo.examples;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Mp3Player implements Runnable,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,GPIOManager.OnButtonStateChangeListener {
    private static final String TAG = Mp3Player.class.getSimpleName();

    private Context mContext;
    private SongsManager sm;
    private int currentSongIndex = 0;
    private AudioManager am;
    private MediaPlayer mp;
    private  int volumeStep = 3;
    WifiManager.WifiLock wifiLock;
    private Visualizer mVisualizer;

    @Override
    public void onButtonStateChanged(GPIOManager.ButtonsState state) {
        switch (state){
            case Play:
                Play();
                break;
            case Stop:
                Stop();
                break;

            case Next:
                Next();
                break;
            case Back:
                Back();
                break;

            case VolUp:
                if(isMuted()){
                    unmute();
                }
                int volM = (getCurrentVolume() + volumeStep);
                if (volM < getMaxVolume()) {
                    setVolume(volM);
                }
                break;
            case VolDown:
                int volL = (getCurrentVolume() - volumeStep);
                if (volL > 0 ) {
                    setVolume(volL);
                }else{
                    mute();
                }
                break;

        }
    }

    public enum MediaState {
        Idle, Playing, Paused,Next,Back
    }
    private MediaState mState;
    private List<OnMediaStateChangeListener> mStateChangeListeners = new LinkedList<>();
    private int volumeBeforeMute;

    public interface OnMediaStateChangeListener {
        void onMediaStateChanged(MediaState state);
        void onVisualizerChanged(byte[] mBytes, boolean isFFT);
    }


    public Mp3Player(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        Log.d(TAG, "Initialize MP3 player...");
        // Set the current Thread priority as standard audio threads
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        // Get an instance of AudioManager for volume control
        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        sm = new SongsManager();
        mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
        mp.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        //mVisualizer = new Visualizer(mp.getAudioSessionId());
        //mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener()
        {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate)
            {
                updateVisualizer(bytes);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
                                         int samplingRate)
            {
                updateVisualizerFFT(bytes);
            }

        };
        //mVisualizer.setDataCaptureListener(captureListener,
        //        Visualizer.getMaxCaptureRate() / 2, true, true);


        setMediaState(MediaState.Idle);
    }


    private synchronized void updateVisualizer(byte[] bytes) {
        for (OnMediaStateChangeListener listener : mStateChangeListeners)
            listener.onVisualizerChanged(bytes, false);
    }

    private synchronized void updateVisualizerFFT(byte[] bytes) {
        for (OnMediaStateChangeListener listener : mStateChangeListeners)
            listener.onVisualizerChanged(bytes, true);
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        setMediaState(MediaState.Idle);
        if (++currentSongIndex < sm.size())
            Play();
        else currentSongIndex = 0;
        // Disable Visualizer
        //mVisualizer.setEnabled(false);
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        mp.start();
        setMediaState(MediaState.Playing);
        // Enabled Visualizer

        //mVisualizer.setEnabled(true);
    }

    public void Play() {
        switch (mState) {
            case Idle:
                playSong(currentSongIndex);
                break;
            case Playing:
                mp.pause();
                setMediaState(MediaState.Paused);
                wifiLock.release();
                break;
            case Paused:
                wifiLock.acquire();
                mp.start();
                setMediaState(MediaState.Playing);
                break;
        }
    }

    public void Stop() {
        if (mState != MediaState.Idle) {
            wifiLock.release();
            mp.stop();
            setMediaState(MediaState.Idle);
        }
    }


    public void Next() {
        Stop();
        setMediaState(MediaState.Next);
        currentSongIndex++;
        if (currentSongIndex > sm.size()){
            currentSongIndex = 0;
        }
        Play();
    }

    public void Back() {
        Stop();
        setMediaState(MediaState.Back);
        currentSongIndex--;
        if (currentSongIndex < 0 ) {
            currentSongIndex = sm.size();
        }
        Play();
    }

    public boolean isMuted() {
        return am.isStreamMute(AudioManager.STREAM_MUSIC);
    }

    public void mute() {
        if (!isMuted()) {
            volumeBeforeMute = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        }
    }

    public void unmute() {
        if (isMuted()) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeMute, 0);
        }
    }

    public int getMaxVolume() {
        return am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public int getCurrentVolume() {
        return am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public void setVolume(int volume) {
        am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    private synchronized void setMediaState(MediaState newState) {
        mState = newState;
        for (OnMediaStateChangeListener listener : mStateChangeListeners)
            listener.onMediaStateChanged(newState);
    }


    public void subscribeStateChangeNotification(OnMediaStateChangeListener listener) {
        mStateChangeListeners.add(listener);
    }

    public void unsubscribeStateChangeNotification(OnMediaStateChangeListener listener) {
        mStateChangeListeners.remove(listener);
    }

    public MediaState getCurrentState() {
        return mState;
    }

    public String getCurrentTitle() {
        return sm.getSongTitle(currentSongIndex);
    }

    private void playSong(int index) {
        try {
            wifiLock.acquire();
            Log.d(TAG, "Playing " + sm.getSongTitle(index));
            mp.reset();
            if(sm.isIndexUrl(index)) {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            mp.setDataSource(sm.getSongPath(index));
            mp.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }
}
