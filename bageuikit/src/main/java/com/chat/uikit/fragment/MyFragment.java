package com.chat.uikit.fragment;

import android.content.Intent;
import android.text.TextUtils;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.base.BageBaseFragment;
import com.chat.base.common.BageCommonModel;
import com.chat.base.config.BageConfig;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.PersonalInfoMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.FragMyLayoutBinding;
import com.chat.uikit.user.MyInfoActivity;
import com.chat.uikit.user.UserQrActivity;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 2019-11-12 14:58
 * 我的
 */
public class MyFragment extends BageBaseFragment<FragMyLayoutBinding> {
    private static final int PERSONAL_GRID_SPAN = 3;

    private PersonalItemAdapter adapter;

    @Override
    protected FragMyLayoutBinding getViewBinding() {
        return FragMyLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        bageVBinding.recyclerView.setNestedScrollingEnabled(false);
        adapter = new PersonalItemAdapter(new ArrayList<>());
        initMyAdapter(bageVBinding.recyclerView, adapter);

        bageVBinding.idLeftTv.setText(String.format(getString(R.string.identity), getString(R.string.app_name)));

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
        bageVBinding.avatarView.setSize(90);
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        Theme.setPressedBackground(bageVBinding.qrIv);
    }

    @Override
    protected void initListener() {
        EndpointManager.getInstance().setMethod("my_fragment", EndpointCategory.bageRefreshPersonalCenter, object -> {
            reloadPersonalMenus();
            return null;
        });
        adapter.setOnItemClickListener((adapter1, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, view1 -> {
            PersonalInfoMenu menu = (PersonalInfoMenu) adapter1.getItem(position);
            if (menu != null && menu.iPersonalInfoMenuClick != null) {
                menu.iPersonalInfoMenuClick.onClick();
            }
        }));
        SingleClickUtil.onSingleClick(bageVBinding.avatarView, view -> gotoMyInfo());
        SingleClickUtil.onSingleClick(bageVBinding.qrIv, view -> {
            startActivity(new Intent(requireActivity(), UserQrActivity.class));
        });

        BageCommonModel.getInstance().getChannel(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL, (code, msg, entity) -> {
            if (entity != null && entity.extra != null) {
                Object shortNoObject = entity.extra.get("short_no");
                if (shortNoObject != null) {
                    String shortNo = (String) shortNoObject;
                    bageVBinding.tvID.setText(shortNo);
                    bageVBinding.nameTv.setText(entity.name);
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
                        && BageConfig.getInstance().getAppConfig().register_invite_on == 0) {
                    endpoints.remove(i);
                    break;
                }
            }
            adapter.setList(endpoints);
        }
    }

    void gotoMyInfo() {
//        String str = BageDeviceUtils.getSignature(getActivity());
//        Log.e("签名",str+"");
        startActivity(new Intent(getActivity(), MyInfoActivity.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        bageVBinding.nameTv.setText(BageConfig.getInstance().getUserInfo().name);
        bageVBinding.tvID.setText(BageConfig.getInstance().getUserInfo().short_no);
        bageVBinding.avatarView.showAvatar(BageConfig.getInstance().getUid(), BageChannelType.PERSONAL);
        if (null != adapter) {
            try {
                BageCommonModel.getInstance().getAppNewVersion(false, version -> {
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
                BageLogUtils.w("检查新版本错误");
            }
        }
        BageCommonModel.getInstance().getAppConfig((code, msg, bageappConfig) -> {
            if (code == HttpResponseCode.success) {
                if (adapter == null || BageReader.isEmpty(adapter.getData())) {
                    return;
                }
                if (bageappConfig.register_invite_on == 0) {
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
