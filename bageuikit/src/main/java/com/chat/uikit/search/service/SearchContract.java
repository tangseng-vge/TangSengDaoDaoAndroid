package com.chat.uikit.search.service;

import com.chat.base.base.BageBasePresenter;
import com.chat.base.base.BageBaseView;
import com.chat.uikit.search.SearchUserEntity;

/**
 * 2019-11-20 14:11
 * 搜索
 */
public class SearchContract {
    public interface SearchUserPresenter extends BageBasePresenter {
        void searchUser(String keyword);
    }

    public interface SearchUserView extends BageBaseView {
        void setSearchUser(SearchUserEntity searchUser);
    }
}
