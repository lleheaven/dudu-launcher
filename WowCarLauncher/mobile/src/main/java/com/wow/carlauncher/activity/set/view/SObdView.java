package com.wow.carlauncher.activity.set.view;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.wow.carlauncher.R;
import com.wow.carlauncher.common.CommonData;
import com.wow.carlauncher.common.ex.BleManage;
import com.wow.carlauncher.common.ex.ToastManage;
import com.wow.carlauncher.common.ex.event.BleEventDeviceChange;
import com.wow.carlauncher.common.view.SetView;
import com.wow.carlauncher.dialog.ListDialog;
import com.wow.carlauncher.plugin.obd.ObdPlugin;
import com.wow.carlauncher.plugin.obd.ObdProtocolEnum;
import com.wow.frame.util.CommonUtil;
import com.wow.frame.util.SharedPreUtil;
import com.wow.frame.util.ThreadObj;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

import static com.wow.carlauncher.common.CommonData.SDATA_OBD_CONTROLLER;

/**
 * Created by 10124 on 2018/4/22.
 */

public class SObdView extends FrameLayout {
    public static final ObdProtocolEnum[] ALL_OBD_CONTROLLER = {ObdProtocolEnum.YJ_TYB};

    public SObdView(@NonNull Context context) {
        super(context);
        initView();
    }

    public SObdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    @ViewInject(R.id.sv_obd_select)
    private SetView sv_obd_select;

    @ViewInject(R.id.sv_obd_impl_select)
    private SetView sv_obd_impl_select;

    @ViewInject(R.id.sv_obd_disconnect)
    private SetView sv_obd_disconnect;

    private void initView() {
        LinearLayout view = (LinearLayout) View.inflate(getContext(), R.layout.content_set_obd, null);
        this.addView(view, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        x.view().inject(this);

        ObdProtocolEnum p1 = ObdProtocolEnum.getById(SharedPreUtil.getSharedPreInteger(SDATA_OBD_CONTROLLER, ObdProtocolEnum.YJ_TYB.getId()));
        sv_obd_impl_select.setSummary("OBD使用的协议：" + p1.getName());
        sv_obd_impl_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ObdProtocolEnum p = ObdProtocolEnum.getById(SharedPreUtil.getSharedPreInteger(SDATA_OBD_CONTROLLER, ObdProtocolEnum.YJ_TYB.getId()));
                final ObdProtocolEnum[] show = ALL_OBD_CONTROLLER;
                String[] items = new String[show.length];
                int select = 0;
                for (int i = 0; i < show.length; i++) {
                    items[i] = show[i].getName();
                    if (show[i].equals(p)) {
                        select = i;
                    }
                }
                final ThreadObj<Integer> obj = new ThreadObj<>(select);
                AlertDialog dialog = new AlertDialog.Builder(getContext()).setTitle("请选择协议").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreUtil.saveSharedPreInteger(SDATA_OBD_CONTROLLER, show[obj.getObj()].getId());
                        sv_obd_impl_select.setSummary("OBD使用的协议：" + show[obj.getObj()].getName());

                        ObdPlugin.self().disconnect();
                        BleManage.self().forceCallBack();
                    }
                }).setSingleChoiceItems(items, select, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        obj.setObj(which);
                    }
                }).create();
                dialog.show();
            }
        });


        sv_obd_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreUtil.saveSharedPreString(CommonData.SDATA_OBD_ADDRESS, null);
                SharedPreUtil.saveSharedPreString(CommonData.SDATA_OBD_NAME, null);
                ObdPlugin.self().disconnect();
                ToastManage.self().show("OBD绑定已删除");
            }
        });


        String address = SharedPreUtil.getSharedPreString(CommonData.SDATA_OBD_ADDRESS);
        if (CommonUtil.isNotNull(address)) {
            sv_obd_select.setSummary("绑定了设备:" + SharedPreUtil.getSharedPreString(CommonData.SDATA_OBD_NAME) + "  地址:" + address);
        } else {
            sv_obd_select.setSummary("没有绑定蓝牙设备");
        }
        sv_obd_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final ThreadObj<ListDialog> listTemp = new ThreadObj<>();

                final List<BluetoothDevice> devices = new ArrayList<>();

                final Object listener = new Object() {
                    @Subscribe
                    public void onEventMainThread(BleEventDeviceChange event) {
                        devices.clear();
                        devices.addAll(event.getBluetoothDevices());

                        String[] items = new String[devices.size()];
                        for (int i = 0; i < items.length; i++) {
                            BluetoothDevice bluetoothDevice = devices.get(i);
                            items[i] = bluetoothDevice.getName() + ":" + bluetoothDevice.getAddress();
                        }
                        listTemp.getObj().getListView().setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, items));
                    }
                };
                EventBus.getDefault().register(listener);

                final ListDialog dialog = new ListDialog(getContext());
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        EventBus.getDefault().unregister(listener);
                    }
                });

                dialog.setTitle("请选择一个蓝牙设备");
                dialog.show();
                listTemp.setObj(dialog);

                dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        dialog.dismiss();
                        BluetoothDevice device = devices.get(i);
                        BleManage.self().removeDevice(device);
                        SharedPreUtil.saveSharedPreString(CommonData.SDATA_OBD_ADDRESS, device.getAddress());
                        SharedPreUtil.saveSharedPreString(CommonData.SDATA_OBD_NAME, device.getName());

                        sv_obd_select.setSummary("绑定了设备:" + device.getName() + "  地址:" + device.getAddress());

                        BleManage.self().forceCallBack();
                    }
                });
                BleManage.self().forceCallBack();
            }
        });
    }
}