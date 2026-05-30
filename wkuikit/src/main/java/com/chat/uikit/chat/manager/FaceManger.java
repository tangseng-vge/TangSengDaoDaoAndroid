package com.chat.uikit.chat.manager;

import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.chat.base.utils.singleclick.SingleClickUtil;

import com.chat.base.config.WKConstants;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatFunctionMenu;
import com.chat.base.msg.IConversationContext;
import com.chat.base.utils.AndroidUtilities;
import com.chat.uikit.R;
import com.chat.uikit.chat.face.Drop;
import com.chat.uikit.chat.face.DropAdapter;
import com.chat.uikit.chat.face.FunctionAdapter;
import com.chat.uikit.chat.face.FunctionViewPageAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 2019-11-13 10:58
 * 面板管理
 */
public class FaceManger {

    private FaceManger() {
    }

    private static class FaceMangerBinder {
        final static FaceManger faceManger = new FaceManger();
    }

    public static FaceManger getInstance() {
        return FaceMangerBinder.faceManger;
    }

    /**
     * 功能面板（键盘高度，用于表情/更多等弹层）
     */
    public View getFunctionView(IConversationContext iConversationContext, IFuncListener iFuncListener) {
        int h = WKConstants.getKeyboardHeight();
        if (h == 0) {
            h = AndroidUtilities.dp(280);
        }
        int showH = h - AndroidUtilities.dp(23);
        return buildFunctionView(iConversationContext, iFuncListener, showH);
    }

    /**
     * 输入栏下方常驻功能菜单（固定网格，图标+文字，不可滚动）
     */
    public View getInlineFunctionView(IConversationContext iConversationContext, IFuncListener iFuncListener) {
        List<ChatFunctionMenu> list = EndpointManager.getInstance()
                .invokes(EndpointCategory.chatFunction, iConversationContext);
        int itemCount = list == null ? 0 : list.size();
        if (itemCount == 0) {
            View empty = new View(iConversationContext.getChatActivity());
            empty.setVisibility(View.GONE);
            return empty;
        }
        return buildInlineFunctionView(iConversationContext, iFuncListener, list);
    }

    private View buildInlineFunctionView(
            IConversationContext iConversationContext,
            IFuncListener iFuncListener,
            List<ChatFunctionMenu> list
    ) {
        View view = LayoutInflater.from(iConversationContext.getChatActivity())
                .inflate(R.layout.panel_function_inline_layout, null);
        RecyclerView recyclerView = view.findViewById(R.id.functionRecyclerView);
        int itemHeight = AndroidUtilities.dp(84);
        int rows = (list.size() + 3) / 4;
        recyclerView.getLayoutParams().height = itemHeight * rows;
        recyclerView.setLayoutManager(new GridLayoutManager(
                iConversationContext.getChatActivity(), 4));
        recyclerView.setNestedScrollingEnabled(false);
        FunctionAdapter adapter = new FunctionAdapter(list, itemHeight, true);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((adapter1, view1, position) -> SingleClickUtil.determineTriggerSingleClick(view1, v -> {
            ChatFunctionMenu menu = (ChatFunctionMenu) adapter1.getItem(position);
            if (menu != null && iFuncListener != null) {
                iFuncListener.onFuncLick(menu);
            }
        }));
        return view;
    }

    private View buildFunctionView(
            IConversationContext iConversationContext,
            IFuncListener iFuncListener,
            int showH
    ) {
        View view = LayoutInflater.from(iConversationContext.getChatActivity())
                .inflate(R.layout.panel_function_layout, null);
        ViewPager2 viewPager2 = view.findViewById(R.id.viewPage);
        RecyclerView dropRecyclerView = view.findViewById(R.id.dropRecyclerView);

        List<ChatFunctionMenu> list = EndpointManager.getInstance()
                .invokes(EndpointCategory.chatFunction, iConversationContext);
        List<List<ChatFunctionMenu>> allList = new ArrayList<>();
        List<ChatFunctionMenu> tempList = new ArrayList<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            if (tempList.size() < 8) {
                tempList.add(list.get(i));
                if (i == list.size() - 1) {
                    allList.add(tempList);
                    tempList = new ArrayList<>();
                }
            } else {
                allList.add(tempList);
                tempList = new ArrayList<>();
            }
        }

        dropRecyclerView.getLayoutParams().width = AndroidUtilities.dp(allList.size() * 10);
        FunctionViewPageAdapter adapter = new FunctionViewPageAdapter(
                iConversationContext.getChatActivity(), allList, showH, iFuncListener);
        viewPager2.setAdapter(adapter);
        DropAdapter dropAdapter = new DropAdapter();
        dropRecyclerView.setLayoutManager(new LinearLayoutManager(
                iConversationContext.getChatActivity(), LinearLayoutManager.HORIZONTAL, false));
        dropRecyclerView.setAdapter(dropAdapter);
        List<Drop> dropList = new ArrayList<>();
        for (int i = 0; i < allList.size(); i++) {
            dropList.add(new Drop(i == 0));
        }
        dropAdapter.setList(dropList);
        dropRecyclerView.setVisibility(allList.size() > 1 ? View.VISIBLE : View.GONE);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                boolean isRefresh = true;
                for (int i = 0, size = dropAdapter.getData().size(); i < size; i++) {
                    if (dropAdapter.getData().get(i).isSelect()) {
                        if (position != i) {
                            dropAdapter.getData().get(i).setSelect(false);
                            dropAdapter.notifyItemChanged(i);
                            break;
                        } else {
                            isRefresh = false;
                        }
                    }
                }
                if (isRefresh) {
                    dropAdapter.getData().get(position).setSelect(true);
                    dropAdapter.notifyItemChanged(position);
                }
            }
        });
        return view;
    }

    public interface IFuncListener {
        void onFuncLick(ChatFunctionMenu functionMenu);
    }

}
