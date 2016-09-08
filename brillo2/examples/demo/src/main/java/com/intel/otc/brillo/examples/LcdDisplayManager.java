package com.intel.otc.brillo.examples;

import android.util.Log;

import java.util.concurrent.TimeUnit;

public class LcdDisplayManager implements Runnable,
        Mp3Player.OnMediaStateChangeListener,
        OcResourceBrightness.OnBrightnessChangeListener
{
    private static final String TAG = LcdDisplayManager.class.getSimpleName();
    private static final int Service_Interval_In_Msec = 500;

    private Mp3Player mp3Player;
    private int timeEscapedInMsec = 0;
    private LcdRgbBacklight lcd;
    private int LCDTrackPos =0;

    public LcdDisplayManager(Mp3Player player) {
        mp3Player = player;
        lcd = new LcdRgbBacklight();
    }

    @Override
    public void run() {
        Log.d(TAG, "LCD display manager started");

        lcd.begin(16, 2, LcdRgbBacklight.LCD_5x10DOTS);
        boolean showTimeEscaped = false;
        while (true)
            try {
                TimeUnit.MILLISECONDS.sleep(Service_Interval_In_Msec);
                Mp3Player.MediaState state = mp3Player.getCurrentState();
                switch (state) {
                    case Idle:
                        continue;
                    case Playing:
                        timeEscapedInMsec += Service_Interval_In_Msec;
                        showTimeEscaped = true;
                        break;
                    case Paused:
                        showTimeEscaped = !showTimeEscaped;
                        break;
                }
                int second = timeEscapedInMsec / 1000;
                int minute = second / 60;
                second %= 60;
                int hour = minute / 60;
                minute %= 60;

                display(0,0, showTimeEscaped? (toLeadingZeroNumber(minute) + ":" + toLeadingZeroNumber(second)) : "     ");
                display(1,0, scrollingText(mp3Player.getCurrentTitle()));
                display(0,13, mp3Player.getCurrentVolume()+"%");

            } catch (InterruptedException e) {
                // Ignore sleep nterruption
            }
    }

    private String scrollingText(String track_){
        String LCDTrack = track_;

        if(track_.length() > 16){
            if (LCDTrackPos + 16 <= track_.length()){
                LCDTrack = track_.substring(LCDTrackPos,16);
            }else if(LCDTrackPos  <= track_.length()){
                int firstCount = track_.length() - LCDTrackPos;
                int nextCount = 13 - firstCount;
                LCDTrack = track_.substring(LCDTrackPos,firstCount);
                LCDTrack =LCDTrack +"   " + track_.substring (0,nextCount);
            }else {
                LCDTrackPos =0;
            }
            LCDTrackPos = LCDTrackPos+2;
        }
        return  LCDTrack;
    }

    @Override
    public void onMediaStateChanged(Mp3Player.MediaState state) {
        lcd.setCursor(0, 5);
        switch (state) {
            case Idle:
                lcd.write("Idle    ");
                timeEscapedInMsec = 0;
                break;
            case Playing:
                lcd.write("Playing ");
                break;
            case Paused:
                lcd.write("Paused  ");
                break;
        }
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        if (0 <= brightness && brightness <= 100) {
            int c = brightness * 255 / 100;
            lcd.setRGB(c, c, c);
        }
    }

    private void display(int col, int row, String s) {
        lcd.setCursor(col, row);
        lcd.write(s);
    }

    private String toLeadingZeroNumber(int n) {
        n %= 100;
        return ((n < 10)? "0" : "") + n;
    }
}
