package com.wow.carlauncher.common.ex;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.inuker.bluetooth.library.BluetoothClient;
import com.wow.frame.util.CommonUtil;

import org.xutils.x;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.wow.carlauncher.common.CommonData.TAG;

/**
 * Created by 10124 on 2018/4/22.
 */

public class BleManageEx extends ContextEx {
    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static BleManageEx instance = new BleManageEx();
    }

    public static BleManageEx self() {
        return SingletonHolder.instance;
    }

    private BleManageEx() {
        super();
    }

    public void init(Context context) {
        setContext(context);
        bluetoothClient = new BluetoothClient(context);

        bluetoothDevices = Collections.synchronizedList(new ArrayList<BluetoothDeviceEx>());
        BluetoothManager mBluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            bluetoothAdapter = mBluetoothManager.getAdapter();
        } else {
            ToastEx.self().show("设备不支持蓝牙");
        }

        bluetoothClient.closeBluetooth();
        bluetoothClient.openBluetooth();
    }

    private BluetoothClient bluetoothClient;

    public BluetoothClient client() {
        return bluetoothClient;
    }


    private final static int HELF_SECOND = 500;

    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDeviceEx> bluetoothDevices;

    private BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int index, byte[] bytes) {
            if (CommonUtil.isNull(bluetoothDevice.getName())) {
                return;
            }
            Log.d("scan", "onLeScan2: " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
            boolean have = false;
            for (BluetoothDeviceEx device : bluetoothDevices) {
                if (device.bluetoothDevice.getAddress().equals(bluetoothDevice.getAddress())) {
                    device.findTime = System.currentTimeMillis();
                    have = true;
                }
            }
            if (!have) {
                bluetoothDevices.add(new BluetoothDeviceEx(bluetoothDevice, System.currentTimeMillis()));
            }
        }
    };

    public void startSearch() {
        x.task().post(new Runnable() {
            @Override
            public void run() {
                if (bluetoothAdapter == null) {
                    ToastEx.self().show("设备不支持蓝牙");
                    return;
                }
                Log.d(TAG, "run: !!!!!!!!!!!!!!!!!!!!!开始扫描蓝牙");
                bluetoothAdapter.startLeScan(callback);
                startClearTimer();
            }
        });
    }

    public void stopSearch() {
        if (bluetoothAdapter == null) {
            return;
        }
        bluetoothAdapter.stopLeScan(callback);
        stopClearTimer();
    }

    public void forceCallBack() {
        callListener();
    }

    public void removeDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return;
        }
        for (BluetoothDeviceEx device : bluetoothDevices) {
            if (device.bluetoothDevice.getAddress().equals(bluetoothDevice.getAddress())) {
                bluetoothDevices.remove(device);
            }
            break;
        }
    }

    private Timer timer;

    private void startClearTimer() {
        stopClearTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clearDevice();
            }
        }, HELF_SECOND, HELF_SECOND);
    }

    private void stopClearTimer() {
        if (timer != null) {
            Log.e(TAG, "stopTimer: ");
            timer.cancel();
            timer = null;
        }
    }

    private void clearDevice() {
        List<BluetoothDeviceEx> willRemove = new ArrayList<>();
        for (BluetoothDeviceEx device : bluetoothDevices) {
            if (System.currentTimeMillis() - device.findTime > 3000) {
                willRemove.add(device);
            }
        }
        if (willRemove.size() > 0) {
            bluetoothDevices.removeAll(willRemove);
        }
        if (bluetoothDevices.size() > 0) {
            callListener();
        }
    }

    private void callListener() {
        Log.d(TAG, "callListener: 下发一下蓝牙设备列表");
        x.task().post(new Runnable() {
            @Override
            public void run() {
                final List<BluetoothDevice> devices = new ArrayList<>();
                for (BluetoothDeviceEx device : bluetoothDevices) {
                    devices.add(device.bluetoothDevice);
                }
                x.task().autoPost(new Runnable() {
                    @Override
                    public void run() {
                        for (BleDeviceSearchListener l : listeners) {
                            try {
                                l.deviceListChange(devices);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }

    private List<BleDeviceSearchListener> listeners = Collections.synchronizedList(new ArrayList<BleDeviceSearchListener>());

    public void addListener(BleDeviceSearchListener l) {
        listeners.add(l);
    }

    public void removeListener(BleDeviceSearchListener l) {
        listeners.remove(l);
    }

    class BluetoothDeviceEx {
        BluetoothDeviceEx(BluetoothDevice bluetoothDevice, Long findTime) {
            this.bluetoothDevice = bluetoothDevice;
            this.findTime = findTime;
        }

        private BluetoothDevice bluetoothDevice;
        private Long findTime;
    }

    public interface BleDeviceSearchListener {
        void deviceListChange(List<BluetoothDevice> bluetoothDevices);
    }
}