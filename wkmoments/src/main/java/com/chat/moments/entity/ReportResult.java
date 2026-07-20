package com.chat.moments.entity;

/** 举报动态或评论后的服务端处理结果。 */
public class ReportResult {
    public int status;
    public String msg;
    public String target_type;
    public String moment_no;
    public String comment_id;
    public boolean remove_content;

    public boolean shouldRemoveContent() {
        return remove_content;
    }
}
