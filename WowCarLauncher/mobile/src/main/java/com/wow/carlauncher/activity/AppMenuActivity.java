package com.wow.carlauncher.activity;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.wow.carlauncher.R;
import com.wow.carlauncher.activity.adapter.AllAppAdapter;
import com.wow.carlauncher.common.AppInfoManage;
import com.wow.carlauncher.common.BaseActivity;
import com.wow.carlauncher.common.CommonData;
import com.wow.carlauncher.common.util.AppUtil.AppInfo;
import com.wow.carlauncher.common.util.CommonUtil;
import com.wow.carlauncher.common.util.SharedPreUtil;
import com.wow.carlauncher.event.LauncherAppRefreshEvent;
import com.wow.carlauncher.plugin.pevent.PEventMusicInfoChange;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

public class AppMenuActivity extends BaseActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private AllAppAdapter adapter;

    @ViewInject(R.id.gridview)
    private GridView gridview;

    private PackageManager pm;

    @Override
    public void init() {
        setContent(R.layout.activity_all_app);
        adapter = new AllAppAdapter(this);
        pm = getPackageManager();
        EventBus.getDefault().register(this);
    }

    @Override
    public void initView() {
        setTitle("全部应用");

        gridview.setAdapter(adapter);
        gridview.setOnItemClickListener(this);
        gridview.setOnItemLongClickListener(this);

        setBackground(WallpaperManager.getInstance(this).getDrawable());
    }

    @Override
    public void loadData() {
        showLoading("加载中。。。。", new ProgressInterruptListener() {
            @Override
            public void onProgressInterruptListener(ProgressDialog progressDialog) {
                finish();
            }
        });
        x.task().run(new Runnable() {
            @Override
            public void run() {
                adapter.clear();
                final List<AppInfo> appInfos = AppInfoManage.self().getAppInfos();
                String selectapp = SharedPreUtil.getSharedPreString(CommonData.SDATA_HIDE_APPS);
                List<AppInfo> hides = new ArrayList<>();
                for (AppInfo appInfo : appInfos) {
                    if (selectapp.contains("[" + appInfo.packageName + "]")) {
                        hides.add(appInfo);
                    }
                }
                appInfos.removeAll(hides);

                x.task().autoPost(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addItems(appInfos);
                        hideLoading();
                    }
                });
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        AppInfo info = adapter.getItem(i);
        run(info);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final AppInfo info = adapter.getItem(position);
        final AlertDialog dialog = new AlertDialog.Builder(mContext).setView(R.layout.dialog_menu_all_app).show();
        dialog.findViewById(R.id.run).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                run(info);
            }
        });
        dialog.findViewById(R.id.un).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                un(info);
            }
        });
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        showTip("卸载成功");
        showLoading("加载中...", null);
    }

    @Subscribe
    public void onEventMainThread(final LauncherAppRefreshEvent event) {
        loadData();
    }

    private void run(AppInfo info) {
        Intent appIntent = pm.getLaunchIntentForPackage(info.packageName);
        if (appIntent == null) {
            showTip("APP不存在!!");
            return;
        }
        appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(appIntent);
        finish();
    }

    private void un(AppInfo info) {
        Intent deleteIntent = new Intent();
        deleteIntent.setAction(Intent.ACTION_DELETE);
        deleteIntent.setData(Uri.parse("package:" + info.packageName));
        startActivityForResult(deleteIntent, 0);
    }

}
