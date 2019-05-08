package com.wow.carlauncher.view.activity.launcher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.wow.carlauncher.R;
import com.wow.carlauncher.common.CommonData;
import com.wow.carlauncher.common.LogEx;
import com.wow.carlauncher.common.ViewPagerOnPageChangeListener;
import com.wow.carlauncher.common.util.SharedPreUtil;
import com.wow.carlauncher.ex.manage.ThemeManage;
import com.wow.carlauncher.ex.manage.appInfo.AppInfoManage;
import com.wow.carlauncher.ex.manage.appInfo.event.MAppInfoRefreshShowEvent;
import com.wow.carlauncher.ex.manage.location.event.MNewLocationEvent;
import com.wow.carlauncher.ex.manage.toast.ToastManage;
import com.wow.carlauncher.ex.plugin.console.ConsolePlugin;
import com.wow.carlauncher.ex.plugin.console.event.PConsoleEventCallState;
import com.wow.carlauncher.ex.plugin.fk.event.PFkEventAction;
import com.wow.carlauncher.ex.plugin.music.MusicPlugin;
import com.wow.carlauncher.view.activity.launcher.event.LEventRefreshFmPluginTest;
import com.wow.carlauncher.view.activity.launcher.event.LItemRefreshEvent;
import com.wow.carlauncher.view.activity.launcher.event.LItemToFristEvent;
import com.wow.carlauncher.view.activity.launcher.event.LLayoutRefreshEvent;
import com.wow.carlauncher.view.activity.launcher.event.LPageTransformerChangeEvent;
import com.wow.carlauncher.view.activity.launcher.view.LAppsView;
import com.wow.carlauncher.view.activity.launcher.view.LDockView;
import com.wow.carlauncher.view.activity.launcher.view.LPageView;
import com.wow.carlauncher.view.activity.launcher.view.LPagerPostion;
import com.wow.carlauncher.view.activity.launcher.view.LPromptView;
import com.wow.carlauncher.view.activity.set.event.SEventPromptShowRefresh;
import com.wow.carlauncher.view.activity.set.event.SEventSetHomeFull;
import com.wow.carlauncher.view.popup.ConsoleWin;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import per.goweii.anypermission.AnyPermission;
import per.goweii.anypermission.RequestListener;
import per.goweii.anypermission.RuntimeRequester;

import static com.wow.carlauncher.common.CommonData.APP_WIDGET_FM_PLUGIN;
import static com.wow.carlauncher.common.CommonData.REQUEST_SELECT_FM_PLUGIN;
import static com.wow.carlauncher.common.CommonData.SDATA_HOME_FULL;
import static com.wow.carlauncher.common.CommonData.SDATA_LAUNCHER_ITEM_TRAN;
import static com.wow.carlauncher.common.CommonData.SDATA_LAUNCHER_LAYOUT;
import static com.wow.carlauncher.common.CommonData.SDATA_LAUNCHER_PROMPT_SHOW;
import static com.wow.carlauncher.ex.plugin.fk.FangkongProtocolEnum.YLFK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.CENTER_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.CENTER_LONG_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.LEFT_BOTTOM_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.LEFT_BOTTOM_LONG_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.LEFT_TOP_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.LEFT_TOP_LONG_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.RIGHT_BOTTOM_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.RIGHT_BOTTOM_LONG_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.RIGHT_TOP_CLICK;
import static com.wow.carlauncher.ex.plugin.fk.protocol.YiLianProtocol.RIGHT_TOP_LONG_CLICK;

public class LauncherActivity extends Activity implements ThemeManage.OnThemeChangeListener {

    @SuppressLint("StaticFieldLeak")
    private static LauncherActivity old;

    @ViewInject(R.id.viewPager)
    private ViewPager viewPager;

    @ViewInject(R.id.ll_base)
    private LinearLayout ll_base;

    @ViewInject(R.id.ll_center)
    private LinearLayout ll_center;

    @ViewInject(R.id.ll_top)
    private LPromptView ll_top;

    @ViewInject(R.id.ll_dock)
    private LDockView ll_dock;

    @ViewInject(R.id.postion)
    private LPagerPostion postion;

