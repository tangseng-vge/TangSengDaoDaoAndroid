package com.chat.uikit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.chat.base.adapter.WKFragmentStateAdapter;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.common.WKCommonModel;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKConstants;
import com.chat.base.config.WKSharedPreferencesUtil;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.MailListDot;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.ActManagerUtils;
import com.chat.base.utils.WKDeviceUtils;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.WKTimeUtils;
import com.chat.base.utils.language.WKMultiLanguageUtil;
import com.chat.base.utils.rxpermissions.RxPermissions;
import com.chat.base.utils.systembar.WKStatusBarUtils;
import com.chat.uikit.contacts.service.FriendModel;
import com.chat.uikit.databinding.ActTabMainBinding;
import com.chat.uikit.fragment.ChatFragment;
import com.chat.uikit.fragment.ContactsFragment;
import com.chat.uikit.fragment.DiscoverFragment;
import com.chat.uikit.fragment.MyFragment;
import com.chat.uikit.user.service.UserModel;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;


/**
 * 2019-11-12 13:57
 * tab导航栏
 */
public class TabActivity extends WKBaseActivity<ActTabMainBinding> {
    /** ViewPager / 底部导航默认选中：聊天（第 3 个 Tab） */
    private static final int DEFAULT_TAB_INDEX = 2;
    private static final int CHAT_TAB_INDEX = 2;
    /** PushNotificationHelper 当前统一使用该 ID 展示普通聊天消息通知。 */
    private static final int MESSAGE_NOTIFICATION_ID = 1;

    private ChatFragment chatFragment;

    private long lastClickChatTabTime = 0L;
    private final boolean isShowTabText = true;

    @Override
    protected ActTabMainBinding getViewBinding() {
        return ActTabMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initPresenter() {
        ActManagerUtils.getInstance().clearAllActivity();
    }

    @Override
    public boolean supportSlideBack() {
        return false;
    }

    @SuppressLint("CheckResult")
    @Override
    protected void initView() {
//        wkVBinding.vp.setUserInputEnabled(false);
        UserModel.getInstance().device();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String desc = String.format(getString(R.string.notification_permissions_desc), getString(R.string.app_name));
            RxPermissions rxPermissions = new RxPermissions(this);
            rxPermissions.request(Manifest.permission.POST_NOTIFICATIONS).subscribe(aBoolean -> {
                if (aBoolean) {
                    // 服务可能在授权弹窗出现前已经启动，当时通知不会显示。
                    // 授权成功后再次启动，触发 onStartCommand() 立即补发常驻通知。
                    WKIMKeepAliveService.start(this);
                } else {
                    WKDialogUtils.getInstance().showDialog(this, getString(com.chat.base.R.string.authorization_request), desc, true, getString(R.string.cancel), getString(R.string.to_set), 0, Theme.colorAccount, index -> {
                        if (index == 1) {
                            EndpointManager.getInstance().invoke("show_open_notification_dialog", this);
                        }
                    });
                }
            });
        } else {
            boolean isEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
            if (!isEnabled) {
                EndpointManager.getInstance().invoke("show_open_notification_dialog", this);
            } else {
                WKIMKeepAliveService.start(this);
            }
        }

        statusBarMode();

        List<Fragment> fragments = new ArrayList<>(4);
        fragments.add(new ContactsFragment());
        fragments.add(new DiscoverFragment());
        chatFragment = new ChatFragment();
        fragments.add(chatFragment);
        fragments.add(new MyFragment());

        wkVBinding.vp.setAdapter(new WKFragmentStateAdapter(this, fragments));
        WKCommonModel.getInstance().getAppNewVersion(false, version -> {
            String v = WKDeviceUtils.getInstance().getVersionName(TabActivity.this);
            if (version != null && !TextUtils.isEmpty(version.download_url) && !version.app_version.equals(v)) {
                WKDialogUtils.getInstance().showNewVersionDialog(TabActivity.this, version);
            }
        });
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 只清理普通聊天消息通知。cancelAll() 会波及 IM 前台保活通知，
        // 在部分厂商系统上会导致状态栏的常驻通知直接消失。
        notificationManager.cancel(MESSAGE_NOTIFICATION_ID);
        WKCommonModel.getInstance().getAppConfig(null);

        wkVBinding.bottomNavigation.setLabelVisibilityMode(isShowTabText
                ? NavigationBarView.LABEL_VISIBILITY_LABELED
                : NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        wkVBinding.bottomNavigation.setItemIconTintList(null);
        wkVBinding.bottomNavigation.setItemTextColor(ContextCompat.getColorStateList(this, R.color.tab_nav_item_text_color));

        initTabBadges();

        wkVBinding.vp.setCurrentItem(DEFAULT_TAB_INDEX, false);
        wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_chat);
    }

    private void initTabBadges() {
        int[] ids = new int[]{R.id.i_contacts, R.id.i_discover, R.id.i_chat, R.id.i_my};
        for (int id : ids) {
            BadgeDrawable badge = wkVBinding.bottomNavigation.getOrCreateBadge(id);
            styleUnreadBadge(badge);
            badge.setVisible(false);
        }
    }

