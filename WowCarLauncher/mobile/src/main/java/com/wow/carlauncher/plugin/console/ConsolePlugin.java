package com.wow.carlauncher.plugin.console;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wow.carlauncher.R;
import com.wow.carlauncher.activity.LockActivity;
import com.wow.carlauncher.common.console.ConsoleManage;
import com.wow.carlauncher.common.util.AppUtil;
import com.wow.carlauncher.plugin.IPlugin;
import com.wow.carlauncher.plugin.PluginManage;

/**
 * Created by 10124 on 2017/11/4.
 */

public class ConsolePlugin implements IPlugin, View.OnClickListener {
    public final static String TAG = "ConsolePlugin";
    private PluginManage pluginManage;
    private Context context;
    private View launcherView;
    private TextView launcherWifi;

    public ConsolePlugin(Context context, PluginManage pluginManage) {
        this.pluginManage = pluginManage;
        this.context = context;

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mReceiver, mFilter);
    }

    private void initLauncherView(View launcherView) {
        launcherView.findViewById(R.id.btn_vu).setOnClickListener(this);
        launcherView.findViewById(R.id.btn_vd).setOnClickListener(this);
        launcherView.findViewById(R.id.btn_jy).setOnClickListener(this);
        launcherView.findViewById(R.id.btn_close_screen).setOnClickListener(this);

        launcherWifi = (TextView) launcherView.findViewById(R.id.tv_wifi);
        refreshWifi();
    }

    @Override
    public View getLauncherView() {
        if (launcherView == null) {
            launcherView = View.inflate(context, R.layout.plugin_controller_launcher, null);
            initLauncherView(launcherView);
        }
        return launcherView;
    }

    @Override
    public View getPopupView() {
        return null;
    }

    @Override
    public void destroy() {
        if (launcherView.getParent() != null) {
            ((ViewGroup) launcherView.getParent()).removeView(launcherView);
        }

        context.unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_close_screen: {
                pluginManage.getCurrentActivity().startActivity(new Intent(pluginManage.getCurrentActivity(), LockActivity.class));
                break;
            }
            case R.id.btn_vu: {
                ConsoleManage.self().incVolume();
                break;
            }
            case R.id.btn_vd: {
                ConsoleManage.self().decVolume();
                break;
            }
            case R.id.btn_jy: {
                ConsoleManage.self().mute();
                break;
            }
        }
    }

    private void refreshWifi() {
        AppUtil.NetWorkState netWorkState = AppUtil.getNetWorkState(context);
        if (launcherWifi != null) {
            switch (netWorkState) {
                case NETWORKSTATE_NONE: {
                    launcherWifi.setText("无线连接：无网络");
                    break;
                }
                case NETWORKSTATE_MOBILE: {
                    launcherWifi.setText("无线连接：移动网络");
                    break;
                }
                case NETWORKSTATE_WIFI: {
                    launcherWifi.setText("无线连接：" + AppUtil.getConnectWifiSsid(context));
                    break;
                }
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                refreshWifi();
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        break;
                }
            }
        }
    };
}
