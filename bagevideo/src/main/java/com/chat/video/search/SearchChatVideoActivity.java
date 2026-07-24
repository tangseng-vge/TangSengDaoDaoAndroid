package com.chat.video.search;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import com.chat.base.act.PlayVideoActivity;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.base.views.FullyGridLayoutManager;
import com.chat.base.views.pinnedsectionitemdecoration.PinnedHeaderItemDecoration;
import com.chat.video.R;
import com.chat.video.databinding.ActSearchChatVideoLayoutBinding;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMsg;
import com.bage.im.entity.BageChannelType;
import com.bage.im.msgmodel.BageVideoContent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 3/23/21 12:32 PM
 * 查询聊天视频
 */
public class SearchChatVideoActivity extends BageBaseActivity<ActSearchChatVideoLayoutBinding> {
    private String channelID;
    private byte channelType;
    private long oldestOrderSeq = 0;
    private final int[] types = new int[]{BageContentType.Bage_VIDEO};
    private SearchChatVideoAdapter adapter;

    @Override
    protected ActSearchChatVideoLayoutBinding getViewBinding() {
        return ActSearchChatVideoLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.bage_video);
    }

    @Override
    protected void initPresenter() {
        channelID = getIntent().getStringExtra("channel_id");
        channelType = getIntent().getByteExtra("channel_type", BageChannelType.PERSONAL);
    }

    @Override
    protected void initView() {
        bageVBinding.spinKit.setColor(Theme.colorAccount);
        PinnedHeaderItemDecoration mHeaderItemDecoration = new PinnedHeaderItemDecoration.Builder(1).enableDivider(false).create();
        int wH = (AndroidUtilities.getScreenWidth() - AndroidUtilities.dp( 6)) / 4;
        FullyGridLayoutManager layoutManager = new FullyGridLayoutManager(this, 4);
        bageVBinding.recyclerView.setLayoutManager(layoutManager);
        adapter = new SearchChatVideoAdapter(wH, searchChatVideoEntity -> EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(SearchChatVideoActivity.this, channelID, channelType, searchChatVideoEntity.oldestOrderSeq, false)));
        bageVBinding.recyclerView.setAdapter(adapter);
        bageVBinding.recyclerView.addItemDecoration(mHeaderItemDecoration);
    }

    @Override
    protected void initListener() {
        getData();
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
        adapter.addChildClickViewIds(R.id.imageView);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view2 -> {
            SearchChatVideoEntity entity = (SearchChatVideoEntity) adapter1.getData().get(position);
            if (entity != null && entity.getItemType() == 0) {
                @SuppressWarnings("unchecked") ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(SearchChatVideoActivity.this, new Pair<>(view1, "coverIv"));
                Intent intent = new Intent(this, PlayVideoActivity.class);
                intent.putExtra("coverImg", BageApiConfig.getShowUrl(entity.coverUrl));
                intent.putExtra("url", entity.videoUrl);
                startActivity(intent, activityOptions.toBundle());
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            }
        }));
    }


    private void getData() {
        List<BageMsg> list = BageIM.getInstance().getMsgManager().searchMsgWithChannelAndContentTypes(channelID, channelType, oldestOrderSeq, 120, types);
        if (BageReader.isNotEmpty(list)) {
            List<SearchChatVideoEntity> fileEntityList = new ArrayList<>();

            bageVBinding.refreshLayout.finishLoadMore();
            // 构造数据
            for (BageMsg msg : list) {
                String date = BageTimeUtils.getInstance().time2YearMonth(msg.timestamp * 1000);
                if (BageReader.isNotEmpty(fileEntityList)) {
                    if (!fileEntityList.get(fileEntityList.size() - 1).date.equals(date)) {
                        SearchChatVideoEntity entity = new SearchChatVideoEntity();
                        entity.date = date;
                        entity.itemType = 1;
                        fileEntityList.add(entity);
                    }
                } else {
                    SearchChatVideoEntity entity = new SearchChatVideoEntity();
                    entity.date = date;
                    entity.itemType = 1;
                    fileEntityList.add(entity);
                }
                SearchChatVideoEntity entity = new SearchChatVideoEntity();
                entity.date = date;
                entity.messageContent = msg.baseContentMsgModel;
                entity.oldestOrderSeq = msg.orderSeq;
                BageVideoContent videoContent = (BageVideoContent) msg.baseContentMsgModel;
                String showUrl = "";
                if (!TextUtils.isEmpty(videoContent.localPath)) {
                    File file = new File(videoContent.localPath);
                    if (file.exists()) {
                        showUrl = videoContent.localPath;
                    }
                }
                if (TextUtils.isEmpty(showUrl)) {
                    showUrl = BageApiConfig.getShowUrl(videoContent.url);
                }
                entity.videoUrl = showUrl;
                if (videoContent.second > 0) {
                    //分
                    int minute = (int) (videoContent.second / (60));
                    //秒
                    int second = (int) (videoContent.second % 60);
                    String showMinute = minute < 10 ? ("0" + minute) : minute + "";
                    String showSecond = second < 10 ? ("0" + second) : second + "";
                    entity.second = String.format("%s:%s", showMinute, showSecond);
                }
                entity.coverUrl = BageApiConfig.getShowUrl(videoContent.cover);
                fileEntityList.add(entity);
            }
            if (BageReader.isNotEmpty(adapter.getData()) && BageReader.isNotEmpty(fileEntityList)) {
                SearchChatVideoEntity entity = adapter.getData().get(adapter.getData().size() - 1);
                if (entity.date.equals(fileEntityList.get(0).date)) {
                    fileEntityList.remove(0);
                }
            }
            adapter.addData(fileEntityList);
            bageVBinding.noDataTv.setVisibility(View.GONE);
        } else {
            bageVBinding.refreshLayout.finishLoadMoreWithNoMoreData();
            if (oldestOrderSeq == 0) {
                bageVBinding.refreshLayout.setEnableLoadMore(false);
                bageVBinding.noDataTv.setVisibility(View.VISIBLE);
            }
        }

    }
}
