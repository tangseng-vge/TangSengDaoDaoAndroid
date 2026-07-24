package com.chat.uikit.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.base.BageBaseFragment;
import com.chat.base.config.BageConfig;
import com.chat.base.config.BageConstants;
import com.chat.base.config.BageSharedPreferencesUtil;
import com.chat.base.config.BageSystemAccount;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.endpoint.entity.ContactsMenu;
import com.chat.base.entity.PopupMenuItem;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.HanziToPinyin;
import com.chat.base.utils.LayoutHelper;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.base.views.sidebar.listener.OnQuickSideBarTouchListener;
import com.chat.uikit.R;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.contacts.FriendAdapter;
import com.chat.uikit.contacts.FriendUIEntity;
import com.chat.uikit.databinding.FragContactsLayoutBinding;
import com.chat.uikit.search.SearchAllActivity;
import com.chat.uikit.search.remote.GlobalActivity;
import com.chat.uikit.user.UserDetailActivity;
import com.chat.uikit.utils.CharacterParser;
import com.chat.uikit.utils.PyingUtils;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 2019-11-12 14:57
 * 联系人
 */
public class ContactsFragment extends BageBaseFragment<FragContactsLayoutBinding> implements OnQuickSideBarTouchListener {

    private static final int CONTACTS_HEADER_GRID_SPAN = 4;

    private ContactsHeaderAdapter contactsHeaderAdapter;
    private FriendAdapter friendAdapter;
    private TextView allContactsCountTv;

    private FriendUIEntity sendFileEntity;
    private FriendUIEntity systemNoticeEntity;


    @Override
    protected boolean isShowBackLayout() {
        return false;
    }

