package com.chat.moments.activities;

import android.content.Intent;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChooseContactsMenu;
import com.chat.base.endpoint.entity.ChooseLabelEntity;
import com.chat.base.endpoint.entity.ChooseLabelMenu;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageReader;
import com.chat.moments.R;
import com.chat.moments.adapter.ChooseLabelAdapter;
import com.chat.moments.databinding.ActMomentsVisibleRangeLayoutBinding;
import com.chat.moments.entity.MomentsRange;
import com.bage.im.entity.BageChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-11-12 13:32
 * 选择动态可见范围
 */
public class MomentsVisibleRangeActivity extends BageBaseActivity<ActMomentsVisibleRangeLayoutBinding> {
    private List<BageChannel> partiallyUsers;
    private List<BageChannel> invisibleUsers;
    //部分可见标签
    private ChooseLabelAdapter partiallyLabelAdapter;
    //不可见标签
    private ChooseLabelAdapter invisibleLabelAdapter;
    List<ChooseLabelEntity> labels;


    @Override
    protected ActMomentsVisibleRangeLayoutBinding getViewBinding() {
        return ActMomentsVisibleRangeLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.visible_range);
    }

    @Override
    protected String getRightBtnText(Button titleRightBtn) {
        return getString(R.string.b_complete);
    }

    @Override
    protected void rightButtonClick() {
        super.rightButtonClick();
        int visibleRangeType = MomentsRange.PUBLIC;
        List<BageChannel> list = null;
        List<ChooseLabelEntity> labels = new ArrayList<>();
        if (bageVBinding.privateIv.isSelected()) visibleRangeType = MomentsRange.PRIVATE;
        if (bageVBinding.partiallyIv.isSelected()) {
            list = partiallyUsers;
            visibleRangeType = MomentsRange.PARTIALLY;

            for (int i = 0, size = partiallyLabelAdapter.getData().size(); i < size; i++) {
                if (partiallyLabelAdapter.getData().get(i).isSelected) {
                    labels.add(partiallyLabelAdapter.getData().get(i));
                }
            }
        }
        if (bageVBinding.invisibleIv.isSelected()) {
            list = invisibleUsers;
            visibleRangeType = MomentsRange.GONE;
            for (int i = 0, size = invisibleLabelAdapter.getData().size(); i < size; i++) {
                if (invisibleLabelAdapter.getData().get(i).isSelected) {
                    labels.add(invisibleLabelAdapter.getData().get(i));
                }
            }
        }
        Intent intent = new Intent();
        intent.putExtra("visibleRangeType", visibleRangeType);
        intent.putParcelableArrayListExtra("list", (ArrayList<? extends Parcelable>) list);
        intent.putParcelableArrayListExtra("labels", (ArrayList<? extends Parcelable>) labels);

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        bageVBinding.partiallyMaillistNamesTv.setTextColor(Theme.colorAccount);
        partiallyLabelAdapter = new ChooseLabelAdapter(1, new ArrayList<>());
        initAdapter(bageVBinding.partiallyLabelRecyclerView, partiallyLabelAdapter);
        bageVBinding.partiallyLabelRecyclerView.setNestedScrollingEnabled(false);

        invisibleLabelAdapter = new ChooseLabelAdapter(2, new ArrayList<>());
        initAdapter(bageVBinding.invisibelLabelRecyclerView, invisibleLabelAdapter);
        bageVBinding.invisibelLabelRecyclerView.setNestedScrollingEnabled(false);
        int visibleRangeType = getIntent().getIntExtra("visibleRangeType", MomentsRange.PUBLIC);
        List<BageChannel> list = getIntent().getParcelableArrayListExtra("list");
        labels = getIntent().getParcelableArrayListExtra("labels");
        if (visibleRangeType == 0) {
            bageVBinding.publicIv.setSelected(true);
        } else if (visibleRangeType == 1) {
            bageVBinding.privateIv.setSelected(true);
        } else if (visibleRangeType == 2) {
            bageVBinding.partiallyIv.setSelected(true);
            partiallyUsers = list;
            setPartiallyUsers();
            bageVBinding.partiallyEb.setExpanded(true);
        } else {
            bageVBinding.invisibleIv.setSelected(true);
            invisibleUsers = list;
            setInvisibleUsers();
            bageVBinding.invisibleEb.setExpanded(true);
        }
        chooseLabelUsers(visibleRangeType, false);
    }

    @Override
    protected void initListener() {
        invisibleLabelAdapter.setOnItemClickListener((adapter, view1, position) -> {
            ChooseLabelEntity entity = (ChooseLabelEntity) adapter.getData().get(position);
            if (entity != null) {
                entity.isSelected = !entity.isSelected;
                adapter.notifyItemChanged(position);
            }
        });
        partiallyLabelAdapter.setOnItemClickListener((adapter, view1, position) -> {
            ChooseLabelEntity entity = (ChooseLabelEntity) adapter.getData().get(position);
            if (entity != null) {
                entity.isSelected = !entity.isSelected;
                adapter.notifyItemChanged(position);
            }
        });
        bageVBinding.partiallyLayout.setOnClickListener(v -> {
            bageVBinding.partiallyEb.setExpanded(!bageVBinding.partiallyEb.isExpanded());
            setSelected(bageVBinding.partiallyIv);
            bageVBinding.invisibleEb.setExpanded(false);
        });
        bageVBinding.invisibleLayout.setOnClickListener(v -> {
            bageVBinding.invisibleEb.setExpanded(!bageVBinding.invisibleEb.isExpanded());
            setSelected(bageVBinding.invisibleIv);
            bageVBinding.partiallyEb.setExpanded(false);
        });
        bageVBinding.privateLayout.setOnClickListener(v -> {
            setSelected(bageVBinding.privateIv);
            bageVBinding.partiallyEb.setExpanded(false);
            bageVBinding.invisibleEb.setExpanded(false);
        });
        bageVBinding.publicLayout.setOnClickListener(v -> {
            setSelected(bageVBinding.publicIv);
            bageVBinding.partiallyEb.setExpanded(false);
            bageVBinding.invisibleEb.setExpanded(false);
        });
        bageVBinding.partiallyMaillistLayout.setOnClickListener(v -> {
            //部分可见从通讯录选择
            chooseContacts(partiallyUsers, 1);
        });
        bageVBinding.invisibleMaillistLayout.setOnClickListener(v -> {
            //不可见从通讯录选择
            chooseContacts(invisibleUsers, 2);
        });
        EndpointManager.getInstance().setMethod("refresh_label_list", object -> {
            if (bageVBinding.partiallyEb.isExpanded()) {
                chooseLabelUsers(MomentsRange.PARTIALLY, true);
            }
            if (bageVBinding.invisibleEb.isExpanded()) {
                chooseLabelUsers(MomentsRange.GONE, true);
            }
            return null;
        });
    }


    private void setSelected(ImageView imageView) {
        bageVBinding.publicIv.setSelected(false);
        bageVBinding.privateIv.setSelected(false);
        bageVBinding.invisibleIv.setSelected(false);
        bageVBinding.partiallyIv.setSelected(false);
        imageView.setSelected(true);
    }

    private void chooseContacts(List<BageChannel> list, int type) {
        EndpointManager.getInstance().invoke("choose_contacts", new ChooseContactsMenu(-1, true, true, list, selectedList -> {
            if (type == 1) {
                partiallyUsers = selectedList;
                setPartiallyUsers();
            } else {
                invisibleUsers = selectedList;
                setInvisibleUsers();
            }
        }));
    }

    private void setPartiallyUsers() {
        if (BageReader.isEmpty(partiallyUsers)) {
            bageVBinding.partiallyMaillistNamesTv.setText("");
            bageVBinding.partiallyMaillistNamesTv.setVisibility(View.GONE);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0, size = partiallyUsers.size(); i < size; i++) {
                if (TextUtils.isEmpty(stringBuilder)) {
                    stringBuilder.append(partiallyUsers.get(i).channelName);
                } else
                    stringBuilder.append("、").append(partiallyUsers.get(i).channelName);
            }
            bageVBinding.partiallyMaillistNamesTv.setText(stringBuilder);
            bageVBinding.partiallyMaillistNamesTv.setVisibility(View.VISIBLE);
        }
    }

    private void setInvisibleUsers() {
        if (BageReader.isEmpty(invisibleUsers)) {
            bageVBinding.invisibleMaillistNamesTv.setText("");
            bageVBinding.invisibleMaillistNamesTv.setVisibility(View.GONE);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0, size = invisibleUsers.size(); i < size; i++) {
                if (TextUtils.isEmpty(stringBuilder)) {
                    stringBuilder.append(invisibleUsers.get(i).channelName);
                } else
                    stringBuilder.append("、").append(invisibleUsers.get(i).channelName);
            }
            bageVBinding.invisibleMaillistNamesTv.setText(stringBuilder);
            bageVBinding.invisibleMaillistNamesTv.setVisibility(View.VISIBLE);
        }
    }

    void chooseLabelUsers(int visibleRangeType, boolean isSelectMax) {
        EndpointManager.getInstance().invoke("choose_label", new ChooseLabelMenu(list -> {
            List<ChooseLabelEntity> invisibleTempList = new ArrayList<>();
            int maxId = 0;
            for (int i = 0, size = list.size(); i < size; i++) {
                if (!TextUtils.isEmpty(list.get(i).labelId)) {
                    int id = Integer.parseInt(list.get(i).labelId);
                    if (id > maxId) maxId = id;
                }
                ChooseLabelEntity entity = new ChooseLabelEntity();
                entity.labelId = list.get(i).labelId;
                entity.labelName = list.get(i).labelName;
                entity.members = list.get(i).members;
                boolean isSelected = false;
                if (visibleRangeType == MomentsRange.GONE && BageReader.isNotEmpty(labels)) {
                    for (ChooseLabelEntity chooseLabelEntity : labels) {
                        if (chooseLabelEntity.labelId.equals(entity.labelId)) {
                            isSelected = true;
                            break;
                        }
                    }
                }
                entity.isSelected = isSelected;
                invisibleTempList.add(entity);
            }
            if (isSelectMax && BageReader.isNotEmpty(invisibleTempList)) {
                for (ChooseLabelEntity entity : invisibleTempList) {
                    entity.isSelected = entity.labelId.equals(maxId + "");
                }
                invisibleUsers = null;
                setInvisibleUsers();
            }
            invisibleLabelAdapter.setList(invisibleTempList);
            List<ChooseLabelEntity> tempList = new ArrayList<>();
            for (int i = 0, size = list.size(); i < size; i++) {
                ChooseLabelEntity entity = new ChooseLabelEntity();
                entity.labelId = list.get(i).labelId;
                entity.labelName = list.get(i).labelName;
                entity.members = list.get(i).members;
                boolean isSelected = false;
                if (visibleRangeType == MomentsRange.PARTIALLY && BageReader.isNotEmpty(labels)) {
                    for (ChooseLabelEntity chooseLabelEntity : labels) {
                        if (chooseLabelEntity.labelId.equals(entity.labelId)) {
                            isSelected = true;
                            break;
                        }
                    }
                }
                entity.isSelected = isSelected;
                tempList.add(entity);
            }
            if (isSelectMax && BageReader.isNotEmpty(tempList)) {
                for (ChooseLabelEntity entity : tempList) {
                    entity.isSelected = entity.labelId.equals(maxId + "");
                }
                partiallyUsers = null;
                setPartiallyUsers();
            }
            partiallyLabelAdapter.setList(tempList);
        }));
    }
}
