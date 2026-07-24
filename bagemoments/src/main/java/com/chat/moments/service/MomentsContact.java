package com.chat.moments.service;

import com.chat.base.base.BageBasePresenter;
import com.chat.base.base.BageBaseView;
import com.chat.moments.entity.MomentSetting;
import com.chat.moments.entity.Moments;

import java.util.List;

/**
 * 2020-11-25 14:47
 */
public class MomentsContact {
    public interface MomentsPresent extends BageBasePresenter {
        void list(int page);

        void listByUid(int page, String uid);

        void momentSetting(String toUID);
    }

    public interface MomentsView extends BageBaseView {
        void setList(List<Moments> list);

        void setMomentSetting(MomentSetting momentSetting);
    }
}