    @Override
    protected FragContactsLayoutBinding getViewBinding() {
        return FragContactsLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        bageVBinding.textView.setTextSize(22);
        Typeface face = Typeface.createFromAsset(getResources().getAssets(),
                "fonts/mw_bold.ttf");
        bageVBinding.textView.setTypeface(face);
        bageVBinding.quickSideBarView.setTextChooseColor(Theme.colorAccount);
        bageVBinding.quickSideBarTipsView.setBackgroundColor(Theme.colorAccount);
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.searchIv.setVisibility(View.GONE);
        Theme.setPressedBackground(bageVBinding.searchIv);
        Theme.setPressedBackground(bageVBinding.rightIv);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initListener() {
        Object orgViewObject = EndpointManager.getInstance().invoke("org_contacts_view", requireContext());
        friendAdapter = new FriendAdapter();
        RecyclerView headerRecyclerView = new RecyclerView(requireContext());
        friendAdapter.addHeaderView(headerRecyclerView);
        if (orgViewObject != null) {
            View orgView = (View) orgViewObject;
            friendAdapter.addHeaderView(orgView);
        }
        friendAdapter.addFooterView(getFooterView());
        initAdapter(bageVBinding.recyclerView, friendAdapter);
        headerRecyclerView.setNestedScrollingEnabled(false);
        contactsHeaderAdapter = new ContactsHeaderAdapter();
        initContactsHeaderGridAdapter(headerRecyclerView, contactsHeaderAdapter);
        bageVBinding.quickSideBarView.setOnQuickSideBarTouchListener(this);
        friendAdapter.addChildClickViewIds(R.id.contentLayout);
        friendAdapter.setOnItemChildClickListener((adapter, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, view1 -> {
            FriendUIEntity friendEntity = (FriendUIEntity) adapter.getItem(position);
            if (friendEntity != null) {
                Intent intent = new Intent(getActivity(), UserDetailActivity.class);
                intent.putExtra("uid", friendEntity.channel.channelID);
                startActivity(intent);
            }
        }));
        contactsHeaderAdapter.setOnItemClickListener((adapter, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, view1 -> {
            ContactsMenu item = (ContactsMenu) adapter.getItem(position);
            if (item != null && item.iMenuClick != null) {
                item.iMenuClick.onClick();
            }
        }));
        bageVBinding.rightIv.setOnClickListener(view -> {
            List<PopupMenuItem> list = EndpointManager.getInstance().invokes(EndpointCategory.tabMenus, null);
            BageDialogUtils.getInstance().showScreenPopup(view, list);
        });
        //成员刷新监听
        BageIM.getInstance().getChannelManager().addOnRefreshChannelInfo("contacts_fragment_refresh_channel", (channel, isEnd) -> {
            if (channel != null) {
                Observable.create((ObservableOnSubscribe<Integer>) e -> {
                    if (sendFileEntity != null
                            && sendFileEntity.channel != null
                            && sendFileEntity.channel.channelID.equals(channel.channelID)
                            && sendFileEntity.channel.channelType == channel.channelType) {
                        applyChannelUpdate(sendFileEntity, channel);
                    }
                    if (systemNoticeEntity != null
                            && systemNoticeEntity.channel != null
                            && systemNoticeEntity.channel.channelID.equals(channel.channelID)
                            && systemNoticeEntity.channel.channelType == channel.channelType) {
                        applyChannelUpdate(systemNoticeEntity, channel);
                    }
                    for (int i = 0, size = friendAdapter.getData().size(); i < size; i++) {
                        if (friendAdapter.getData().get(i).channel != null
                                && friendAdapter.getData().get(i).channel.channelID.equals(channel.channelID)
                                && friendAdapter.getData().get(i).channel.channelType == channel.channelType) {
                            applyChannelUpdate(friendAdapter.getData().get(i), channel);
                            e.onNext(i);
                            break;
                        }
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Observer<>() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull Integer index) {
                        friendAdapter.notifyItemChanged(index + friendAdapter.getHeaderLayoutCount());
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

            }
        });
        bageVBinding.rlSearch.setOnClickListener( view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), new Pair<>(bageVBinding.searchIv, "searchView"));
                startActivity(new Intent(getActivity(), GlobalActivity.class), activityOptions.toBundle());
            } else {
                startActivity(new Intent(getActivity(), GlobalActivity.class));
            }
        });
        bageVBinding.searchIv.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), new Pair<>(bageVBinding.searchIv, "searchView"));
                startActivity(new Intent(getActivity(), GlobalActivity.class), activityOptions.toBundle());
            } else {
                startActivity(new Intent(getActivity(), GlobalActivity.class));
            }
        });
        //监听刷新通讯录
        EndpointManager.getInstance().setMethod("", EndpointCategory.bageRefreshMailList, object -> {
            resetHeaderData();
            return null;
        });

        EndpointManager.getInstance().setMethod(BageConstants.refreshContacts, object -> {
            getContacts();
            return null;
        });
    }

    @Override
    protected void initData() {
        bageVBinding.quickSideBarView.setLetters(CharacterParser.getInstance().getList());
        contactsHeaderAdapter.setList(buildContactsHeaderList());
        getContacts();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetHeaderData();
        getContacts();
    }

    private void getContacts() {
        List<BageChannel> allList = BageIM.getInstance().getChannelManager().getWithFollowAndStatus(BageChannelType.PERSONAL, 1, 1);
        List<FriendUIEntity> friendList = new ArrayList<>();
        for (int i = 0, size = allList.size(); i < size; i++) {
            FriendUIEntity entity = new FriendUIEntity(allList.get(i));
            String channelId = entity.channel.channelID;
            if (BageSystemAccount.system_file_helper.equals(channelId)) {
                if (isAdded()) {
                    entity.channel.channelName = getString(R.string.bage_file_helper);
                }
                sendFileEntity = entity;
                continue;
            }
            if (BageSystemAccount.system_team.equals(channelId)) {
                if (isAdded()) {
                    entity.channel.channelName = getString(R.string.bage_system_notice);
                }
                systemNoticeEntity = entity;
                continue;
            }
            friendList.add(entity);
        }
        List<FriendUIEntity> otherList = new ArrayList<>();
        List<FriendUIEntity> letterList = new ArrayList<>();
        List<FriendUIEntity> numList = new ArrayList<>();
        for (int i = 0, size = friendList.size(); i < size; i++) {
            String showName = friendList.get(i).channel.channelRemark;
            if (TextUtils.isEmpty(showName)) {
                showName = friendList.get(i).channel.channelName;
            }
            if (!TextUtils.isEmpty(showName)) {
                if (PyingUtils.getInstance().isStartNum(showName)) {
                    friendList.get(i).pying = "#";
                } else {
                    friendList.get(i).pying = HanziToPinyin.getInstance().getPY(showName);
                }
            } else {
                friendList.get(i).pying = "#";
            }
        }
        PyingUtils.getInstance().sortListBasic(friendList);

        for (int i = 0, size = friendList.size(); i < size; i++) {
            if (PyingUtils.getInstance().isStartLetter(friendList.get(i).pying)) {
                letterList.add(friendList.get(i));
            } else if (PyingUtils.getInstance().isStartNum(friendList.get(i).pying)) {
                numList.add(friendList.get(i));
            } else {
                otherList.add(friendList.get(i));
            }
        }
        List<FriendUIEntity> tempList = new ArrayList<>();
        tempList.addAll(letterList);
        tempList.addAll(numList);
        tempList.addAll(otherList);
        friendAdapter.setList(tempList);
        if (isAdded()) {
            allContactsCountTv.setText(String.format(getString(R.string.contacts_num), tempList.size()));
        }
    }

    private void applyChannelUpdate(FriendUIEntity entity, BageChannel channel) {
        entity.channel.channelName = channel.channelName;
        entity.channel.channelRemark = channel.channelRemark;
        entity.channel.mute = channel.mute;
        entity.channel.top = channel.top;
        entity.channel.avatar = channel.avatar;
        entity.channel.remoteExtraMap = channel.remoteExtraMap;
        entity.channel.online = channel.online;
        entity.channel.lastOffline = channel.lastOffline;
        entity.channel.deviceFlag = channel.deviceFlag;
    }

    private View getFooterView() {
        allContactsCountTv = new TextView(requireContext());
        allContactsCountTv.setGravity(Gravity.CENTER);
        allContactsCountTv.setTextSize(16);
        allContactsCountTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorDark));
        LinearLayout linearLayout = new LinearLayout(requireContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

//        linearLayout.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.homeColor));
        linearLayout.addView(allContactsCountTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) allContactsCountTv.getLayoutParams();
        layoutParams.topMargin = AndroidUtilities.dp(15);
        layoutParams.bottomMargin = AndroidUtilities.dp(15);
        return linearLayout;
    }

    @Override
    public void onLetterChanged(String letter, int position, float y) {
        bageVBinding.quickSideBarTipsView.setText(letter, position, y);
        //有此key则获取位置并滚动到该位置
        List<FriendUIEntity> list = friendAdapter.getData();
        if (BageReader.isNotEmpty(list)) {
            for (int i = 0, size = list.size(); i < size; i++) {
                if (list.get(i).pying.startsWith(letter)) {
                    bageVBinding.recyclerView.scrollToPosition(i + friendAdapter.getHeaderLayoutCount());
                    break;
                }
            }
        }
    }

    @Override
    public void onLetterTouching(boolean touching) {
        bageVBinding.quickSideBarTipsView.setVisibility(touching ? View.VISIBLE : View.INVISIBLE);
    }

    private void initContactsHeaderGridAdapter(RecyclerView recyclerView, ContactsHeaderAdapter adapter) {
        if (recyclerView == null || adapter == null) {
            return;
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), CONTACTS_HEADER_GRID_SPAN);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setHasFixedSize(true);
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        int spacingPx = getResources().getDimensionPixelSize(R.dimen.contacts_header_grid_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(CONTACTS_HEADER_GRID_SPAN, spacingPx));
        recyclerView.setAdapter(adapter);
        adapter.setAnimationFirstOnly(true);
    }

    private void resetHeaderData() {
        if (isAdded()) {
            contactsHeaderAdapter.setList(buildContactsHeaderList());
        }
    }

    /**
     * 联系人页头部菜单（不含朋友圈，朋友圈在发现页展示）。
     */
    private List<ContactsMenu> buildContactsHeaderList() {
        List<ContactsMenu> contactsMenus = EndpointManager.getInstance().invokes(EndpointCategory.mailList, getActivity());
        List<ContactsMenu> list = new ArrayList<>();
        if (contactsMenus != null) {
            for (ContactsMenu item : contactsMenus) {
                if (Objects.equals(item.sid, "moments")) {
                    continue;
                }
                if (!TextUtils.isEmpty(item.sid) && item.sid.equals("friend")) {
                    item.badgeNum = BageSharedPreferencesUtil.getInstance().getInt(BageConfig.getInstance().getUid() + "_new_friend_count");
                }
                list.add(item);
            }
        }
        list.add(new ContactsMenu("file_helper", R.mipmap.ic_send_file, getString(R.string.bage_file_helper), () -> {
            String uid = sendFileEntity != null && sendFileEntity.channel != null
                    ? sendFileEntity.channel.channelID
                    : BageSystemAccount.system_file_helper;
            BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(requireActivity(), uid, BageChannelType.PERSONAL, 0, true));
        }));
        list.add(new ContactsMenu("system_team", R.mipmap.ic_bage_ai, "Bage AI", () -> {
            String uid = systemNoticeEntity != null && systemNoticeEntity.channel != null
                    ? systemNoticeEntity.channel.channelID
                    : BageSystemAccount.system_team;
            BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(requireActivity(), uid, BageChannelType.PERSONAL, 0, true));
        }));
        return list;
    }

}
