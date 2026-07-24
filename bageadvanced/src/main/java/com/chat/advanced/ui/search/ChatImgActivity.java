package com.chat.advanced.ui.search;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.chat.advanced.R;
import com.chat.advanced.databinding.ActSearchChatImgLayoutBinding;
import com.chat.base.base.BageBaseActivity;
import com.chat.base.config.BageApiConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatChooseContacts;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.endpoint.entity.ChooseChatMenu;
import com.chat.base.entity.ImagePopupBottomSheetItem;
import com.chat.base.msgitem.BageContentType;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageDialogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.base.views.CustomImageViewerPopup;
import com.chat.base.views.FullyGridLayoutManager;
import com.chat.base.views.pinnedsectionitemdecoration.PinnedHeaderItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMsg;
import com.bage.im.msgmodel.BageImageContent;
import com.bage.im.msgmodel.BageMessageContent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 3/23/21 10:07 AM
 * 搜索聊天图片
 */
public class ChatImgActivity extends BageBaseActivity<ActSearchChatImgLayoutBinding> {
    private String channelID;
    private byte channelType;
    private long oldestOrderSeq = 0;
    private final int[] types = new int[]{BageContentType.Bage_IMAGE};
    private ChatImgAdapter adapter;

    @Override
    protected ActSearchChatImgLayoutBinding getViewBinding() {
        return ActSearchChatImgLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.image);
    }

    @Override
    protected void initPresenter() {
        channelID = getIntent().getStringExtra("channel_id");
        channelType = getIntent().getByteExtra("channel_type", BageChannelType.PERSONAL);
    }

    @Override
    protected void initView() {
        PinnedHeaderItemDecoration mHeaderItemDecoration = new PinnedHeaderItemDecoration.Builder(1).enableDivider(false).create();
        int wH = (AndroidUtilities.getScreenWidth() - AndroidUtilities.dp(6)) / 4;
        FullyGridLayoutManager layoutManager = new FullyGridLayoutManager(this, 4);
        bageVBinding.recyclerView.setLayoutManager(layoutManager);
        adapter = new ChatImgAdapter(wH, new ChatImgAdapter.ICLick() {
            @Override
            public void onClick(ChatImgEntity searchChatImgEntity) {
                EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(ChatImgActivity.this, channelID, channelType, searchChatImgEntity.oldestOrderSeq, false));
            }

            @Override
            public void onForward(ChatImgEntity searchChatImgEntity) {
                forward(searchChatImgEntity);
            }
        });
        bageVBinding.recyclerView.setAdapter(adapter);
        bageVBinding.recyclerView.addItemDecoration(mHeaderItemDecoration);
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
        adapter.addChildClickViewIds(R.id.imageView);
        adapter.setOnItemChildClickListener((adapter1, view1, position) -> {
            ChatImgEntity entity = (ChatImgEntity) adapter1.getData().get(position);
            if (entity != null && entity.getItemType() == 0) {
                showImg(entity.url, (ImageView) view1);
            }
        });
    }

    private void getData() {
        List<BageMsg> list = BageIM.getInstance().getMsgManager().searchMsgWithChannelAndContentTypes(channelID, channelType, oldestOrderSeq, 120, types);
        if (BageReader.isNotEmpty(list)) {
            List<ChatImgEntity> fileEntityList = new ArrayList<>();

            bageVBinding.refreshLayout.finishLoadMore();
            // 构造数据
            for (BageMsg msg : list) {
                String date = BageTimeUtils.getInstance().time2YearMonth(msg.timestamp * 1000);
                if (BageReader.isNotEmpty(fileEntityList)) {
                    if (!fileEntityList.get(fileEntityList.size() - 1).date.equals(date)) {
                        ChatImgEntity entity = new ChatImgEntity();
                        entity.date = date;
                        entity.itemType = 1;
                        fileEntityList.add(entity);
                    }
                } else {
                    ChatImgEntity entity = new ChatImgEntity();
                    entity.date = date;
                    entity.itemType = 1;
                    fileEntityList.add(entity);
                }
                ChatImgEntity entity = new ChatImgEntity();
                entity.date = date;
                entity.clientMsgNo = msg.clientMsgNO;
                entity.messageContent = msg.baseContentMsgModel;
                entity.oldestOrderSeq = msg.orderSeq;
                BageImageContent msgModel = (BageImageContent) msg.baseContentMsgModel;
                String showUrl = "";
                if (!TextUtils.isEmpty(msgModel.localPath)) {
                    File file = new File(msgModel.localPath);
                    if (file.exists()) {
                        showUrl = msgModel.localPath;
                    }
                }
                if (TextUtils.isEmpty(showUrl)) {
                    showUrl = BageApiConfig.getShowUrl(msgModel.url);
                }
                entity.url = showUrl;
                fileEntityList.add(entity);
            }
            if (BageReader.isNotEmpty(adapter.getData())) {
                ChatImgEntity entity = adapter.getData().get(adapter.getData().size() - 1);
                if (entity.date.equals(fileEntityList.get(0).date)) {
                    fileEntityList.remove(0);
                }
            }
            adapter.addData(fileEntityList);
        } else {
            bageVBinding.refreshLayout.finishLoadMoreWithNoMoreData();
            if (oldestOrderSeq == 0) {
                bageVBinding.refreshLayout.setEnableLoadMore(false);
                bageVBinding.nodataTv.setVisibility(View.VISIBLE);
            }
        }

    }


    private void showImg(String uri, ImageView imageView) {
        //查看大图
        List<Object> tempImgList = new ArrayList<>();
        List<Object> urlList = new ArrayList<>();
        List<ImageView> imgList = new ArrayList<>();
        for (int i = 0, size = adapter.getData().size(); i < size; i++) {
            if (adapter.getData().get(i).getItemType() == 0) {
                tempImgList.add(adapter.getData().get(i));
                urlList.add(adapter.getData().get(i).url);
                ImageView imageView1 = (ImageView) adapter.getViewByPosition(i, R.id.imageView);
                imgList.add(imageView1);
            }
        }
        int index = 0;
        for (int i = 0; i < tempImgList.size(); i++) {
            ChatImgEntity entity = (ChatImgEntity) tempImgList.get(i);
            if (entity.url.equals(uri)) {
                index = i;
                break;
            }
        }

        List<ImagePopupBottomSheetItem> bottomEntityArrayList = new ArrayList<>();
        bottomEntityArrayList.add(new ImagePopupBottomSheetItem(getString(R.string.forward), R.mipmap.msg_forward, position -> {
            ChatImgEntity entity = (ChatImgEntity) tempImgList.get(position);
            if (entity == null || entity.messageContent == null) return;
            forward(entity);
        }));
        bottomEntityArrayList.add(new ImagePopupBottomSheetItem(getString(R.string.go_to_chat_item), R.mipmap.msg_message, position -> {
            ChatImgEntity entity = (ChatImgEntity) tempImgList.get(position);
            EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(ChatImgActivity.this, channelID, channelType, entity.oldestOrderSeq, false));
        }));
        BageDialogUtils.getInstance().showImagePopup(this, urlList, imgList, imageView, index, bottomEntityArrayList, new CustomImageViewerPopup.IImgPopupMenu() {
            @Override
            public void onForward(int position) {
            }

            @Override
            public void onFavorite(int position) {
                ChatImgEntity entity = (ChatImgEntity) tempImgList.get(position);
                BageMsg msg = BageIM.getInstance().getMsgManager().getWithClientMsgNO(entity.clientMsgNo);
                if (msg != null) {
                    collect(msg);
                }
            }

            @Override
            public void onShowInChat(int position) {
                ChatImgEntity entity = (ChatImgEntity) tempImgList.get(position);
                EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(ChatImgActivity.this, channelID, channelType, entity.oldestOrderSeq, false));
            }
        }, null);

    }


    private void collect(BageMsg msg) {
        JSONObject jsonObject = new JSONObject();
        BageImageContent msgModel = (BageImageContent) msg.baseContentMsgModel;
        jsonObject.put("content", BageApiConfig.getShowUrl(msgModel.url));
        jsonObject.put("width", msgModel.width);
        jsonObject.put("height", msgModel.height);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("type", msg.type);
        String unique_key = msg.messageID;
        if (TextUtils.isEmpty(unique_key))
            unique_key = msg.clientMsgNO;
        hashMap.put("unique_key", unique_key);
        if (msg.getFrom() != null) {
            hashMap.put("author_uid", msg.getFrom().channelID);
            hashMap.put("author_name", msg.getFrom().channelName);
        }
        hashMap.put("payload", jsonObject);
        hashMap.put("activity", this);
        EndpointManager.getInstance().invoke("favorite_add", hashMap);
    }

    private void forward(ChatImgEntity entity) {
        BageMessageContent finalBageMessageContent = entity.messageContent;
        EndpointManager.getInstance().invoke(EndpointSID.showChooseChatView, new ChooseChatMenu(new ChatChooseContacts(list1 -> {
            if (BageReader.isNotEmpty(list1)) {
                for (BageChannel channel : list1) {
                    BageIM.getInstance().getMsgManager().send(finalBageMessageContent, channel);
                }
                ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content).getRootView();
                Snackbar.make(viewGroup, getString(R.string.is_forward), 1000)
                        .setAction("", v1 -> {
                        })
                        .show();
            }
        }), entity.messageContent));

    }
}
