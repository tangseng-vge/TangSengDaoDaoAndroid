package com.bage.im.entity;

import org.json.JSONObject;

/**
 * 2/3/21 2:21 PM
 * CMD
 */
public class BageCMD {
    // 命令类型
    public String cmdKey;
    // 命令参数
    public JSONObject paramJsonObject;

    public BageCMD(String cmdKey, JSONObject paramJsonObject) {
        this.cmdKey = cmdKey;
        this.paramJsonObject = paramJsonObject;
    }
}
