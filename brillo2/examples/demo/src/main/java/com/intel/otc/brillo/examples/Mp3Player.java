package com.intel.otc.brillo.examples;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
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
    public int currentSongIndex = 0;
    private AudioManager am;
    private MediaPlayer mp;
    private  int volumeStep = 1;
    private boolean autoNext =false;


    @Override
    public void onButtonStateChanged(GPIOManager.ButtonsState state) {
        Log.d(TAG,"onButtonStateChanged");
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
        if(autoNext) {
            if (++currentSongIndex < sm.size()) {
                Log.d(TAG, "onCompletion()");
                Play();
            } else {
                currentSongIndex = 0;
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        mp.start();
        setMediaState(MediaState.Playing);
    }

    //the current position in milliseconds
    public int getCurrentTrackPosition(){
        return  mp.getCurrentPosition();
    }

    public void Play() {
        autoNext = true;
        switch (mState) {
            case Idle:
                playSong(currentSongIndex);
                break;
            case Playing:
                mp.pause();
                setMediaState(MediaState.Paused);
                break;
            case Paused:
                mp.start();
                setMediaState(MediaState.Playing);
                break;
        }
    }

    public void Stop() {
        autoNext =false;
        if (mState != MediaState.Idle) {
            mp.stop();
            setMediaState(MediaState.Idle);
        }
    }


    public void Next() {
        boolean au = autoNext;
        Stop();
        autoNext = au;
        //setMediaState(MediaState.Next);
        currentSongIndex++;
        if (currentSongIndex > sm.size()-1){
            currentSongIndex = 0;
        }
        if(autoNext == true){
            Play();
        }

    }

    public void Back() {
        boolean au = autoNext;
        Stop();
        autoNext = au;
        //setMediaState(MediaState.Back);
        currentSongIndex--;
        if (currentSongIndex < 0 ) {
            currentSongIndex = sm.size()-1;
        }
        if(autoNext == true){
            Play();
        }
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
