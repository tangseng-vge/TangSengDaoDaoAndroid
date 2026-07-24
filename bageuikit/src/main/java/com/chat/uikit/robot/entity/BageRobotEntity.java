package com.chat.uikit.robot.entity;

public class BageRobotEntity {
    public String robot_id;
    public String username;
    public String inline_on;
    public String placeholder;
    public long version;
    public long token;
    public String created_at;
    public String updated_at;

    public BageRobotEntity() {

    }

    public BageRobotEntity(String robot_id, long version) {
        this.robot_id = robot_id;
        this.version = version;
    }

    public BageRobotEntity(String robot_id, String username, long version) {
        this.robot_id = robot_id;
        this.username = username;
        this.version = version;
    }
}
