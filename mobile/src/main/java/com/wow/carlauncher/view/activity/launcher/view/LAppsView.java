package com.wow.carlauncher.view.activity.launcher.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wow.carlauncher.R;
import com.wow.carlauncher.common.CommonData;
import com.wow.carlauncher.common.LogEx;
import com.wow.carlauncher.ex.manage.ThemeManage;
import com.wow.carlauncher.ex.manage.appInfo.AppInfo;
import com.wow.carlauncher.ex.manage.appInfo.AppInfoManage;
import com.wow.carlauncher.view.activity.launcher.LayoutEnum;
import com.wow.carlauncher.view.activity.set.event.SEventPromptShowRefresh;
import com.wow.carlauncher.view.base.BaseEXView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
import java.util.List;


public class LAppsView extends BaseEXView implements View.OnClickListener, View.OnLongClickListener {

    public LAppsView(@NonNull Context context, int columnNum, int page) {
        super(context);

        this.columnNum = columnNum;
        this.page = page;

        cellViews = new ArrayList<>();
        rows = new ArrayList<>();
        loadView();
        changedTheme(ThemeManage.self());
    }

    @Override
    protected int getContent() {
        return R.layout.content_app_grid;
    }

    @ViewInject(R.id.ll_base)
    private LinearLayout ll_base;

    private int columnNum;
    private int page;
    private List<AppInfo> appInfos;

    private List<View> cellViews;
    private List<LinearLayout> rows;
    private ViewTreeObserver.OnPreDrawListener oldOnPreDrawListener;

    public void loadView() {
        cellViews.clear();
        rows.clear();

        List<AppInfo> list = AppInfoManage.self().getShowAppInfos();
        int num = columnNum * 4;
        //1    0-16
        //2    16-32
        if (list.size() < num * page) {
            return;
        }
        int end = list.size();
        if (end > num * (page + 1)) {
            end = num * (page + 1);
        }
        appInfos = list.subList(num * page, end);
        LinearLayout row = null;
        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
        cellLp.weight = 1;
        for (int i = 0; i < num; i++) {
            if (i % columnNum == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                rows.add(row);
                ll_base.addView(row, LayoutParams.MATCH_PARENT, 100);
            }

            AppInfo model = null;
            if (i < appInfos.size()) {
                model = appInfos.get(i);
            }
            View cellView;
            if (model != null) {
                cellView = View.inflate(getContext(), R.layout.content_app_grid_item, null);
                ((TextView) cellView.findViewById(R.id.name)).setText(model.name);
                cellView.setOnClickListener(this);
                cellView.setOnLongClickListener(this);
                cellView.setTag(i);
            } else {
                cellView = new View(getContext());
                cellView.setTag(-1);
            }
            row.addView(cellView, cellLp);
            cellViews.add(cellView);
        }
        addRefreshItemHandle();

        LogEx.e(this, "initView: ");
    }

    private LayoutEnum layoutEnum = LayoutEnum.LAYOUT1;

    private int oldHeight = 0;//用来比对布局发生改变的

    public void setLayoutEnum(LayoutEnum layoutEnum) {
        if (layoutEnum == null) {
            return;
        }
        if (!layoutEnum.equals(this.layoutEnum)) {
            this.layoutEnum = layoutEnum;
            addRefreshItemHandle();
        }
    }

    @Override
    public void changedTheme(ThemeManage manage) {
        for (View cell : cellViews) {
            Integer index = (Integer) cell.getTag();
            if (index != null && index >= 0) {
                cell.findViewById(R.id.ll_base).setBackgroundResource(ThemeManage.self().getCurrentThemeRes(R.drawable.n_app_bg));
                cell.findViewById(R.id.line1).setBackgroundResource(ThemeManage.self().getCurrentThemeRes(R.drawable.n_line3));
                ((TextView) cell.findViewById(R.id.name)).setTextColor(ThemeManage.self().getCurrentThemeColor(R.color.l_msg));

                if (index < appInfos.size()) {
                    AppInfo model = appInfos.get(index);
                    ((ImageView) cell.findViewById(R.id.icon)).setImageDrawable(AppInfoManage.self().getIcon(model.clazz));
                }
            }
        }

        LogEx.e(this, "changedTheme: ");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SEventPromptShowRefresh event) {
        addRefreshItemHandle();
    }


    @Override
    public void onClick(View cell) {
        Integer index = (Integer) cell.getTag();
        if (index != null && index >= 0) {
            AppInfo info = appInfos.get(index);
            AppInfoManage.self().openApp(info.appMark + CommonData.CONSTANT_APP_PACKAGE_SEPARATE + info.clazz);
        }
    }

    @Override
    public boolean onLongClick(View cell) {
        Integer index = (Integer) cell.getTag();
        if (index != null && index >= 0) {
            final AppInfo info = appInfos.get(index);
            if (info.appMark == AppInfo.MARK_OTHER_APP) {
                final AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(R.layout.dialog_menu_all_app).show();
                dialog.findViewById(R.id.run).setOnClickListener(v -> {
                    dialog.dismiss();
                    AppInfoManage.self().openApp(info.appMark + CommonData.CONSTANT_APP_PACKAGE_SEPARATE + info.clazz);
                });
                dialog.findViewById(R.id.un).setOnClickListener(v -> {
                    dialog.dismiss();
                    un(info);
                });
            }
        }
        return true;
    }

    private void un(AppInfo info) {
        Intent deleteIntent = new Intent();
        deleteIntent.setAction(Intent.ACTION_DELETE);
        deleteIntent.setData(Uri.parse("package:" + info.clazz));
        getContext().startActivity(deleteIntent);
    }

    private void addRefreshItemHandle() {
        ll_base.getViewTreeObserver().removeOnPreDrawListener(oldOnPreDrawListener);
        oldOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (oldHeight != ll_base.getHeight() && ll_base.getHeight() > 0) {
                    oldHeight = ll_base.getHeight();
                    ll_base.getViewTreeObserver().removeOnPreDrawListener(this);
                    int h = ll_base.getHeight() / 4;
                    for (View row : rows) {
                        ViewGroup.LayoutParams lp = row.getLayoutParams();
                        lp.height = h;
                        row.setLayoutParams(lp);
                    }
                }
                return true;
            }
        };
        ll_base.getViewTreeObserver().addOnPreDrawListener(oldOnPreDrawListener);
    }
}
