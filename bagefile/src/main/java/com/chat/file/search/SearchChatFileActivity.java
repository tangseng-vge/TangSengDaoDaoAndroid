package com.chat.file.search;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.ui.Theme;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.StringUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.file.ChatFileActivity;
import com.chat.file.R;
import com.chat.file.databinding.ActSearchChatFileLayoutBinding;
import com.chat.file.msgitem.FileContent;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageChannelType;

import java.util.ArrayList;
import java.util.List;

/**
 * 3/22/21 2:12 PM
 * 搜索聊天文件
 */
public class SearchChatFileActivity extends BageBaseActivity<ActSearchChatFileLayoutBinding> {
    private String channelID;
    private byte channelType;
    private long oldestOrderSeq = 0;
    private final int[] types = new int[]{BageContentType.Bage_FILE};
    private SearchFileAdapter adapter;

    @Override
    protected ActSearchChatFileLayoutBinding getViewBinding() {
        return ActSearchChatFileLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.str_file_file);
    }

    @Override
    protected void initPresenter() {
        channelID = getIntent().getStringExtra("channel_id");
        channelType = getIntent().getByteExtra("channel_type", BageChannelType.PERSONAL);
    }

    @Override
    protected void initView() {
        adapter = new SearchFileAdapter();
        initAdapter(bageVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        getData();
        bageVBinding.spinKit.setColor(Theme.colorAccount);
        bageVBinding.refreshLayout.setEnableRefresh(false);
        bageVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                oldestOrderSeq = adapter.getData().get(adapter.getData().size() - 1).oldestOrderSeq;
                getData();
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {

            }
        });
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view -> {
            SearchFileEntity entity = (SearchFileEntity) adapter1.getData().get(position);
            if (entity != null) {
                Intent intent = new Intent(SearchChatFileActivity.this, ChatFileActivity.class);
                intent.putExtra("clientMsgNo", entity.msg.clientMsgNO);
                startActivity(intent);
            }
        }));
    }

    private void getData() {
        List<BageMsg> list = BageIM.getInstance().getMsgManager().searchMsgWithChannelAndContentTypes(channelID, channelType, oldestOrderSeq, 20, types);
        if (BageReader.isNotEmpty(list)) {
            bageVBinding.noDataTv.setVisibility(View.GONE);
            List<SearchFileEntity> fileEntityList = new ArrayList<>();
            for (BageMsg msg : list) {
                SearchFileEntity entity = new SearchFileEntity();
                entity.UID = msg.fromUID;
                entity.oldestOrderSeq = msg.orderSeq;
                if (channelType == BageChannelType.PERSONAL) {
                    if (msg.getFrom() != null) {
                        entity.avatarCacheKey = msg.getFrom().avatarCacheKey;
                        entity.userName = msg.getFrom().channelName;
                    }
                } else {
                    if (msg.getMemberOfFrom() != null) {
                        entity.avatarCacheKey = msg.getMemberOfFrom().memberAvatarCacheKey;
                        entity.userName = msg.getMemberOfFrom().memberName;
                    }
                }
                entity.date = BageTimeUtils.getInstance().time2YearMonth(msg.timestamp * 1000);
                entity.time = BageTimeUtils.getInstance().time2DataDay(msg.timestamp * 1000);
                entity.msg = msg;
                FileContent fileContent = (FileContent) msg.baseContentMsgModel;
                if (fileContent != null) {
                    entity.fileName = fileContent.name;
                    entity.fileSize = StringUtils.sizeFormatNum2String(fileContent.size);
                    if (fileContent.name.contains(".")) {
                        String type = fileContent.name.substring(fileContent.name.lastIndexOf(".") + 1);
                        if (!TextUtils.isEmpty(type))
                            entity.fileType = type.toUpperCase();
                        else entity.fileType = getString(R.string.unknown_file);
                    } else entity.fileType = getString(R.string.unknown_file);
                }
                fileEntityList.add(entity);
            }
            adapter.addData(fileEntityList);
            bageVBinding.refreshLayout.finishLoadMore();
        } else {
            bageVBinding.refreshLayout.finishLoadMoreWithNoMoreData();
            if (oldestOrderSeq == 0) {
                bageVBinding.noDataTv.setVisibility(View.VISIBLE);
                bageVBinding.refreshLayout.setEnableLoadMore(false);
            }
        }

    }
}
