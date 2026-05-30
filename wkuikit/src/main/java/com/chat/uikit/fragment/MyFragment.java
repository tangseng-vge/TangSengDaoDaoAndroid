package com.chat.uikit.fragment;

import android.content.Intent;
import android.text.TextUtils;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.base.WKBaseFragment;
import com.chat.base.common.WKCommonModel;
import com.chat.base.config.WKConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.PersonalInfoMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.WKLogUtils;
import com.chat.base.utils.WKReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.FragMyLayoutBinding;
import com.chat.uikit.user.MyInfoActivity;
import com.chat.uikit.user.UserQrActivity;
import com.xinbida.wukongim.entity.WKChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2019-11-12 14:58
 * 我的
 */
public class MyFragment extends WKBaseFragment<FragMyLayoutBinding> {
    private static final int PERSONAL_GRID_SPAN = 3;

    private PersonalItemAdapter adapter;

    @Override
    protected FragMyLayoutBinding getViewBinding() {
        return FragMyLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        wkVBinding.recyclerView.setNestedScrollingEnabled(false);
        adapter = new PersonalItemAdapter(new ArrayList<>());
        initMyAdapter(wkVBinding.recyclerView, adapter);

        wkVBinding.idLeftTv.setText(String.format(getString(R.string.identity), getString(R.string.app_name)));

        reloadPersonalMenus();
    }

    private void initMyAdapter(RecyclerView recyclerView, PersonalItemAdapter adapter) {
        if (recyclerView == null || adapter == null) {
            return;
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), PERSONAL_GRID_SPAN);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setHasFixedSize(true);
        while (recyclerView.getItemDecorationCount() > 0) {
            recyclerView.removeItemDecorationAt(0);
        }
        int spacingPx = getResources().getDimensionPixelSize(R.dimen.personal_grid_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(PERSONAL_GRID_SPAN, spacingPx));
        recyclerView.setAdapter(adapter);
        adapter.setAnimationFirstOnly(true);
    }


    @Override
    protected void initPresenter() {
        wkVBinding.avatarView.setSize(90);
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        wkVBinding.refreshLayout.setEnableRefresh(false);
        Theme.setPressedBackground(wkVBinding.qrIv);
    }

    @Override
    protected void initListener() {
        EndpointManager.getInstance().setMethod("my_fragment", EndpointCategory.wkRefreshPersonalCenter, object -> {
            reloadPersonalMenus();
            return null;
        });
        adapter.setOnItemClickListener((adapter1, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, view1 -> {
            PersonalInfoMenu menu = (PersonalInfoMenu) adapter1.getItem(position);
            if (menu != null && menu.iPersonalInfoMenuClick != null) {
                menu.iPersonalInfoMenuClick.onClick();
            }
        }));
        SingleClickUtil.onSingleClick(wkVBinding.avatarView, view -> gotoMyInfo());
        SingleClickUtil.onSingleClick(wkVBinding.qrIv, view -> {
            startActivity(new Intent(requireActivity(), UserQrActivity.class));
        });

        WKCommonModel.getInstance().getChannel(WKConfig.getInstance().getUid(), WKChannelType.PERSONAL, (code, msg, entity) -> {
            if (entity != null && entity.extra != null) {
                Object shortNoObject = entity.extra.get("short_no");
                if (shortNoObject != null) {
                    String shortNo = (String) shortNoObject;
                    wkVBinding.tvID.setText(shortNo);
                    wkVBinding.nameTv.setText(entity.name);
                }
            }
        });

    }

    private void reloadPersonalMenus() {
        if (!isAdded() || adapter == null) {
            return;
        }
        List<PersonalInfoMenu> endpoints = EndpointManager.getInstance()
                .invokes(EndpointCategory.personalCenter, getActivity());
        if (endpoints != null) {
            for (int i = 0; i < endpoints.size(); i++) {
                if (!TextUtils.isEmpty(endpoints.get(i).sid)
                        && endpoints.get(i).sid.equals("invite_code")
                        && WKConfig.getInstance().getAppConfig().register_invite_on == 0) {
                    endpoints.remove(i);
                    break;
                }
            }
            adapter.setList(endpoints);
        }
    }

    void gotoMyInfo() {
//        String str = WKDeviceUtils.getSignature(getActivity());
//        Log.e("签名",str+"");
        startActivity(new Intent(getActivity(), MyInfoActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        wkVBinding.nameTv.setText(WKConfig.getInstance().getUserInfo().name);
        wkVBinding.tvID.setText(WKConfig.getInstance().getUserInfo().short_no);
        wkVBinding.avatarView.showAvatar(WKConfig.getInstance().getUid(), WKChannelType.PERSONAL);
        if (null != adapter) {
            try {
                WKCommonModel.getInstance().getAppNewVersion(false, version -> {
                    int index = -1;
                    for (int i = 0; i < adapter.getData().size(); i++) {
                        if (getString(R.string.currency).equals(adapter.getData().get(i).text)) {
                            index = i;
                            break;
                        }
                    }
                    if (index != -1) {
                        if (version != null && !TextUtils.isEmpty(version.download_url)) {
                            if (!adapter.getData().get(index).isNewVersionIv) {
                                adapter.getData().get(index).setIsNewVersionIv(true);
                                adapter.notifyItemChanged(index);
                            }
                        } else if (adapter.getData().get(index).isNewVersionIv) {
                            adapter.getData().get(index).setIsNewVersionIv(false);
                            adapter.notifyItemChanged(index);
                        }
                    }
                });
            } catch (Exception e) {
                WKLogUtils.w("检查新版本错误");
            }
        }
        WKCommonModel.getInstance().getAppConfig((code, msg, wkappConfig) -> {
            if (code == HttpResponseCode.success) {
                if (adapter == null || WKReader.isEmpty(adapter.getData())) {
                    return;
                }
                if (wkappConfig.register_invite_on == 0) {
                    for (int i = 0; i < adapter.getData().size(); i++) {
                        if (!TextUtils.isEmpty(adapter.getData().get(i).sid) && adapter.getData().get(i).sid.equals("invite_code")) {
                            adapter.removeAt(i);
                            break;
                        }
                    }
                } else {
                    List<PersonalInfoMenu> endpoints = EndpointManager.getInstance().invokes(EndpointCategory.personalCenter, getActivity());
                    if (endpoints != null) {
                        adapter.setList(endpoints);
                    }
                }
            }
        });
    }
}
