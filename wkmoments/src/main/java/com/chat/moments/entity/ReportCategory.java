package com.chat.moments.entity;

import java.util.List;

/** 举报分类。字段名与服务端 JSON 保持一致。 */
public class ReportCategory {
    public String category_no;
    public String category_name;
    public String parent_category_no;
    public List<ReportCategory> children;
}
