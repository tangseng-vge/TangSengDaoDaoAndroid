package com.chat.moments.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chat.base.config.BageConfig;
import com.chat.base.entity.BottomSheetItem;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageDialogUtils;
import com.chat.moments.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

//仿微信朋友圈列表页下拉刷新控件
public class FriendRefreshView extends ViewGroup implements OnDetectScrollListener {
    private ImageView backIv;
    private ImageView cameraIv;
    private ImageView msgIv;
    private View titleView;
    private boolean titleShowCamera;
    private boolean titleShowMsg;
    private TextView titleTv;
    private TextView updateBgTv;
    //圆形指示器
    private ImageView mRainbowView;
    private ImageView momentBgIv;
    private View momentTopLayout;
    private View momentCenterView;
    private RecyclerView mContentView;
    private boolean momentCoverMetricsReady;
    private int momentCoverBaseHeight;
    private int momentCenterBaseTopMargin;

    //控件宽，高
    private int sWidth;
    private int sHeight;

    private ViewDragHelper mDragHelper;

    //contentView的当前top属性
    private int currentTop;
    //listView首个item
    private int firstItem;
    private boolean bScrollDown = false;

    private boolean bDraging = false;

    //圆形加载指示器最大top
    private final int rainbowMaxTop = 180;
    //圆形加载指示器刷新时的top
    private final int rainbowStickyTop = 180;
    //圆形加载指示器初始top
    private final int rainbowStartTop = -120;
    //圆形加载指示器的半径
    private final int rainbowRadius = 100;
    private int rainbowTop = -120;
    private int rainbowLeft;
    private int rainbowSize;
    //圆形加载指示器旋转的角度
    private int rainbowRotateAngle = 0;
    /** 刷新时在屏幕中央播放旋转动画 */
    private boolean centerRainbowMode;

    private boolean bViewHelperSettling = false;

    //刷新接口listener
    private OnRefreshListener mRefreshLisenter;
    int sh;
    private AbsListView.OnScrollListener onScrollListener;
    private final OnDetectScrollListener onDetectScrollListener;
    private View mHeadViw;

    public enum State {
        NORMAL,
        REFRESHING,
        DRAGING
    }

    //控件当前状态
    private State mState = State.NORMAL;

    public FriendRefreshView(Context context) {
        this(context, null);
    }

