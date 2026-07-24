package com.chat.scan;

import android.widget.TextView;

import com.chat.base.base.BageBaseActivity;
import com.chat.scan.databinding.ActScanOtherResultLayoutBinding;

/**
 * 2020-04-19 18:25
 * 扫描其他内容
 */
public class BageScanOtherResultActivity extends BageBaseActivity<ActScanOtherResultLayoutBinding> {
    @Override
    protected ActScanOtherResultLayoutBinding getViewBinding() {
        return ActScanOtherResultLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.bage_scan_module_other_result);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        String result = getIntent().getStringExtra("result");
        bageVBinding.resultTv.setText(result);
    }

    @Override
    protected void initListener() {

    }
}