    private void styleUnreadBadge(BadgeDrawable badge) {
        badge.setBackgroundColor(ContextCompat.getColor(this, R.color.reminderColor));
        badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void statusBarMode() {
        Window window = getWindow();
        if (window == null) return;
        WKStatusBarUtils.transparentStatusBar(window);
        if (!Theme.getDarkModeStatus(this))
            WKStatusBarUtils.setDarkMode(window);
        else WKStatusBarUtils.setLightMode(window);
    }

    @Override
    protected void initListener() {
        wkVBinding.vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                lastClickChatTabTime = 0;
                if (position != CHAT_TAB_INDEX && chatFragment != null) {
                    chatFragment.dismissOpenedSwipeImmediate();
                }
                if (position == 0) {
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_contacts);
                } else if (position == 1) {
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_discover);
                } else if (position == 2) {
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_chat);
                } else {
                    wkVBinding.bottomNavigation.setSelectedItemId(R.id.i_my);
                }
            }
        });
        wkVBinding.bottomNavigation.setItemIconTintList(null);
        wkVBinding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.i_contacts) {
                wkVBinding.vp.setCurrentItem(0);
                return true;
            } else if (item.getItemId() == R.id.i_discover) {
                wkVBinding.vp.setCurrentItem(1);
                return true;
            } else if (item.getItemId() == R.id.i_chat) {
                long nowTime = WKTimeUtils.getInstance().getCurrentMills();
                if (wkVBinding.vp.getCurrentItem() == 2) {
                    if (nowTime - lastClickChatTabTime <= 300) {
                        EndpointManager.getInstance().invoke("scroll_to_unread_channel", null);
                    }
                    lastClickChatTabTime = nowTime;
                    return true;
                }
                lastClickChatTabTime = 0;
                wkVBinding.vp.setCurrentItem(2);
                return true;
            } else {
                wkVBinding.vp.setCurrentItem(3);
                return true;
            }
        });
        EndpointManager.getInstance().setMethod("tab_activity", EndpointCategory.wkRefreshMailList, object -> {
            getAllRedDot();
            return null;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getAllRedDot();
        boolean sync_friend = WKSharedPreferencesUtil.getInstance().getBoolean("sync_friend");
        if (sync_friend) {
            FriendModel.getInstance().syncFriends((code, msg) -> {
                if (code != HttpResponseCode.success && !TextUtils.isEmpty(msg)) {
                    showToast(msg);
                }
                if (code == HttpResponseCode.success) {
                    WKSharedPreferencesUtil.getInstance().putBoolean("sync_friend", false);
                }
            });
        }
    }

    public void setMsgCount(int number) {
        WKUIKitApplication.getInstance().totalMsgCount = number;
        BadgeDrawable badge = wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_chat);
        styleUnreadBadge(badge);
        if (number > 0) {
            badge.setNumber(Math.min(number, 999));
            badge.setVisible(true);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }

    public void setContactCount(int number, boolean showDot) {
        BadgeDrawable badge = wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_contacts);
        styleUnreadBadge(badge);
        if (number > 0) {
            badge.setNumber(Math.min(number, 999));
            badge.setVisible(true);
        } else if (showDot) {
            badge.clearNumber();
            badge.setVisible(true);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }

    public void setDiscoverCount(int number, boolean showDot) {
        BadgeDrawable badge = wkVBinding.bottomNavigation.getOrCreateBadge(R.id.i_discover);
        styleUnreadBadge(badge);
        if (number > 0) {
            badge.setNumber(Math.min(number, 999));
            badge.setVisible(true);
        } else if (showDot) {
            badge.clearNumber();
            badge.setVisible(true);
        } else {
            badge.clearNumber();
            badge.setVisible(false);
        }
    }

    private void getAllRedDot() {
        int newFriendCount = WKSharedPreferencesUtil.getInstance().getInt(WKConfig.getInstance().getUid() + "_new_friend_count");
        setContactCount(newFriendCount, false);

        boolean showDot = false;
        int momentCount = 0;
        List<MailListDot> list = EndpointManager.getInstance().invokes(EndpointCategory.wkGetMailListRedDot, null);
        if (WKReader.isNotEmpty(list)) {
            for (MailListDot mailListDot : list) {
                if (mailListDot != null) {
                    momentCount += mailListDot.numCount;
                    if (!showDot) showDot = mailListDot.showDot;
                }
            }
        }
        setDiscoverCount(momentCount, showDot);
    }

    @Override
    public Resources getResources() {
        float fontScale = WKConstants.getFontScale();
        Resources res = super.getResources();
        Configuration config = res.getConfiguration();
        config.fontScale = fontScale; //1 设置正常字体大小的倍数
        res.updateConfiguration(config, res.getDisplayMetrics());
        return res;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        } else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        WKMultiLanguageUtil.getInstance().setConfiguration();
        Theme.applyTheme();
        EndpointManager.getInstance().invokes(EndpointCategory.wkRefreshChatConversation, null);
    }

    @Override
    public void finish() {
        super.finish();
        EndpointManager.getInstance().remove("tab_activity");
    }
}
