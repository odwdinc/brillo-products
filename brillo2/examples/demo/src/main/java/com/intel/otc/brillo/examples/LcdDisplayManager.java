package com.intel.otc.brillo.examples;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import static android.os.SystemClock.sleep;

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
    private byte heart[] = {0x0, 0xa, 0x1f, 0x1f, 0xe, 0x4, 0x0, 0x0};

    private byte Step0[] = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x1F};
    private byte Step1[] = {0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x1F, 0x1F};
    private byte Step2[] = {0x0, 0x0, 0x0, 0x0, 0x0, 0x1F, 0x1F, 0x1F};
    private byte Step3[] = {0x0, 0x0, 0x0, 0x0, 0x1F, 0x1F, 0x1F, 0x1F};
    private byte Step4[] = {0x0, 0x0, 0x0, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F};
    private byte Step5[] = {0x0, 0x0, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F};
    private byte Step6[] = {0x0, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F, 0x1F};
    private byte Step7[] = {0x0, 0x1F, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0};

    public LcdDisplayManager(Mp3Player player) {
        mp3Player = player;
        lcd = new LcdRgbBacklight();
        lcd.createChar(0,heart);
        lcd.createChar(1,Step1);
        lcd.createChar(2,Step2);
        lcd.createChar(3,Step3);
        lcd.createChar(4,Step4);
        lcd.createChar(5,Step5);
        lcd.createChar(6,Step6);
        lcd.createChar(7,Step7);
    }

    @Override
    public void run() {
        Log.d(TAG, "LCD display manager started");

        lcd.begin(16, 2, LcdRgbBacklight.LCD_5x10DOTS);
        lcd.write("Hello Traveler");
        lcd.setCursor(0, 1);
        lcd.write(" Intel ");
        lcd.write((byte) 0);
        lcd.write(" Brillo!");
        sleep(2000);

        lcd.createChar(0,Step0);

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

    int mDivisions =16;

    @Override
    public void onVisualizerChanged(byte[] mBytes, boolean isFFT) {
        if (isFFT) {
            for (int i = 0; i < mBytes.length / mDivisions; i++) {
                byte rfk = mBytes[mDivisions * i];
                byte ifk = mBytes[mDivisions * i + 1];
                float magnitude = (rfk * rfk + ifk * ifk);
                int dbValue = (int) (10 * Math.log10(magnitude));
                lcd.setCursor(i, 1);
                lcd.write((byte) (dbValue * 2 - 10));
            }
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
