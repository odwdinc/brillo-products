package com.intel.otc.brillo.examples;

import android.os.RemoteException;
import android.pio.Gpio;
import android.pio.PeripheralManagerService;
import android.pio.PioInterruptEventListener;
import android.system.ErrnoException;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import static com.intel.otc.brillo.examples.GPIOManager.ButtonsState.Back;
import static com.intel.otc.brillo.examples.GPIOManager.ButtonsState.Next;
import static com.intel.otc.brillo.examples.GPIOManager.ButtonsState.Play;
import static com.intel.otc.brillo.examples.GPIOManager.ButtonsState.Stop;
import static com.intel.otc.brillo.examples.GPIOManager.ButtonsState.VolDown;
import static com.intel.otc.brillo.examples.GPIOManager.ButtonsState.Volup;

/**
 * Created by Ap on 9/8/2016.
 */
public class GPIOManager implements Runnable {
    private static final String TAG = "GPIOManager";

    private PeripheralManagerService mService;

    /*
    // uses the grove shild for pin mapping.
    private static final String Play_GPIO_PORT = "IO6";
    private static final String Stop_GPIO_PORT = "IO8";

    private static final String Next_GPIO_PORT = "IO9";
    private static final String Back_GPIO_PORT = "IO7";
    private static final String Volup_GPIO_PORT = "IO5";
    private static final String VolDown_GPIO_PORT = "IO4";
    */

    private static final String Play_GPIO_PORT = "IO9";
    private static final String Stop_GPIO_PORT = "IO10";

    private static final String Next_GPIO_PORT = "IO13";
    private static final String Back_GPIO_PORT = "IO8";

    private static final String Volup_GPIO_PORT = "IO12";
    private static final String VolDown_GPIO_PORT = "IO11";



    private  Gpio Play_gpio;
    private  Gpio Stop_gpio;

    private  Gpio Next_gpio;
    private  Gpio Back_gpio;

    private  Gpio Volup_gpio;
    private  Gpio VolDown_gpio;
    private List<String> gpios;

    private boolean runing =true;

    public enum ButtonsState {
        Play, Stop, Next, Back, Volup, VolDown
    }

    public interface OnButtonStateChangeListener {
        void onButtonStateChanged(ButtonsState state);
    }
    private ButtonsState mState;

    private List<OnButtonStateChangeListener> bStateChangeListeners = new LinkedList<>();

    public GPIOManager() {
        mService = new PeripheralManagerService();

        try {
            gpios = listGpios();

            if (gpios == null || gpios.isEmpty()) {
                return;
            }

            Play_gpio = setupGPIO(Play_GPIO_PORT);
            Stop_gpio = setupGPIO(Stop_GPIO_PORT);

            Next_gpio = setupGPIO(Next_GPIO_PORT);
            Back_gpio = setupGPIO(Back_GPIO_PORT);

            Volup_gpio = setupGPIO(Volup_GPIO_PORT);
            VolDown_gpio = setupGPIO(VolDown_GPIO_PORT);


        } catch (RemoteException | ErrnoException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

    }

    private Gpio setupGPIO(String _portName) throws RemoteException, ErrnoException {
        Gpio _port;
        _portName = gpios.get(gpios.indexOf(_portName));
        _port =  mService.openGpio(_portName);
        _port.setDirection(Gpio.DIRECTION_IN);
        _port.setEdgeTriggerType(_port.EDGE_FALLING);
        _port.registerInterruptHandler(GPIOlistener);
        return _port;
    }

    public void closePorts(){
        Play_gpio.close();
        Stop_gpio.close();

        Next_gpio.close();
        Back_gpio.close();

        Volup_gpio.close();
        VolDown_gpio.close();
    }

    private PioInterruptEventListener GPIOlistener =new PioInterruptEventListener(){

        @Override
        public boolean onInterruptEvent(String name) {
            try {
                if(Play_gpio.getValue()) {
                    setButtonState(Play);
                }
                if(Stop_gpio.getValue()) {
                    setButtonState(Stop);
                }


                if(Next_gpio.getValue()) {
                    setButtonState(Next);
                }
                if(Back_gpio.getValue()) {
                    setButtonState(Back);
                }


                if(Volup_gpio.getValue()) {
                    setButtonState(Volup);
                }
                if(VolDown_gpio.getValue()) {
                    setButtonState(VolDown);
                }
            } catch (RemoteException | ErrnoException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
            return true;
        }

        @Override
        public void onError(String name, int errorCode) {

        }
    };


    @Override
    public void run() {
        Log.i(TAG, "Executing task GPIOManager");
        while (runing){

        }
        closePorts();
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

    private synchronized void setButtonState(ButtonsState newState) {
        mState = newState;
        for (OnButtonStateChangeListener listener : bStateChangeListeners)
            listener.onButtonStateChanged(newState);
    }

    public void subscribeStateChangeNotification(OnButtonStateChangeListener listener) {
        bStateChangeListeners.add(listener);
    }

    public void unsubscribeStateChangeNotification(OnButtonStateChangeListener listener) {
        bStateChangeListeners.remove(listener);
    }
}