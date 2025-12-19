package com.chat.base.base;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chat.base.R;
import com.chat.base.utils.WKDialogUtils;

/**
 * 2019-05-02 16:50
 * fragment基类
 */
public abstract class WKBaseFragment<WKVBinding extends ViewBinding> extends Fragment {

    protected View mContentView;
    protected WKVBinding wkVBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if (getArguments() != null) {
            getDataBundle(getArguments());
        }


        if (mContentView == null) {
            //设置布局
            wkVBinding = getViewBinding();
            mContentView = wkVBinding.getRoot();

            initPresenter();
            initView();
            initListener();
            initData();
            initTitleBar();
        } else {
            ViewGroup parent = (ViewGroup) mContentView.getParent();
            if (parent != null) {
                parent.removeView(mContentView);
            }
        }
        applyInsets(mContentView);
        return mContentView;
    }

    private void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {

            Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());

            Insets ime =
                    insets.getInsets(WindowInsetsCompat.Type.ime());

            v.setPadding(
                    systemBars.left,
                    0,
                    systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom)
            );
            return insets;
        });
    }

    protected abstract WKVBinding getViewBinding();

    /**
     * 初始化化View
     */
    protected void initView() {
    }

    //初始化present
    protected void initPresenter() {
    }


    /**
     * 初始化事件
     */
    protected void initListener() {
    }


    /**
     * 初始化数据
     */
    protected void initData() {
    }


    //设置标题
    protected void setTitle(TextView titleTv) {
    }


    //获取fragment传递的参数
    protected void getDataBundle(Bundle bundle) {
    }

    //获取标题栏右上角文本
    protected String getRightTvText() {
        return "";
    }

    //获取标题栏右上角icon id 默认-1不显示
    protected int getRightIvResourceId(ImageView imageView) {
        return -1;
    }

    //是否显示标题栏底部view
    protected boolean isShowTitleBottomView() {
        return true;
    }

    //是否显示返回
    protected boolean isShowBackLayout() {
        return true;
    }

    //获取右控件
    protected void getRightView(ImageView rightIv) {

    }

    //标题栏右上角事件
    protected void rightLayoutClick() {
    }


    //标题栏事件（含view返回）
    protected void rightLayoutClick(View view) {

    }

    //标题栏左上角事件
    protected void leftLayoutClick() {
    }

    //是否显示系统状态栏
    protected boolean isHiddenSystemTitleBar() {
        return false;
    }

    //初始化标题栏
    private void initTitleBar() {
        if (mContentView == null) {
            return;
        }
        
        // 动态设置statusBarView的高度为实际状态栏高度
        View statusBarView = mContentView.findViewById(R.id.statusBarView);
        if (statusBarView != null && getContext() != null) {
            ViewGroup.LayoutParams params = statusBarView.getLayoutParams();
            params.height = com.chat.base.utils.systembar.WKStatusBarUtils.getStatusBarHeight(getContext());
            statusBarView.setLayoutParams(params);
        }
        
        View titleBar = mContentView.findViewById(R.id.titleBarLayout);
        if (titleBar == null) return;
        //设置标题
        TextView titleCenterTv = mContentView.findViewById(R.id.titleCenterTv);
        setTitle(titleCenterTv);
        View titleBottomLinView = mContentView.findViewById(R.id.titleBottomLinView);
        if (isShowTitleBottomView())
            titleBottomLinView.setVisibility(View.VISIBLE);
        else titleBottomLinView.setVisibility(View.GONE);
        final View titleRightLayout = mContentView.findViewById(R.id.titleRightLayout);
        ImageView rightIv = mContentView.findViewById(R.id.titleRightIv);
        if (getRightIvResourceId(rightIv) != -1) {
            rightIv.setImageResource(getRightIvResourceId(rightIv));
            rightIv.setVisibility(View.VISIBLE);
            titleRightLayout.setVisibility(View.VISIBLE);
            getRightView(rightIv);
        }

        TextView rightTv = mContentView.findViewById(R.id.titleRightTv);
        if (!TextUtils.isEmpty(getRightTvText())) {
            rightTv.setText(getRightTvText());
            titleRightLayout.setVisibility(View.VISIBLE);
        }

        titleRightLayout.setOnClickListener(view -> {
            rightLayoutClick();
            rightLayoutClick(view);
        });
    }


    /**
     * 初始化默认适配器（垂直列表）
     *
     * @param recyclerView 列表
     * @param adapter      adapter
     */
    protected void initAdapter(RecyclerView recyclerView, BaseQuickAdapter<?, ?> adapter) {
        if (recyclerView == null || adapter == null) return;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        adapter.setAnimationFirstOnly(true);
    }
}
