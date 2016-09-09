package com.intel.otc.brillo.examples;

import android.os.RemoteException;
import android.pio.Gpio;
import android.pio.PeripheralManagerService;
import android.system.ErrnoException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

/**
 * Created by Ap on 9/8/2016.
 */

public class FullGraphicSmartController {
    private PeripheralManagerService mService;
    int delaytime;
    int DEFAULTTIME;
    Gpio latchPin ;
    Gpio clockPin ;
    Gpio dataPin ;
    private List<String> gpios;

    String _latchPinName = "IO15"; //LCDRS
    String _clockPinName = "IO16"; //LCD4
    String _dataPinName = "IO17";  //LCDE
    public boolean started =false;

    public FullGraphicSmartController() {
        mService = new PeripheralManagerService();
        DEFAULTTIME = 80; // 80 ms default time
        delaytime = DEFAULTTIME;
    }



    private void delayMicroseconds(int msec) {
        try {
            TimeUnit.MICROSECONDS.sleep(msec);
        } catch (InterruptedException e) {
            // Ignore sleep interruption.
        }
    }

    private List<String> listGpios() throws RemoteException {
        // List available GPIOs:
        List<String> gpios = mService.getGpioList();
        if (gpios == null || gpios.isEmpty()) {
            Log.i(TAG, "No GPIO port available on this device.");
        } else {
            Log.i(TAG, "List of available GPIO ports: " + gpios);
        }
        return gpios;
    }

    void delayns()
    {
        delayMicroseconds(delaytime);
    }

    boolean isSet(byte value, int bit){
        return (value&(1<<bit))!=0;
    }

    void shiftOut(byte val) throws RemoteException, ErrnoException
    {

        for (byte i = 0; i < 8; i++)  {
            if((val & (1 << ((7 - i)))) == 0){
                dataPin.setValue(false);
            }else{
                dataPin.setValue(true);
            }

            clockPin.setValue(true);
            clockPin.setValue(false);
        }
    }

    void WriteByte(int dat)
    {
        try {
            latchPin.setValue(true);
            delayns();
            shiftOut( (byte) dat);
            latchPin.setValue(false);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (ErrnoException e) {
            e.printStackTrace();
        }




    }


    void WriteCommand(int CMD)
    {
        int H_data,L_data;
        H_data = CMD;
        H_data &= 0xf0;           //ÆÁ±ÎµÍ4Î»µÄÊý¾Ý
        L_data = CMD;             //xxxx0000¸ñÊ½
        L_data &= 0x0f;           //ÆÁ±Î¸ß4Î»µÄÊý¾Ý
        L_data <<= 4;             //xxxx0000¸ñÊ½
        WriteByte(0xf8);          //RS=0£¬Ð´ÈëµÄÊÇÖ¸Áî£»
        WriteByte(H_data);
        WriteByte(L_data);
    }


    void WriteData(int CMD)
    {
        int H_data,L_data;
        H_data = CMD;
        H_data &= 0xf0;           //ÆÁ±ÎµÍ4Î»µÄÊý¾Ý
        L_data = CMD;             //xxxx0000¸ñÊ½
        L_data &= 0x0f;           //ÆÁ±Î¸ß4Î»µÄÊý¾Ý
        L_data <<= 4;             //xxxx0000¸ñÊ½
        WriteByte(0xfa);          //RS=1£¬Ð´ÈëµÄÊÇÊý¾Ý
        WriteByte(H_data);
        WriteByte(L_data);
    }



    void Begin()
    {
        try {
        gpios = listGpios();

        if (gpios == null || gpios.isEmpty()) {
            Log.e(TAG, "Error gpios isEmpty");
            return;
        }

            _latchPinName = gpios.get(gpios.indexOf(_latchPinName));
            latchPin =  mService.openGpio(_latchPinName);
            latchPin.setDirection(latchPin.DIRECTION_OUT_INITIALLY_LOW);

            _clockPinName = gpios.get(gpios.indexOf(_clockPinName));
            clockPin =  mService.openGpio(_clockPinName);
            clockPin.setDirection(clockPin.DIRECTION_OUT_INITIALLY_HIGH);

            _dataPinName = gpios.get(gpios.indexOf(_dataPinName));
            dataPin =  mService.openGpio(_dataPinName);
            dataPin.setDirection(dataPin.DIRECTION_OUT_INITIALLY_HIGH);

        } catch (RemoteException | ErrnoException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        delayns();

        WriteCommand(0x30);
        WriteCommand(0x0c);
        WriteCommand(0x01);
        WriteCommand(0x06);

        Log.i(TAG, "Setup FullGraphicSmartController");
        started=true;
    }


    void CLEAR()
    {
        WriteCommand(0x30);
        WriteCommand(0x01);
        delayMicroseconds(500);          // this command takes a long time!
    }


    void DisplayString(int X,int Y,String data)
    {

        switch(X)
        {
            case 0:  Y|=0x80;break;

            case 1:  Y|=0x90;break;

            case 2:  Y|=0x88;break;

            case 3:  Y|=0x98;break;

            default: break;
        }

        WriteCommand(Y);
        for (byte ch : data.getBytes())
            WriteData(ch);
    }



    void DisplaySig(int M,int N,int sig)
    {
        switch(M)
        {
            case 0:  N|=0x80;break;

            case 1:  N|=0x90;break;

            case 2:  N|=0x88;break;

            case 3:  N|=0x98;break;

            default: break;
        }
        WriteCommand(N);
        WriteData(sig);
    }




    void DrawFullScreen(int p[])
    {
        int ygroup,x,y,i;
        int temp;
        int tmp;

        for(ygroup=0;ygroup<64;ygroup++)           //Ð´ÈëÒº¾§ÉÏ°ëÍ¼Ïó²¿·Ö
        {                           //Ð´Èë×ø±ê
            if(ygroup<32)
            {
                x=0x80;
                y=ygroup+0x80;
            }
            else
            {
                x=0x88;
                y=ygroup-32+0x80;
            }
            WriteCommand(0x34);        //Ð´ÈëÀ©³äÖ¸ÁîÃüÁî
            WriteCommand(y);           //Ð´ÈëyÖá×ø±ê
            WriteCommand(x);           //Ð´ÈëxÖá×ø±ê
            WriteCommand(0x30);        //Ð´Èë»ù±¾Ö¸ÁîÃüÁî
            tmp=ygroup*16;
            for(i=0;i<16;i++)
            {
                temp=p[tmp++];
                WriteData(temp);
            }
        }
        WriteCommand(0x34);        //Ð´ÈëÀ©³äÖ¸ÁîÃüÁî
        WriteCommand(0x36);        //ÏÔÊ¾Í¼Ïó
    }




}
