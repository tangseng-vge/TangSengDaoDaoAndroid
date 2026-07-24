package com.chat.uikit.setting;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.HanziToPinyin;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActBlackListLayoutBinding;
import com.chat.uikit.enity.BlackListEntity;
import com.chat.uikit.setting.adapter.BlackListAdapter;
import com.chat.uikit.user.UserDetailActivity;
import com.chat.uikit.user.service.UserModel;
import com.chat.uikit.utils.PyingUtils;

import java.util.ArrayList;
import java.util.List;

public class BlackListActivity extends BageBaseActivity<ActBlackListLayoutBinding> {

    private BlackListAdapter adapter;

    @Override
    protected ActBlackListLayoutBinding getViewBinding() {
        return ActBlackListLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.black_list);
    }

    @Override
    protected void initView() {
        adapter = new BlackListAdapter(new ArrayList<>());
        initAdapter(bageVBinding.recyclerView, adapter);
        bageVBinding.refreshLayout.setEnableOverScrollDrag(true);
        bageVBinding.refreshLayout.setEnableLoadMore(false);
    }

    @Override
    protected void initListener() {
        bageVBinding.refreshLayout.setOnRefreshListener(refreshLayout -> loadBlackList());
        adapter.addChildClickViewIds(R.id.removeTv);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> {
            BlackListEntity entity = adapter.getItem(position);
            if (entity == null) return;
            BageDialogUtils.getInstance().showDialog(this,
                    getString(R.string.pull_out_black_list),
                    getString(R.string.pull_out_black_list_tips),
                    true, "", "", 0, 0, index -> {
                        if (index == 1) {
                            removeFromBlackList(entity);
                        }
                    });
        });
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view -> {
            BlackListEntity entity = adapter.getItem(position);
            if (entity != null && !TextUtils.isEmpty(entity.uid)) {
                Intent intent = new Intent(this, UserDetailActivity.class);
                intent.putExtra("uid", entity.uid);
                startActivity(intent);
            }
        }));
    }

    @Override
    protected void initData() {
        super.initData();
        loadBlackList();
    }

    private void loadBlackList() {
        boolean showCenterLoading = !bageVBinding.refreshLayout.isRefreshing() && BageReader.isEmpty(adapter.getData());
        if (showCenterLoading) {
            loadingPopup.show();
        }
        UserModel.getInstance().getBlackLists((code, msg, list) -> {
            bageVBinding.refreshLayout.finishRefresh();
            loadingPopup.dismiss();
            if (code != HttpResponseCode.success) {
                if (!TextUtils.isEmpty(msg)) {
                    showToast(msg);
                }
                return;
            }
            List<BlackListEntity> data = sortBlackList(list == null ? new ArrayList<>() : list);
            adapter.setList(data);
            bageVBinding.nodataTv.setVisibility(BageReader.isEmpty(data) ? View.VISIBLE : View.GONE);
        });
    }

    private List<BlackListEntity> sortBlackList(List<BlackListEntity> list) {
        if (BageReader.isEmpty(list)) {
            return list;
        }
        for (int i = 0, size = list.size(); i < size; i++) {
            BlackListEntity entity = list.get(i);
            String showName = !TextUtils.isEmpty(entity.name) ? entity.name : entity.username;
            if (!TextUtils.isEmpty(showName)) {
                if (PyingUtils.getInstance().isStartNum(showName)) {
                    entity.pying = "#";
                } else {
                    entity.pying = HanziToPinyin.getInstance().getPY(showName);
                }
            } else {
                entity.pying = "#";
            }
        }
        PyingUtils.getInstance().sortBlackList(list);

        List<BlackListEntity> letterList = new ArrayList<>();
        List<BlackListEntity> numList = new ArrayList<>();
        List<BlackListEntity> otherList = new ArrayList<>();
        for (BlackListEntity entity : list) {
            if (PyingUtils.getInstance().isStartLetter(entity.pying)) {
                letterList.add(entity);
            } else if (PyingUtils.getInstance().isStartNum(entity.pying)) {
                numList.add(entity);
            } else {
                otherList.add(entity);
            }
        }
        List<BlackListEntity> sortedList = new ArrayList<>();
        sortedList.addAll(letterList);
        sortedList.addAll(numList);
        sortedList.addAll(otherList);
        return sortedList;
    }

    private void removeFromBlackList(BlackListEntity entity) {
        UserModel.getInstance().removeBlackList(entity.uid, (code, msg) -> {
            if (code != HttpResponseCode.success) {
                if (!TextUtils.isEmpty(msg)) {
                    showToast(msg);
                }
                return;
            }
            loadBlackList();
        });
    }
}
