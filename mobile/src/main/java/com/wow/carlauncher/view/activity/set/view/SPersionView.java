package com.wow.carlauncher.view.activity.set.view;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.wow.carlauncher.R;
import com.wow.carlauncher.common.AppContext;
import com.wow.carlauncher.common.user.event.UEventLoginState;
import com.wow.carlauncher.common.view.SetView;
import com.wow.carlauncher.ex.manage.ImageManage;
import com.wow.carlauncher.view.activity.set.SetActivity;
import com.wow.carlauncher.view.activity.set.SetBaseView;
import com.wow.carlauncher.view.activity.set.event.SEventRequestLogin;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;

/**
 * Created by 10124 on 2018/4/22.
 */
@SuppressLint("ViewConstructor")
public class SPersionView extends SetBaseView {
    public SPersionView(SetActivity activity) {
        super(activity);
    }

    @Override
    protected int getContent() {
        return R.layout.content_set_persion;
    }

    @Override
    public String getName() {
        return "个人中心";
    }

    @BindView(R.id.sv_logout)
    SetView sv_logout;

    @BindView(R.id.ll_user)
    LinearLayout ll_user;

    @BindView(R.id.tv_nickname)
    TextView tv_nickname;

    @BindView(R.id.iv_user_pic)
    ImageView iv_user_pic;

    protected void initView() {
        if (AppContext.self().getLocalUser() != null) {
            tv_nickname.setText(AppContext.self().getLocalUser().getNickname());
            ImageManage.self().loadImage(AppContext.self().getLocalUser().getUserPic(), iv_user_pic, new ImageSize(100, 100));
        }
        View.OnClickListener onClickListener = v -> {
            switch (v.getId()) {
                case R.id.ll_user:
                    EventBus.getDefault().post(new SEventRequestLogin());
                    break;
                case R.id.sv_logout:
                    AppContext.self().logout();
                    break;
            }
        };

        ll_user.setOnClickListener(onClickListener);
        sv_logout.setOnClickListener(onClickListener);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(UEventLoginState event) {
        if (event.isLogin()) {
            tv_nickname.setText(AppContext.self().getLocalUser().getNickname());
            ImageManage.self().loadImage(AppContext.self().getLocalUser().getUserPic(), iv_user_pic);
        } else {
            tv_nickname.setText("点击登录");
            iv_user_pic.setImageResource(R.drawable.theme_music_dcover);
        }
    }
}