    private int lastPagerNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogEx.e(this, "onCreate:start ");
        //防止初始化两次
        if (old != null) {
            old.finish();
        }
        old = this;

        if (SharedPreUtil.getBoolean(SDATA_HOME_FULL, true)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        //超级大坑,必去全局设置才能用
        ThemeManage.self().refreshTheme();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(homeReceiver, intentFilter);
        initView();

        onThemeChanged(ThemeManage.self());
        ThemeManage.self().registerThemeChangeListener(this);
        ThemeManage.self().refreshTheme();
        x.task().postDelayed(this::requestRuntime, 1000);

        LogEx.e(this, "onCreate:end ");
    }

    public void initView() {
        setContentView(R.layout.activity_lanncher);
        x.view().inject(this);
        viewPager.addOnPageChangeListener(postion);
        viewPager.addOnPageChangeListener(new ViewPagerOnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                lastPagerNum = i;
            }
        });
        viewPager.setPageTransformer(true, ItemTransformer.getById(SharedPreUtil.getInteger(SDATA_LAUNCHER_ITEM_TRAN, ItemTransformer.None.getId())).getTransformer());
        ll_top.setVisibility(SharedPreUtil.getBoolean(SDATA_LAUNCHER_PROMPT_SHOW, true) ? View.VISIBLE : View.GONE);

        EventBus.getDefault().register(this);
        initItem();
        initApps();
        refreshViewPager();
        loadLayout();
    }

    private LPageView[] itemPager;

    private void initItem() {
        LogEx.e(this, "initItem");
        //计算排序
        List<ItemModel> items = new ArrayList<>();
        for (ItemEnum item : CommonData.LAUNCHER_ITEMS) {
            if (item.equals(ItemEnum.FM)) {
                if (SharedPreUtil.getBoolean(CommonData.SDATA_LAUNCHER_ITEM_OPEN_ + item.getId(), false)) {
                    items.add(new ItemModel(item,
                            SharedPreUtil.getInteger(CommonData.SDATA_LAUNCHER_ITEM_SORT_ + item.getId(), item.getId()),
                            SharedPreUtil.getBoolean(CommonData.SDATA_LAUNCHER_ITEM_OPEN_ + item.getId(), false)
                    ));
                }
            } else if (SharedPreUtil.getBoolean(CommonData.SDATA_LAUNCHER_ITEM_OPEN_ + item.getId(), true)) {
                items.add(new ItemModel(item,
                        SharedPreUtil.getInteger(CommonData.SDATA_LAUNCHER_ITEM_SORT_ + item.getId(), item.getId()),
                        SharedPreUtil.getBoolean(CommonData.SDATA_LAUNCHER_ITEM_OPEN_ + item.getId(), true)
                ));
            }
        }
        Collections.sort(items, (o1, o2) -> o1.index - o2.index);
        //获取每页的item数量
        int psize = getPageItemNum();

        //计算全部展示需要几页
        int pageNum = items.size() / psize;
        if (items.size() % psize != 0) {
            pageNum++;
        }
        itemPager = new LPageView[pageNum];
        LayoutEnum layoutEnum = LayoutEnum.getById(SharedPreUtil.getInteger(SDATA_LAUNCHER_LAYOUT, LayoutEnum.LAYOUT1.getId()));
        for (int i = 0; i < itemPager.length; i++) {
            itemPager[i] = new LPageView(this);
            itemPager[i].setLayoutEnum(layoutEnum);
        }

        //记录当前第几页,这里应该是-1,因为下面会从0开始,直接+1
        int npage = -1;
        //声明一个空页
        View[] pageItems = new View[psize];
        //循环可用的items
        for (int i = 0; i < items.size(); i++) {
            ItemModel model = items.get(i);
            //当一页满后,下一页重新创建一个页面View
            if (i % psize == 0) {
                pageItems = new View[psize];
                npage++;
            }
            //根据信息创建一个View
            pageItems[i % psize] = ItemEnum.createView(this, model.info);
            //如果是一组的最后一个,或者是列表最后一个
            if (((i % psize == psize - 1) || (i == items.size() - 1))) {
                itemPager[npage].setItem(pageItems);
            }
        }
        //初始化完毕
    }

    private LAppsView[] appsPager;

    private void initApps() {
        LogEx.e(this, "initApps");
        //获取每页的item数量
        int psize = getPageItemNum();
        int appsize = AppInfoManage.self().getShowAppInfos().size();
        int pageSize = psize * 4;
        int appPageNum = appsize / pageSize;
        if (appsize % pageSize != 0) {
            appPageNum++;
        }

        appsPager = new LAppsView[appPageNum];
        for (int i = 0; i < appPageNum; i++) {
            appsPager[i] = new LAppsView(this, psize, i);
        }
    }

    private void loadLayout() {
        if (ll_top.getParent() != null) {
            ((ViewGroup) ll_top.getParent()).removeView(ll_top);
        }
        if (ll_dock.getParent() != null) {
            ((ViewGroup) ll_dock.getParent()).removeView(ll_dock);
        }

        LayoutEnum layoutEnum = LayoutEnum.getById(SharedPreUtil.getInteger(SDATA_LAUNCHER_LAYOUT, LayoutEnum.LAYOUT1.getId()));
        if (layoutEnum.equals(LayoutEnum.LAYOUT1)) {
            LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            dockLp.weight = 7;
            ll_center.addView(ll_dock, 0, dockLp);

            LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            bottomLp.weight = 1;
            ll_base.addView(ll_top, bottomLp);

            LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            centerLp.weight = 8;
            ll_center.setLayoutParams(centerLp);
        } else if (layoutEnum.equals(LayoutEnum.LAYOUT2)) {
            LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            topLp.weight = 9;

            LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            centerLp.weight = 75;

            LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
            bottomLp.weight = 16;

            ll_base.addView(ll_top, 0, topLp);

            ll_center.setLayoutParams(centerLp);

            ll_base.addView(ll_dock, bottomLp);
        }
        ll_top.setLayoutEnum(layoutEnum);
        ll_dock.setLayoutEnum(layoutEnum);

        for (LPageView lPageView : itemPager) {
            lPageView.setLayoutEnum(layoutEnum);
        }

        for (LAppsView lAppsView : appsPager) {
            lAppsView.setLayoutEnum(layoutEnum);
        }
    }

    private void refreshViewPager() {
        //最后合并一下两个数组,展示出来
        View[] showViews = new View[itemPager.length + appsPager.length];
        for (int i = 0; i < showViews.length; i++) {
            if (i < itemPager.length) {
                showViews[i] = itemPager[i];
            } else {
                showViews[i] = appsPager[i - itemPager.length];
            }
        }
        ViewAdapter viewAdapter = new ViewAdapter(showViews);
        viewPager.setAdapter(viewAdapter);
        postion.loadPostion(showViews.length);

        //放到小圆点后面
        if (lastPagerNum >= viewAdapter.getCount()) {
            lastPagerNum = viewAdapter.getCount() - 1;
        }
        viewPager.setCurrentItem(lastPagerNum);
    }


    @Override
    public void onThemeChanged(ThemeManage manage) {
        if (manage.getTheme().equals(ThemeManage.Theme.KBLACK)) {
            WallpaperManager wallpaperManager = WallpaperManager
                    .getInstance(getApplicationContext());
            // 获取当前壁纸
            Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            ll_base.setBackground(wallpaperDrawable);
        } else {
            ll_base.setBackgroundResource(manage.getCurrentThemeRes(R.drawable.n_desk_bg));
        }
        LogEx.e(this, "onThemeChanged ");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mRuntimeRequester != null) {
            mRuntimeRequester.onActivityResult(requestCode);
        }
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_FM_PLUGIN: {
                    int id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                    SharedPreUtil.saveInteger(APP_WIDGET_FM_PLUGIN, id);
                    EventBus.getDefault().post(new LEventRefreshFmPluginTest());
                    break;
                }
                default:
                    break;
            }
        }
    }

    private boolean show = false;

    @Override
    protected void onResume() {
        super.onResume();
        show = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        show = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(homeReceiver);
        EventBus.getDefault().unregister(this);
        ThemeManage.self().unregisterThemeChangeListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final LLayoutRefreshEvent event) {
        loadLayout();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final SEventSetHomeFull event) {
        this.recreate();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(final MAppInfoRefreshShowEvent event) {
        initApps();
        refreshViewPager();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LItemRefreshEvent event) {
        initItem();
        initApps();
        refreshViewPager();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LItemToFristEvent event) {
        viewPager.setCurrentItem(0, true);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SEventPromptShowRefresh event) {
        ll_top.setVisibility(SharedPreUtil.getBoolean(SDATA_LAUNCHER_PROMPT_SHOW, true) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {

    }

    private String lastadCode;

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MNewLocationEvent event) {
        if (!event.getAdCode().equals(lastadCode)) {
            lastadCode = event.getAdCode();
            ToastManage.self().show("定位成功:" + event.getCity());
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(LPageTransformerChangeEvent event) {
        viewPager.setPageTransformer(true, ItemTransformer.getById(SharedPreUtil.getInteger(SDATA_LAUNCHER_ITEM_TRAN, ItemTransformer.None.getId())).getTransformer());
    }


    private boolean isCalling = false;

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(final PConsoleEventCallState event) {
        isCalling = event.isCalling();
    }

    @Subscribe
    public void onEvent(PFkEventAction event) {
        if (YLFK.equals(event.getFangkongProtocol())) {
            switch (event.getAction()) {
                case LEFT_TOP_CLICK: {
                    ConsolePlugin.self().decVolume();
                    break;
                }
                case LEFT_TOP_LONG_CLICK: {
                    MusicPlugin.self().pre();
                    break;
                }
                case RIGHT_TOP_CLICK: {
                    ConsolePlugin.self().incVolume();
                    break;
                }
                case RIGHT_TOP_LONG_CLICK: {
                    MusicPlugin.self().next();
                    break;
                }
                case CENTER_CLICK: {
                    if (show && viewPager != null && viewPager.getCurrentItem() == 0) {
                        showConsoleWin();
                    } else if (show && viewPager != null) {
                        x.task().autoPost(() -> {
                            viewPager.setCurrentItem(0, true);
                        });
                    } else {
                        x.task().autoPost(() -> {
                            Intent home = new Intent(Intent.ACTION_MAIN);
                            home.addCategory(Intent.CATEGORY_HOME);
                            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            startActivity(home);

//                            Intent home = new Intent(this, LauncherActivity.class);
//                            startActivity(home);
                        });
                    }
                    break;
                }
                case CENTER_LONG_CLICK: {
                    showConsoleWin();
                    break;
                }
                case LEFT_BOTTOM_CLICK: {
                    if (isCalling) {
                        ConsolePlugin.self().callAnswer();
                    } else {
                        ConsolePlugin.self().mute();
                    }
                    break;
                }
                case LEFT_BOTTOM_LONG_CLICK: {
                    LogEx.e(this, "这里唤醒语音比较好");
                    break;
                }
                case RIGHT_BOTTOM_CLICK:
                    if (isCalling) {
                        ConsolePlugin.self().callHangup();
                    } else {
                        x.task().autoPost(() -> {
                            //切换页面?
                            if (viewPager != null && viewPager.getAdapter() != null && itemPager.length > 0) {
                                if (viewPager.getCurrentItem() >= itemPager.length - 1) {
                                    viewPager.setCurrentItem(0, true);
                                } else {
                                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
                                }
                            }
                        });
                    }
                    break;
                case RIGHT_BOTTOM_LONG_CLICK:
                    x.task().autoPost(() -> {
                        if (viewPager != null && appsPager.length > 0) {
                            viewPager.setCurrentItem(itemPager.length, true);
                        }
                    });
                    break;

            }
        }
    }

    private int getPageItemNum() {
        int psize = SharedPreUtil.getInteger(CommonData.SDATA_LAUNCHER_ITEM_NUM, 3);
        if (psize != 4 && psize != 2 && psize != 5) {
            psize = 3;
        }
        return psize;
    }

    private void showConsoleWin() {
        x.task().autoPost(() -> AnyPermission.with(getApplicationContext()).overlay()
                .onWithoutPermission((data, executor) -> {
                    new AlertDialog.Builder(getApplicationContext()).setTitle("警告!")
                            .setNegativeButton("取消", (dialog, which) -> executor.cancel())
                            .setPositiveButton("确定", (dialog2, which2) -> executor.execute())
                            .setMessage("请给与嘟嘟桌面悬浮窗权限,否则无法使用这个功能").show();
                })
                .request(new RequestListener() {
                    @Override
                    public void onSuccess() {
                        ConsoleWin.self().show();
                    }

                    @Override
                    public void onFailed() {
                        ToastManage.self().show("没有悬浮窗权限!");
                    }
                }));
    }

    private RuntimeRequester mRuntimeRequester;

    private void requestRuntime() {
        final AnyPermission anyPermission = AnyPermission.with(this);
        mRuntimeRequester = AnyPermission.with(this).runtime(1)
                .permissions(
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.GET_TASKS,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED,
                        Manifest.permission.KILL_BACKGROUND_PROCESSES,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.RECORD_AUDIO
                )
                .onBeforeRequest((permission, executor) -> {
                    new AlertDialog.Builder(LauncherActivity.this).setTitle("警告!")
                            .setNegativeButton("取消", (dialog2, which2) -> executor.cancel())
                            .setPositiveButton("确定", (dialog2, which2) -> executor.execute())
                            .setMessage("嘟嘟桌面正在申请:" + anyPermission.name(permission) + " 权限").show();
                })
                .onBeenDenied((permission, executor) -> {
                    new AlertDialog.Builder(LauncherActivity.this).setTitle("警告!")
                            .setNegativeButton("不授权", (dialog2, which2) -> executor.cancel())
                            .setPositiveButton("重新授权", (dialog2, which2) -> executor.execute())
                            .setMessage("嘟嘟桌面需要:" + anyPermission.name(permission) + " 权限才能正常运行!").show();
                })
                .onGoSetting((permission, executor) -> {
                    new AlertDialog.Builder(LauncherActivity.this).setTitle("警告!")
                            .setNegativeButton("不授权", (dialog2, which2) -> executor.cancel())
                            .setPositiveButton("重新授权", (dialog2, which2) -> executor.execute())
                            .setMessage("嘟嘟桌面需要:" + anyPermission.name(permission) + " 权限才能正常运行,前往设置界面设置!").show();
                })
                .request(new RequestListener() {
                    @Override
                    public void onSuccess() {
                        requestOverlay();
                    }

                    @Override
                    public void onFailed() {
                        ToastManage.self().show("授权失败,某些权限不足可能导致功能不可用!");
                    }
                });
    }

    private void requestOverlay() {
        AnyPermission.with(this).overlay()
                .onWithoutPermission((data, executor) -> {
                    new AlertDialog.Builder(LauncherActivity.this).setTitle("警告!")
                            .setNegativeButton("不授权", (dialog2, which2) -> executor.cancel())
                            .setPositiveButton("重新授权", (dialog2, which2) -> executor.execute())
                            .setMessage("嘟嘟桌面需要悬浮窗权限才能正常运行!").show();
                })
                .request(new RequestListener() {
                    @Override
                    public void onSuccess() {
                        ToastManage.self().show("权限检查成功");
                    }

                    @Override
                    public void onFailed() {
                        ToastManage.self().show("授权失败,某些权限不足可能导致功能不可用!");
                    }
                });
    }

    class ViewAdapter extends PagerAdapter {
        private View[] datas;

        public ViewAdapter(View[] list) {
            datas = list;
        }

        @Override
        public int getCount() {
            return datas.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = datas[position];
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(datas[position]);
        }
    }

    private BroadcastReceiver homeReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    //表示按了home键,程序到了后台
                    if (show) {
                        if (viewPager != null) {
                            viewPager.setCurrentItem(0, true);
                        }
                    } else {
                        Intent home = new Intent(Intent.ACTION_MAIN);
                        home.addCategory(Intent.CATEGORY_HOME);
                        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        startActivity(home);
                    }
                }
            }
        }
    };
}
