package com.bage.im.entity;

public class BageSendOptions {
    public int expire = 0;
    public String topicID;
    public int flame;
    public int flameSecond;
    public String robotID;
    public BageMsgSetting setting = new BageMsgSetting();
    public BageMsgHeader header = new BageMsgHeader();
}
