package com.wow.carlauncher.view.activity.set.setComponent;

import android.annotation.SuppressLint;
import android.widget.ArrayAdapter;

import com.inuker.bluetooth.library.search.SearchResult;
import com.wow.carlauncher.R;
import com.wow.carlauncher.common.CommonData;
import com.wow.carlauncher.common.TaskExecutor;
import com.wow.carlauncher.common.util.CommonUtil;
import com.wow.carlauncher.common.util.SharedPreUtil;
import com.wow.carlauncher.common.util.ThreadObj;
import com.wow.carlauncher.common.view.SetView;
import com.wow.carlauncher.ex.manage.ble.BleManage;
import com.wow.carlauncher.ex.manage.ble.BleSearchResponse;
import com.wow.carlauncher.ex.manage.toast.ToastManage;
import com.wow.carlauncher.ex.plugin.obd.ObdPlugin;
import com.wow.carlauncher.ex.plugin.obd.ObdProtocolEnum;
import com.wow.carlauncher.view.activity.set.SetActivity;
import com.wow.carlauncher.view.activity.set.SetBaseView;
import com.wow.carlauncher.view.activity.set.commonView.SetSingleSelectView;
import com.wow.carlauncher.view.dialog.ListDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;

import static com.wow.carlauncher.common.CommonData.SDATA_OBD_CONTROLLER;

/**
 * Created by 10124 on 2018/4/22.
 */

@SuppressLint("ViewConstructor")
public class SObdView extends SetBaseView {
    public SObdView(SetActivity activity) {
        super(activity);
    }

    @Override
    protected int getContent() {
        return R.layout.content_set_obd;
    }

    @Override
    public String getName() {
        return "OBD设置";
    }

    @BindView(R.id.sv_obd_select)
    SetView sv_obd_select;

    @BindView(R.id.sv_obd_impl_select)
    SetView sv_obd_impl_select;

    @BindView(R.id.sv_obd_disconnect)
    SetView sv_obd_disconnect;

    @Override
    protected void initView() {
        sv_obd_impl_select.setSummary("OBD使用的协议：" + ObdProtocolEnum.getById(SharedPreUtil.getInteger(SDATA_OBD_CONTROLLER, ObdProtocolEnum.YJ_TYB.getId())).getName());
        sv_obd_impl_select.setOnClickListener(new SetSingleSelectView<ObdProtocolEnum>(getActivity(), "请选择OBD使用的协议") {
            @Override
            public Collection<ObdProtocolEnum> getAll() {
                return Arrays.asList(CommonData.OBD_CONTROLLER);
            }

            @Override
            public ObdProtocolEnum getCurr() {
                return ObdProtocolEnum.getById(SharedPreUtil.getInteger(SDATA_OBD_CONTROLLER, ObdProtocolEnum.YJ_TYB.getId()));
            }

            @Override
            public boolean onSelect(ObdProtocolEnum setEnum) {
                SharedPreUtil.saveInteger(SDATA_OBD_CONTROLLER, setEnum.getId());
                sv_obd_impl_select.setSummary("OBD使用的协议：" + setEnum.getName());
                ObdPlugin.self().disconnect();
                return true;
            }
        });


        sv_obd_disconnect.setOnClickListener(view -> {
            SharedPreUtil.saveString(CommonData.SDATA_OBD_ADDRESS, null);
            SharedPreUtil.saveString(CommonData.SDATA_OBD_NAME, null);
            ObdPlugin.self().disconnect();
            ToastManage.self().show("OBD绑定已删除");
            sv_obd_select.setSummary("没有绑定蓝牙设备");
        });


        String address = SharedPreUtil.getString(CommonData.SDATA_OBD_ADDRESS);
        if (CommonUtil.isNotNull(address)) {
            sv_obd_select.setSummary("绑定了设备:" + SharedPreUtil.getString(CommonData.SDATA_OBD_NAME) + "  地址:" + address);
        } else {
            sv_obd_select.setSummary("没有绑定蓝牙设备");
        }
        sv_obd_select.setOnClickListener(view -> {
            final ThreadObj<ListDialog> listTemp = new ThreadObj<>();
            final List<SearchResult> devices = new ArrayList<>();
            BleManage.self().startSearch(new BleSearchResponse() {
                @Override
                public void onDeviceFounded(SearchResult device) {
                    boolean have = false;
                    for (SearchResult d : devices) {
                        if (d.getAddress().equals(device.getAddress())) {
                            have = true;
                            break;
                        }
                    }
                    if (!have) {
                        devices.add(device);
                    }
                    String[] items = new String[devices.size()];
                    for (int i = 0; i < items.length; i++) {
                        SearchResult bluetoothDevice = devices.get(i);
                        items[i] = bluetoothDevice.getName() + ":" + bluetoothDevice.getAddress();
                    }
                    TaskExecutor.self().autoPost(() -> listTemp.getObj().getListView().setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, items)));
                }
            });

            final ListDialog dialog = new ListDialog(getContext());
            dialog.setOnDismissListener(dialogInterface -> BleManage.self().stopSearch());

            dialog.setTitle("请选择一个蓝牙设备");
            dialog.show();
            listTemp.setObj(dialog);

            dialog.getListView().setOnItemClickListener((adapterView, view12, i, l) -> {
                dialog.dismiss();
                SearchResult device = devices.get(i);
                SharedPreUtil.saveString(CommonData.SDATA_OBD_ADDRESS, device.getAddress());
                SharedPreUtil.saveString(CommonData.SDATA_OBD_NAME, device.getName());
                sv_obd_select.setSummary("绑定了设备:" + device.getName() + "  地址:" + device.getAddress());
                ObdPlugin.self().disconnect();
            });
        });
    }
}