    public FriendRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FriendRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        sh = AndroidUtilities.getScreenHeight() / 3;
        initHandler();
        initDragHelper();
        initListView();
        initRainbowView();
        setBackgroundColor(Color.TRANSPARENT);
        onDetectScrollListener = this;
    }

    public RecyclerView getmContentView() {
        return mContentView;
    }

    public View getmHeadViw() {
        return mHeadViw;
    }

    /**
     * 初始化handler，当ViewDragHelper释放了mContentView时，
     * 我们通过循环发送消息刷新mRainbowView的位置和角度
     */
    @SuppressLint("HandlerLeak")
    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(@NotNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        if (centerRainbowMode) {
                            centerRainbowMode = false;
                            mRainbowView.setVisibility(GONE);
                            mRainbowView.setAlpha(1f);
                            rainbowTop = rainbowStartTop;
                            break;
                        }
                        if (rainbowTop > rainbowStartTop) {
                            rainbowTop -= 10;
                            requestLayout();
                            mHandler.sendEmptyMessageDelayed(0, 15);
                        } else {
                            mRainbowView.setVisibility(GONE);
                        }
                        break;
                    case 1:
                        if (centerRainbowMode) {
                            if (isRefreshing()) {
                                rainbowRotateAngle -= 10;
                                mRainbowView.setRotation(rainbowRotateAngle);
                                requestLayout();
                                mHandler.sendEmptyMessageDelayed(1, 15);
                            }
                            break;
                        }
                        if (rainbowTop <= rainbowStickyTop) {
                            if (rainbowTop < rainbowStickyTop) {
                                rainbowTop += 10;
                                if (rainbowTop > rainbowStickyTop) {
                                    rainbowTop = rainbowStickyTop;
                                }
                            }
                            mRainbowView.setRotation(rainbowRotateAngle -= 10);
                        } else {
                            mRainbowView.setRotation(rainbowRotateAngle += 10);
                        }
                        if (mRainbowView.getVisibility() != VISIBLE) {
                            mRainbowView.setVisibility(VISIBLE);
                        }
                        requestLayout();
                        mHandler.sendEmptyMessageDelayed(1, 15);
                        break;
                }
            }
        };
    }

    /**
     * 初始化mDragHelper，我们处理拖动的核心类
     */
    private void initDragHelper() {
        mDragHelper = ViewDragHelper.create(this, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(@NotNull View view, int i) {
                return view == mContentView && !bViewHelperSettling;
            }

            @Override
            public int clampViewPositionHorizontal(@NotNull View child, int left, int dx) {
                return 0;
            }

            @Override
            public int clampViewPositionVertical(@NotNull View child, int top, int dy) {
                //限制最大下拉屏幕的三分之一高度

                if (top > sh) top = sh;
                return top;
            }

            @Override
            public void onViewPositionChanged(@NotNull View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                if (changedView == mContentView) {
                    int lastContentTop = currentTop;
                    if (top >= 0) {
                        currentTop = top;
                    } else {
                        top = 0;
                        currentTop = 0;
                    }
                    updateMomentCoverForPull(currentTop);
                    if (centerRainbowMode && isRefreshing()) {
                        requestLayout();
                        return;
                    }
                    int lastTop = rainbowTop;
                    int rTop = top + rainbowStartTop;
                    if (rTop >= rainbowMaxTop) {
                        if (!isRefreshing()) {
                            rainbowRotateAngle += (currentTop - lastContentTop) * 2;
                            rTop = rainbowMaxTop;
                            rainbowTop = rTop;
                            mRainbowView.setRotation(rainbowRotateAngle);
                        } else {
                            rTop = rainbowMaxTop;
                            rainbowTop = rTop;
                        }

                    } else {
                        if (isRefreshing()) {
                            rainbowTop = rainbowStickyTop;
                        } else {
                            rainbowTop = rTop;
                            rainbowRotateAngle += (rainbowTop - lastTop) * 3;
                            mRainbowView.setRotation(rainbowRotateAngle);
                        }
                    }

                    requestLayout();

                }
            }

            @Override
            public void onViewReleased(@NotNull View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                mDragHelper.settleCapturedViewAt(0, 0);
                ViewCompat.postInvalidateOnAnimation(FriendRefreshView.this);
                //如果手势释放时，拖动的距离大于rainbowStickyTop，开始刷新
                if (currentTop >= rainbowStickyTop) {
                    startRefresh();
                }

            }
        });


    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
            bViewHelperSettling = true;
        } else {
            bViewHelperSettling = false;
        }
    }

    /**
     * 我们invoke 方法shouldIntercept来判断是否需要拦截事件，
     * 拦截事件是为了将事件传递给mDragHelper来处理，我们这里只有当mContentView滑动到顶部
     * 且mContentView没有处于滑动状态时才触发拦截。
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mDragHelper.shouldInterceptTouchEvent(ev);
        return shouldIntercept();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        final int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                mLastMotionY = 0;
                bDraging = false;
                bScrollDown = false;
                rainbowRotateAngle = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                int index = event.getActionIndex();// MotionEventCompat.getActionIndex(event);
                int pointerId = event.getPointerId(index);// MotionEventCompat.getPointerId(event, index);
                if (shouldIntercept()) {
                    mDragHelper.captureChildView(mContentView, pointerId);
                }
                break;
        }
        return true;
    }

    /**
     * 判断是否需要拦截触摸事件
     *
     * @return boolean
     */
    private boolean shouldIntercept() {
        if (bDraging) return true;
        int childCount = mContentView.getChildCount();
        if (childCount > 0) {
            View firstChild = mContentView.getChildAt(0);
            return firstChild.getTop() >= 0
                    && firstItem == 0 && currentTop == 0
                    && bScrollDown;
        } else {
            return true;
        }
    }

    /**
     * 判断mContentView是否处于顶部
     *
     * @return boolean
     */
    private boolean checkIsTop() {
        int childCount = mContentView.getChildCount();
        if (childCount > 0) {
            View firstChild = mContentView.getChildAt(0);
            return firstChild.getTop() >= 0
                    && firstItem == 0 && currentTop == 0;
        } else {
            return false;
        }
    }

    private void initRainbowView() {
        mRainbowView = new ImageView(getContext());
        mRainbowView.setImageResource(R.mipmap.rainbow_ic);
        rainbowSize = AndroidUtilities.dp(30);
        mRainbowView.setVisibility(GONE);
        addView(mRainbowView);
    }

    private void beginCenterRainbowAnimation() {
        centerRainbowMode = true;
        mRainbowView.setVisibility(VISIBLE);
        mRainbowView.setAlpha(1f);
        rainbowRotateAngle = 0;
        mRainbowView.setRotation(0);
        if (sWidth > 0) {
            rainbowLeft = (sWidth - rainbowSize) / 2;
            rainbowTop = computeRainbowTopBelowTitleBar();
        }
        requestLayout();
    }

    /** 刷新动画显示在 act_moments_layout 的 titleView 下方居中 */
    private int computeRainbowTopBelowTitleBar() {
        if (titleView == null) {
            return Math.max(0, (sHeight - rainbowSize) / 2);
        }
        int[] titleLoc = new int[2];
        int[] selfLoc = new int[2];
        titleView.getLocationOnScreen(titleLoc);
        getLocationOnScreen(selfLoc);
        int top = titleLoc[1] + titleView.getHeight() - selfLoc[1] + AndroidUtilities.dp(28);
        return Math.max(AndroidUtilities.dp(8), top);
    }

    /**
     * 初始化listView，我们创建了istView for you，所有你要做的
     * 就是调用setAdapter，绑定你自定义的adapter
     */
    int mDistanceY = 0;

    private void initListView() {
        mContentView = new FriendRefreshListView(getContext());
        mHeadViw = LayoutInflater.from(getContext()).inflate(R.layout.list_head_layout, null);
        updateBgTv = mHeadViw.findViewById(R.id.updateBgTv);
        momentBgIv = mHeadViw.findViewById(R.id.momentBgIv);
        momentTopLayout = mHeadViw.findViewById(R.id.topLayout);
        momentCenterView = mHeadViw.findViewById(R.id.centerView);
        momentTopLayout.setOnClickListener(view -> {
            //查看自己的朋友圈才能修改封面
            if (TextUtils.isEmpty(uid) || uid.equals(BageConfig.getInstance().getUid())) {
                List<BottomSheetItem> list = new ArrayList<>();
                list.add(new BottomSheetItem(getContext().getString(R.string.change_conver), R.mipmap.qr_gallery, () -> iConverClick.onChangeBg()));
                BageDialogUtils.getInstance().showBottomSheet(getContext(), getContext().getString(R.string.moment_cover), false, list);
            }
        });
        this.addView(mContentView);
        mContentView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NotNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null || titleView == null) {
                    return;
                }
                int scrollOffset = 0;
                if (layoutManager.findFirstVisibleItemPosition() == 0
                        && recyclerView.getChildCount() > 0
                        && recyclerView.getChildAt(0) != null) {
                    scrollOffset = -recyclerView.getChildAt(0).getTop();
                }
                mDistanceY = scrollOffset;
            }
        });
    }

    /** 下拉时保持列表顶部不露底色，并按下拉距离拉伸朋友圈封面。 */
    private void updateMomentCoverForPull(int pullDistance) {
        if (momentBgIv == null || momentTopLayout == null
                || momentCenterView == null || mContentView == null) {
            return;
        }
        if (!momentCoverMetricsReady) {
            ViewGroup.LayoutParams coverParams = momentTopLayout.getLayoutParams();
            momentCoverBaseHeight = momentTopLayout.getHeight() > 0
                    ? momentTopLayout.getHeight() : coverParams.height;
            ViewGroup.LayoutParams centerParams = momentCenterView.getLayoutParams();
            if (centerParams instanceof ViewGroup.MarginLayoutParams) {
                momentCenterBaseTopMargin =
                        ((ViewGroup.MarginLayoutParams) centerParams).topMargin;
            }
            if (momentCoverBaseHeight <= 0) {
                return;
            }
            momentCoverMetricsReady = true;
        }

        int stretch = Math.max(0, pullDistance);
        int coverHeight = momentCoverBaseHeight + stretch;
        updateViewHeight(momentTopLayout, coverHeight);
        updateViewHeight(momentBgIv, coverHeight);

        ViewGroup.LayoutParams centerParams = momentCenterView.getLayoutParams();
        if (centerParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams =
                    (ViewGroup.MarginLayoutParams) centerParams;
            marginParams.topMargin = momentCenterBaseTopMargin + stretch;
            momentCenterView.setLayoutParams(marginParams);
        }

        // ViewDragHelper 仍使用真实位移判断刷新距离，视觉上由封面拉伸替代灰色空隙。
        mContentView.setTranslationY(-stretch);
    }

    private void updateViewHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params.height == height) {
            return;
        }
        params.height = height;
        view.setLayoutParams(params);
    }

    public void setTitleActionVisibility(boolean showCamera, boolean showMsg) {
        titleShowCamera = showCamera;
        titleShowMsg = showMsg;
        applyTitleActionVisibility();
    }

    private void applyTitleActionVisibility() {
        if (cameraIv != null) {
            cameraIv.setVisibility(titleShowCamera ? VISIBLE : GONE);
            if (titleShowCamera) {
                cameraIv.setImageResource(R.mipmap.floating_camera);
                cameraIv.clearColorFilter();
            }
        }
        if (msgIv != null) {
            msgIv.setVisibility(titleShowMsg ? VISIBLE : GONE);
            if (titleShowMsg) {
                msgIv.setImageResource(R.mipmap.floating_message);
                msgIv.clearColorFilter();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        sWidth = MeasureSpec.getSize(widthMeasureSpec);
        sHeight = MeasureSpec.getSize(heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        LayoutParams contentParams = (LayoutParams) mContentView.getLayoutParams();
        contentParams.left = 0;
        contentParams.top = 0;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        LayoutParams contentParams = (LayoutParams) mContentView.getLayoutParams();
        mContentView.layout(contentParams.left, currentTop,
                contentParams.left + sWidth, currentTop + sHeight);

        if (mRainbowView.getVisibility() != VISIBLE) {
            mRainbowView.layout(0, rainbowStartTop, rainbowSize, rainbowStartTop + rainbowSize);
            return;
        }
        if (centerRainbowMode) {
            rainbowLeft = (sWidth - rainbowSize) / 2;
            rainbowTop = computeRainbowTopBelowTitleBar();
        }
        int left = centerRainbowMode ? rainbowLeft : rainbowRadius;
        int top = rainbowTop;
        mRainbowView.layout(left, top, left + rainbowSize, top + rainbowSize);
    }

    @Override
    public void onUpScrolling() {
        bScrollDown = false;
    }

    @Override
    public void onDownScrolling() {
        bScrollDown = true;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        int left = 0;
        int top = 0;

        LayoutParams(Context arg0, AttributeSet arg1) {
            super(arg0, arg1);
        }

        LayoutParams(int arg0, int arg1) {
            super(arg0, arg1);
        }

        LayoutParams(android.view.ViewGroup.LayoutParams arg0) {
            super(arg0);
        }

    }

    @Override
    public android.view.ViewGroup.LayoutParams generateLayoutParams(
            AttributeSet attrs) {
        return new FriendRefreshView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected android.view.ViewGroup.LayoutParams generateLayoutParams(
            android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof FriendRefreshView.LayoutParams;
    }

    private float mLastMotionX;
    private float mLastMotionY;

    /**
     * 对ListView的触摸事件进行判断，是否处于滑动状态
     */
    private class FriendRefreshListView extends RecyclerView {


        public FriendRefreshListView(Context context) {
            this(context, null);
        }

        public FriendRefreshListView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public FriendRefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            setBackgroundColor(Color.TRANSPARENT);
        }

        /*当前活动的点Id,有效的点的Id*/
        protected int mActivePointerId = INVALID_POINTER;

        /*无效的点*/
        private static final int INVALID_POINTER = -1;

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            final int action = ev.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    int index = ev.getActionIndex();//MotionEventCompat.getActionIndex(ev);
                    mActivePointerId = ev.getPointerId(index);// MotionEventCompat.getPointerId(ev, index);
                    if (mActivePointerId == INVALID_POINTER)
                        break;
                    mLastMotionX = ev.getX();
                    mLastMotionY = ev.getY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    int indexMove = ev.getActionIndex();//MotionEventCompat.getActionIndex(ev);
                    mActivePointerId = ev.getPointerId(indexMove);// MotionEventCompat.getPointerId(ev, indexMove);
                    if (mActivePointerId == INVALID_POINTER) {

                    } else {
                        final float y = ev.getY();
                        float dy = y - mLastMotionY;
                        if (checkIsTop() && dy >= 1.0f) {
                            bScrollDown = true;
                            bDraging = true;
                        } else {
                            bScrollDown = false;
                            bDraging = false;
                        }
                        mLastMotionX = y;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mLastMotionY = 0;
                    break;
            }
            return super.onTouchEvent(ev);
        }
    }

    public boolean isRefreshing() {
        return mState == State.REFRESHING;
    }


    Handler mHandler;

    public void startRefresh() {
        if (!isRefreshing()) {
            mHandler.removeMessages(0);
            mHandler.removeMessages(1);
            mState = State.REFRESHING;
            if (currentTop < AndroidUtilities.dp(60)) {
                beginCenterRainbowAnimation();
            } else {
                centerRainbowMode = false;
                mRainbowView.setVisibility(VISIBLE);
            }
            mHandler.sendEmptyMessage(1);
            invokeListener();
        }

    }

    private void invokeListener() {
        if (mRefreshLisenter != null) {
            mRefreshLisenter.onRefresh();
        }
    }

    public void stopRefresh() {
        mHandler.removeMessages(1);
        mHandler.sendEmptyMessage(0);
        mState = State.NORMAL;
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mRefreshLisenter = listener;
    }

    public void setTitleViews(TextView titleTv, ImageView backIv, ImageView cameraIv, ImageView msgIv, View titleView) {
        this.titleView = titleView;
        this.backIv = backIv;
        this.cameraIv = cameraIv;
        this.msgIv = msgIv;
        this.titleTv = titleTv;
        if (titleView != null) {
            titleView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private String uid;

    public void setUid(String uid) {
//        if (TextUtils.isEmpty(uid)) uid = BageConfig.getInstance().getUid();
        this.uid = uid;
        if (!TextUtils.isEmpty(uid) && !uid.equals(BageConfig.getInstance().getUid())) {
            updateBgTv.setVisibility(GONE);
        }
    }

    public ImageView getMomentBgIv() {
        return momentBgIv;
    }

    public void hideOrShowUpdateBgTv(boolean isShow) {
        updateBgTv.setVisibility(isShow ? VISIBLE : GONE);
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    private IConverClick iConverClick;

    public void setOnConverClick(IConverClick iConverClick) {
        this.iConverClick = iConverClick;
    }

    public interface IConverClick {
        void onChangeBg();
    }
}
