package com.bage.im.manager;

import android.text.TextUtils;

import com.bage.im.db.RobotDBManager;
import com.bage.im.entity.BageRobot;
import com.bage.im.entity.BageRobotMenu;
import com.bage.im.interfaces.IRefreshRobotMenu;
import com.bage.im.utils.BageCommonUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RobotManager extends BaseManager {

    private RobotManager() {
    }

    private static class RobotManagerBinder {
        final static RobotManager manager = new RobotManager();
    }

    public static RobotManager getInstance() {
        return RobotManagerBinder.manager;
    }

    private ConcurrentHashMap<String, IRefreshRobotMenu> refreshRobotMenu;

    public BageRobot getWithRobotID(String robotID) {
        return RobotDBManager.getInstance().query(robotID);
    }

    public BageRobot getWithUsername(String username) {
        return RobotDBManager.getInstance().queryWithUsername(username);
    }

    public List<BageRobot> getWithRobotIds(List<String> robotIds) {
        return RobotDBManager.getInstance().queryRobots(robotIds);
    }

    public List<BageRobotMenu> getRobotMenus(String robotID) {
        return RobotDBManager.getInstance().queryRobotMenus(robotID);
    }

    public List<BageRobotMenu> getRobotMenus(List<String> robotIds) {
        return RobotDBManager.getInstance().queryRobotMenus(robotIds);
    }

    public void saveOrUpdateRobots(List<BageRobot> list) {
        if (BageCommonUtils.isNotEmpty(list)) {
            RobotDBManager.getInstance().insertOrUpdateRobots(list);
        }
    }

    public void saveOrUpdateRobotMenus(List<BageRobotMenu> list) {
        if (BageCommonUtils.isNotEmpty(list)) {
            RobotDBManager.getInstance().insertOrUpdateMenus(list);
        }
        setRefreshRobotMenu();
    }

    public void addOnRefreshRobotMenu(String key, IRefreshRobotMenu iRefreshRobotMenu) {
        if (TextUtils.isEmpty(key) || iRefreshRobotMenu == null) return;
        if (refreshRobotMenu == null) refreshRobotMenu = new ConcurrentHashMap<>();
        refreshRobotMenu.put(key, iRefreshRobotMenu);
    }

    public void removeRefreshRobotMenu(String key) {
        if (TextUtils.isEmpty(key) || refreshRobotMenu == null) return;
        refreshRobotMenu.remove(key);
    }

    private void setRefreshRobotMenu() {
        runOnMainThread(() -> {
            for (Map.Entry<String, IRefreshRobotMenu> entry : refreshRobotMenu.entrySet()) {
                entry.getValue().onRefreshRobotMenu();
            }
        });
    }
}
