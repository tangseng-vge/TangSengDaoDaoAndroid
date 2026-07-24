package com.chat.uikit.chat.search.date;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.BageLogUtils;
import com.chat.base.utils.BageReader;
import com.chat.base.utils.BageTimeUtils;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActSearchMsgWithDateLayoutBinding;
import com.bage.im.BageIM;
import com.bage.im.entity.BageChannelType;
import com.bage.im.entity.BageMessageGroupByDate;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 3/23/21 5:36 PM
 * 通过日期搜索聊天记录
 */
public class SearchWithDateActivity extends BageBaseActivity<ActSearchMsgWithDateLayoutBinding> {
    private String channelID;
    private byte channelType;
    private DateAdapter adapter;

    @Override
    protected ActSearchMsgWithDateLayoutBinding getViewBinding() {
        return ActSearchMsgWithDateLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.uikit_find_chat_content);
    }

    @Override
    protected void initPresenter() {
        channelID = getIntent().getStringExtra("channel_id");
        channelType = getIntent().getByteExtra("channel_type", BageChannelType.GROUP);
    }

    @Override
    protected void initView() {
        int wH = (AndroidUtilities.getScreenWidth() - AndroidUtilities.dp( 30)) / 7;
        adapter = new DateAdapter(wH, channelID, channelType);
        initAdapter(bageVBinding.recyclerView, adapter);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    @Override
    protected void initData() {
        super.initData();
        getData();
    }

    private List<SearchWithDateEntity> getResultWithDate() {
        List<SearchWithDateEntity> entityList = new ArrayList<>();
        List<BageMessageGroupByDate> list = BageIM.getInstance().getMsgManager().getMessageGroupByDateWithChannel(channelID, channelType);
        if (BageReader.isNotEmpty(list)) {
            String firstDate = list.get(0).date;
            int firstYear = Integer.parseInt(firstDate.split("-")[0]);
            int firstMonth = Integer.parseInt(firstDate.split("-")[1]);
            int nowYear = BageTimeUtils.getInstance().getNowYear();
            int nowMonth = BageTimeUtils.getInstance().getNowMonth();
            if (firstYear == nowYear) {
                // 同一年数据
                if (firstMonth == nowMonth) {
                    //同月
                    int maxDay = BageTimeUtils.getInstance().getNowDay(); //Calendar.getInstance(Locale.CHINA).getActualMaximum(Calendar.DATE);
                    String aMonth = String.valueOf(firstMonth);
                    if (firstMonth < 10) {
                        aMonth = "0" + firstMonth;
                    }
                    // 显示一个月的标题
                    SearchWithDateEntity monthEntity = new SearchWithDateEntity(1);
                    monthEntity.itemType = 1;
                    monthEntity.date = firstYear + "-" + aMonth;
                    entityList.add(monthEntity);

                    SearchWithDateEntity dayEntity = new SearchWithDateEntity(0);
                    List<SearchWithDateEntity> tempList = new ArrayList<>();
                    for (int i = 1; i <= maxDay; i++) {
                        // 获取周 todo
                        String day = String.valueOf(i);
                        if (i < 10) {
                            day = "0" + i;
                        }
                        String timeDate = firstYear + "-" + aMonth + "-" + day;
                        if (i == 1) {
                            int nullCount = getNullCount(timeDate);
                            if (nullCount > 0) {
                                for (int n = 0; n < nullCount; n++) {
                                    SearchWithDateEntity nullEntity = new SearchWithDateEntity(0);
                                    nullEntity.isNull = true;
                                    tempList.add(nullEntity);
                                }
                            }
                        }

                        SearchWithDateEntity entity = new SearchWithDateEntity(2);
                        entity.day = day;
                        if (i == maxDay) {
                            entity.isToDay = true;
                            entity.selected = true;
                        }
                        BageMessageGroupByDate groupByDate = getMsgCount(list, timeDate);
                        if (groupByDate != null) {
                            entity.dayCount = groupByDate.count;
                            entity.orderSeq = groupByDate.orderSeq;
                        }
                        tempList.add(entity);
                    }
                    dayEntity.list = tempList;
                    entityList.add(dayEntity);
                } else {
                    // 同年不同月
                    for (int i = firstMonth; i <= nowMonth; i++) {
                        // 显示一个月的标题
                        String aMonth = String.valueOf(i);
                        if (i < 10) {
                            aMonth = "0" + i;
                        }
                        SearchWithDateEntity monthEntity = new SearchWithDateEntity(1);
                        monthEntity.itemType = 1;
                        monthEntity.date = firstYear + "-" + aMonth;
                        entityList.add(monthEntity);
                        // 显示这个月的所有天

                        List<SearchWithDateEntity> dateEntityList = new ArrayList<>();
                        SearchWithDateEntity dayEntity = new SearchWithDateEntity(0);
                        try {
                            int maxDay;
                            if (i == nowMonth) {
                                maxDay = BageTimeUtils.getInstance().getNowDay();
                            } else
                                maxDay = getDaysOfMonth(sdf.parse(firstYear + "-" + aMonth));
                            for (int j = 1; j <= maxDay; j++) {
                                String day = String.valueOf(j);
                                if (j < 10) {
                                    day = "0" + j;
                                }
                                String timeDate = firstYear + "-" + aMonth + "-" + day;
                                if (j == 1) {
                                    int nullCount = getNullCount(timeDate);
                                    if (nullCount > 0) {
                                        for (int n = 0; n < nullCount; n++) {
                                            SearchWithDateEntity nullEntity = new SearchWithDateEntity(0);
                                            nullEntity.isNull = true;
                                            dateEntityList.add(nullEntity);
                                        }
                                    }
                                }
                                SearchWithDateEntity entity = new SearchWithDateEntity(2);
                                entity.day = day;
                                if (i == nowMonth && j == maxDay) {
                                    entity.isToDay = true;
                                    entity.selected = true;
                                }
                                BageMessageGroupByDate groupByDate = getMsgCount(list, timeDate);
                                if (groupByDate != null) {
                                    entity.dayCount = groupByDate.count;
                                    entity.orderSeq = groupByDate.orderSeq;
                                }
                                dateEntityList.add(entity);
                            }
                        } catch (ParseException e) {
                            BageLogUtils.e("按日期查询消息错误");
                        }
                        dayEntity.list = dateEntityList;
                        entityList.add(dayEntity);
                    }

                }
            } else {
                // 不同年
                for (int i = firstYear; i <= nowYear; i++) {
                    int maxMonth = 12;
                    int minMonth = 1;
                    if (i == nowYear) {
                        maxMonth = BageTimeUtils.getInstance().getNowMonth();
                    } else {
                        minMonth = firstMonth;
                    }
                    for (int j = minMonth; j <= maxMonth; j++) {
                        // 添加月份标题
                        String aMonth = String.valueOf(j);
                        if (j < 10) {
                            aMonth = "0" + j;
                        }
                        SearchWithDateEntity monthEntity = new SearchWithDateEntity(1);
                        monthEntity.itemType = 1;
                        monthEntity.date = i + "-" + aMonth;
                        entityList.add(monthEntity);

                        // 显示天
                        List<SearchWithDateEntity> dayList = new ArrayList<>();
                        SearchWithDateEntity dateEntity = new SearchWithDateEntity(0);
                        try {
                            int maxDay;
                            if (i == nowYear && j == maxMonth) {
                                maxDay = BageTimeUtils.getInstance().getNowDay();
                            } else {
                                maxDay = getDaysOfMonth(sdf.parse(i + "-" + aMonth));
                            }
                            for (int m = 1; m <= maxDay; m++) {
                                String day = String.valueOf(m);
                                if (m < 10) {
                                    day = "0" + m;
                                }
                                SearchWithDateEntity entity = new SearchWithDateEntity(2);
                                entity.day = day;
                                if (j == nowMonth && m == maxDay && i == nowYear) {
                                    entity.isToDay = true;
                                    entity.selected = true;
                                }
                                String timeDate = i + "-" + aMonth + "-" + day;
                                if (m == 1) {
                                    int nullCount = getNullCount(timeDate);
                                    if (nullCount > 0) {
                                        for (int n = 0; n < nullCount; n++) {
                                            SearchWithDateEntity nullEntity = new SearchWithDateEntity(0);
                                            nullEntity.isNull = true;
                                            dayList.add(nullEntity);
                                        }
                                    }
                                }
                                BageMessageGroupByDate groupByDate = getMsgCount(list, timeDate);
                                if (groupByDate != null) {
                                    entity.dayCount = groupByDate.count;
                                    entity.orderSeq = groupByDate.orderSeq;
                                }
                                dayList.add(entity);
                            }
                            dateEntity.list = dayList;
                        } catch (ParseException e) {
                            BageLogUtils.e("不同年查询消息错误");
                        }
                        entityList.add(dateEntity);
                    }
                }
            }

        }
        return entityList;
    }

    private void getData() {
        Observable.create((ObservableOnSubscribe<List<SearchWithDateEntity>>) e -> e.onNext(getResultWithDate())).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Observer<>() {
            @Override
            public void onSubscribe(@NotNull Disposable d) {

            }

            @Override
            public void onNext(@NotNull List<SearchWithDateEntity> list) {
                if (BageReader.isNotEmpty(list)) {
                    adapter.setList(list);
                    bageVBinding.nodataTv.setVisibility(View.GONE);
                } else {
                    bageVBinding.nodataTv.setVisibility(View.VISIBLE);
                }
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    bageVBinding.recyclerView.scrollToPosition(adapter.getData().size() - 1);
                    bageVBinding.recyclerView.setVisibility(View.VISIBLE);
                }, 100);

            }

            @Override
            public void onError(@NotNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

    }


    private BageMessageGroupByDate getMsgCount(List<BageMessageGroupByDate> list, String date) {
        BageMessageGroupByDate groupByDate = null;
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).date.equals(date)) {
                groupByDate = list.get(i);
                break;
            }
        }
        return groupByDate;
    }

    private int getDaysOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private int getNullCount(String date) {
        int week = BageTimeUtils.getInstance().getWeek(date);
        if (week == 7)
            return 0;
        return week;
    }

}
