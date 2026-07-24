package com.bage.im.db;

import java.util.List;

/**
 * 2020-09-08 09:58
 * 升级管理
 */
public class BageDBSql {
    public long index;
    public List<String> sqlList;

    public BageDBSql(long index, List<String> sqlList) {
        this.index = index;
        this.sqlList = sqlList;
    }
}
