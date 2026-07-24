package com.chat.uikit.search;

import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.chat.manager.BageIMUtils;
import com.chat.uikit.databinding.ActCommonListLayoutBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageMessageSearchResult;
import com.bage.im.entity.BageMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020-08-30 18:41
 * 搜索消息结果
 */
public class SearchMsgResultActivity extends BageBaseActivity<ActCommonListLayoutBinding> {

    BageMessageSearchResult result;
    SearchMsgResultAdapter adapter;

    @Override
    protected ActCommonListLayoutBinding getViewBinding() {
        return ActCommonListLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(result.bageChannel.channelName);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        result = getIntent().getParcelableExtra("result");
        String searchKey = getIntent().getStringExtra("searchKey");
        adapter = new SearchMsgResultAdapter(searchKey, new ArrayList<>());
        initAdapter(bageVBinding.recyclerView, adapter);
        List<BageMsg> msgList = BageIM.getInstance().getMsgManager().searchWithChannel(searchKey,result.bageChannel.channelID, result.bageChannel.channelType);
        adapter.setList(msgList);
    }

    @Override
    protected void initListener() {
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, view2 -> {
            BageMsg msg = (BageMsg) adapter1.getItem(position);
            if (msg != null) {
                BageIMUtils.getInstance().startChatActivity(new ChatViewMenu(this, msg.channelID, msg.channelType, msg.orderSeq, false));
            }
        }));
    }
}
