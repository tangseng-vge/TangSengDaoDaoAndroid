package com.bage.im.manager;

import android.text.TextUtils;

import com.bage.im.db.ChannelDBManager;
import com.bage.im.db.BageDBColumns;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelSearchResult;
import com.bage.im.interfaces.IChannelInfoListener;
import com.bage.im.interfaces.IGetChannelInfo;
import com.bage.im.interfaces.IRefreshChannel;
import com.bage.im.interfaces.IRefreshChannelAvatar;
import com.bage.im.utils.BageCommonUtils;
import com.bage.im.utils.BageLoggerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 5/20/21 5:49 PM
 * channel管理
 */
public class ChannelManager extends BaseManager {
    private final String TAG = "ChannelManager";

    private ChannelManager() {
    }

    private static class ChannelManagerBinder {
        static final ChannelManager channelManager = new ChannelManager();
    }

    public static ChannelManager getInstance() {
        return ChannelManagerBinder.channelManager;
    }

    private IRefreshChannelAvatar iRefreshChannelAvatar;
    private IGetChannelInfo iGetChannelInfo;
    private final CopyOnWriteArrayList<BageChannel> bageChannelList = new CopyOnWriteArrayList<>();
    //监听刷新频道
    private ConcurrentHashMap<String, IRefreshChannel> refreshChannelMap;

    public synchronized BageChannel getChannel(String channelID, byte channelType) {
        if (TextUtils.isEmpty(channelID)) return null;
        BageChannel bageChannel = null;
        for (BageChannel channel : bageChannelList) {
            if (channel != null && channel.channelID.equals(channelID) && channel.channelType == channelType) {
                bageChannel = channel;
                break;
            }
        }
        if (bageChannel == null) {
            bageChannel = ChannelDBManager.getInstance().query(channelID, channelType);
            if (bageChannel != null) {
                bageChannelList.add(bageChannel);
            }
        }
        return bageChannel;
    }

    // 从网络获取channel
    public void fetchChannelInfo(String channelID, byte channelType) {
        if (TextUtils.isEmpty(channelID)) return;
        BageChannel channel = getChannel(channelID, channelType, bageChannel -> {
            if (bageChannel != null)
                saveOrUpdateChannel(bageChannel);
        });
        if (channel != null) {
            saveOrUpdateChannel(channel);
        }
    }

    public BageChannel getChannel(String channelId, byte channelType, IChannelInfoListener iChannelInfoListener) {
        if (this.iGetChannelInfo != null && !TextUtils.isEmpty(channelId) && iChannelInfoListener != null) {
            return iGetChannelInfo.onGetChannelInfo(channelId, channelType, iChannelInfoListener);
        } else return null;
    }

    public void addOnGetChannelInfoListener(IGetChannelInfo iGetChannelInfoListener) {
        this.iGetChannelInfo = iGetChannelInfoListener;
    }

    public void saveOrUpdateChannel(BageChannel channel) {
        if (channel == null) return;
        //先更改内存数据
        updateChannel(channel);
        setRefreshChannel(channel, true);
        ChannelDBManager.getInstance().insertOrUpdate(channel);
    }


    /**
     * 修改频道信息
     *
     * @param channel 频道
     */
    private void updateChannel(BageChannel channel) {
        if (channel == null) return;
        boolean isAdd = true;
        for (int i = 0, size = bageChannelList.size(); i < size; i++) {
            if (bageChannelList.get(i).channelID.equals(channel.channelID) && bageChannelList.get(i).channelType == channel.channelType) {
                isAdd = false;
                bageChannelList.get(i).forbidden = channel.forbidden;
                bageChannelList.get(i).channelName = channel.channelName;
                bageChannelList.get(i).avatar = channel.avatar;
                bageChannelList.get(i).category = channel.category;
                bageChannelList.get(i).lastOffline = channel.lastOffline;
                bageChannelList.get(i).online = channel.online;
                bageChannelList.get(i).follow = channel.follow;
                bageChannelList.get(i).top = channel.top;
                bageChannelList.get(i).channelRemark = channel.channelRemark;
                bageChannelList.get(i).status = channel.status;
                bageChannelList.get(i).version = channel.version;
                bageChannelList.get(i).invite = channel.invite;
                bageChannelList.get(i).localExtra = channel.localExtra;
                bageChannelList.get(i).mute = channel.mute;
                bageChannelList.get(i).save = channel.save;
                bageChannelList.get(i).showNick = channel.showNick;
                bageChannelList.get(i).isDeleted = channel.isDeleted;
                bageChannelList.get(i).receipt = channel.receipt;
                bageChannelList.get(i).robot = channel.robot;
                bageChannelList.get(i).flameSecond = channel.flameSecond;
                bageChannelList.get(i).flame = channel.flame;
                bageChannelList.get(i).deviceFlag = channel.deviceFlag;
                bageChannelList.get(i).parentChannelID = channel.parentChannelID;
                bageChannelList.get(i).parentChannelType = channel.parentChannelType;
                bageChannelList.get(i).avatarCacheKey = channel.avatarCacheKey;
                bageChannelList.get(i).remoteExtraMap = channel.remoteExtraMap;
                break;
            }
        }
        if (isAdd) {
            bageChannelList.add(channel);
        }
    }

    private void updateChannel(String channelID, byte channelType, String key, Object value) {
        if (TextUtils.isEmpty(channelID) || TextUtils.isEmpty(key)) return;
        for (int i = 0, size = bageChannelList.size(); i < size; i++) {
            if (bageChannelList.get(i).channelID.equals(channelID) && bageChannelList.get(i).channelType == channelType) {
                switch (key) {
                    case BageDBColumns.BageChannelColumns.avatar_cache_key:
                        bageChannelList.get(i).avatarCacheKey = (String) value;
                        break;
                    case BageDBColumns.BageChannelColumns.remote_extra:
                        bageChannelList.get(i).remoteExtraMap = (HashMap<String, Object>) value;
                        break;
                    case BageDBColumns.BageChannelColumns.avatar:
                        bageChannelList.get(i).avatar = (String) value;
                        break;
                    case BageDBColumns.BageChannelColumns.channel_remark:
                        bageChannelList.get(i).channelRemark = (String) value;
                        break;
                    case BageDBColumns.BageChannelColumns.channel_name:
                        bageChannelList.get(i).channelName = (String) value;
                        break;
                    case BageDBColumns.BageChannelColumns.follow:
                        bageChannelList.get(i).follow = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.forbidden:
                        bageChannelList.get(i).forbidden = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.invite:
                        bageChannelList.get(i).invite = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.is_deleted:
                        bageChannelList.get(i).isDeleted = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.last_offline:
                        bageChannelList.get(i).lastOffline = (long) value;
                        break;
                    case BageDBColumns.BageChannelColumns.mute:
                        bageChannelList.get(i).mute = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.top:
                        bageChannelList.get(i).top = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.online:
                        bageChannelList.get(i).online = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.receipt:
                        bageChannelList.get(i).receipt = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.save:
                        bageChannelList.get(i).save = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.show_nick:
                        bageChannelList.get(i).showNick = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.status:
                        bageChannelList.get(i).status = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.username:
                        bageChannelList.get(i).username = (String) value;
                        break;
                    case BageDBColumns.BageChannelColumns.flame:
                        bageChannelList.get(i).flame = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.flame_second:
                        bageChannelList.get(i).flameSecond = (int) value;
                        break;
                    case BageDBColumns.BageChannelColumns.localExtra:
                        bageChannelList.get(i).localExtra = (HashMap<String, Object>) value;
                        break;
                }
                setRefreshChannel(bageChannelList.get(i), true);
                break;
            }
        }
    }

    /**
     * 添加或修改频道信息
     *
     * @param list 频道数据
     */
    public void saveOrUpdateChannels(List<BageChannel> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        // 先修改内存数据
        for (int i = 0, size = list.size(); i < size; i++) {
            updateChannel(list.get(i));
            setRefreshChannel(list.get(i), i == list.size() - 1);
        }
        ChannelDBManager.getInstance().insertChannels(list);
    }

    /**
     * 修改频道状态
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param status      状态
     */
    public void updateStatus(String channelID, byte channelType, int status) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.status, status);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.status, String.valueOf(status));
    }


    /**
     * 修改频道名称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param name        名称
     */
    public void updateName(String channelID, byte channelType, String name) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.channel_name, name);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.channel_name, name);
    }

    /**
     * 获取频道信息
     *
     * @param channelType 频道类型
     * @param status      状态
     * @return List<BageChannel>
     */
    public List<BageChannel> getWithStatus(byte channelType, int status) {
        return ChannelDBManager.getInstance().queryWithStatus(channelType, status);
    }

    public List<BageChannel> getWithChannelIdsAndChannelType(List<String> channelIds, byte channelType) {
        return ChannelDBManager.getInstance().queryWithChannelIdsAndChannelType(channelIds, channelType);
    }

    public List<BageChannel> getChannels(List<String> channelIds) {
        return ChannelDBManager.getInstance().queryWithChannelIds(channelIds);
    }

    /**
     * 搜索频道
     *
     * @param keyword 关键字
     * @return List<BageChannelSearchResult>
     */
    public List<BageChannelSearchResult> search(String keyword) {
        return ChannelDBManager.getInstance().search(keyword);
    }

    /**
     * 搜索频道
     *
     * @param keyword     关键字
     * @param channelType 频道类型
     * @return List<BageChannel>
     */
    public List<BageChannel> searchWithChannelType(String keyword, byte channelType) {
        return ChannelDBManager.getInstance().searchWithChannelType(keyword, channelType);
    }

    public List<BageChannel> searchWithChannelTypeAndFollow(String keyword, byte channelType, int follow) {
        return ChannelDBManager.getInstance().searchWithChannelTypeAndFollow(keyword, channelType, follow);
    }

    /**
     * 获取频道信息
     *
     * @param channelType 频道类型
     * @param follow      关注状态
     * @return List<BageChannel>
     */
    public List<BageChannel> getWithChannelTypeAndFollow(byte channelType, int follow) {
        return ChannelDBManager.getInstance().queryWithChannelTypeAndFollow(channelType, follow);
    }

    /**
     * 修改某个频道免打扰
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param isMute      1：免打扰
     */
    public void updateMute(String channelID, byte channelType, int isMute) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.mute, isMute);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.mute, String.valueOf(isMute));
    }

    /**
     * 修改备注信息
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param hashExtra   扩展字段
     */
    public void updateLocalExtra(String channelID, byte channelType, HashMap<String, Object> hashExtra) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.localExtra, hashExtra);
        if (hashExtra != null) {
            JSONObject jsonObject = new JSONObject();
            for (String key : hashExtra.keySet()) {
                try {
                    jsonObject.put(key, hashExtra.get(key));
                } catch (JSONException e) {
                    BageLoggerUtils.getInstance().e(TAG, "updateLocalExtra error");
                }
            }
            ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.localExtra, jsonObject.toString());
        }
    }

    /**
     * 修改频道是否保存在通讯录
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param isSave      1:保存
     */
    public void updateSave(String channelID, byte channelType, int isSave) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.save, isSave);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.save, String.valueOf(isSave));
    }

    /**
     * 是否显示频道昵称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param showNick    1：显示频道昵称
     */
    public void updateShowNick(String channelID, byte channelType, int showNick) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.show_nick, showNick);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.show_nick, String.valueOf(showNick));
    }

    /**
     * 修改某个频道是否置顶
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param top         1：置顶
     */
    public void updateTop(String channelID, byte channelType, int top) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.top, top);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.top, String.valueOf(top));
    }

    /**
     * 修改某个频道的备注
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param remark      备注
     */
    public void updateRemark(String channelID, byte channelType, String remark) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.channel_remark, remark);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.channel_remark, remark);
    }

    /**
     * 修改关注状态
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param follow      是否关注
     */
    public void updateFollow(String channelID, byte channelType, int follow) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.follow, follow);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.follow, String.valueOf(follow));
    }

    /**
     * 通过follow和status查询频道
     *
     * @param channelType 频道类型
     * @param follow      是否关注 好友或陌生人
     * @param status      状态 正常或黑名单
     * @return list
     */
    public List<BageChannel> getWithFollowAndStatus(byte channelType, int follow, int status) {
        return ChannelDBManager.getInstance().queryWithFollowAndStatus(channelType, follow, status);
    }

    public void updateAvatarCacheKey(String channelID, byte channelType, String avatar) {
        updateChannel(channelID, channelType, BageDBColumns.BageChannelColumns.avatar_cache_key, avatar);
        ChannelDBManager.getInstance().updateWithField(channelID, channelType, BageDBColumns.BageChannelColumns.avatar_cache_key, avatar);
    }

    public void addOnRefreshChannelAvatar(IRefreshChannelAvatar iRefreshChannelAvatar) {
        this.iRefreshChannelAvatar = iRefreshChannelAvatar;
    }

    public void setOnRefreshChannelAvatar(String channelID, byte channelType) {
        if (iRefreshChannelAvatar != null) {
            runOnMainThread(() -> iRefreshChannelAvatar.onRefreshChannelAvatar(channelID, channelType));
        }
    }

    public synchronized void clearARMCache() {
        bageChannelList.clear();
    }

    // 刷新频道
    public void setRefreshChannel(BageChannel channel, boolean isEnd) {
        if (refreshChannelMap != null) {
            runOnMainThread(() -> {
                updateChannel(channel);
                for (Map.Entry<String, IRefreshChannel> entry : refreshChannelMap.entrySet()) {
                    entry.getValue().onRefreshChannel(channel, isEnd);
                }
            });
        }
    }

    // 监听刷新普通
    public void addOnRefreshChannelInfo(String key, IRefreshChannel iRefreshChannelListener) {
        if (TextUtils.isEmpty(key)) return;
        if (refreshChannelMap == null) refreshChannelMap = new ConcurrentHashMap<>();
        if (iRefreshChannelListener != null)
            refreshChannelMap.put(key, iRefreshChannelListener);
    }

    // 移除频道刷新监听
    public void removeRefreshChannelInfo(String key) {
        if (TextUtils.isEmpty(key) || refreshChannelMap == null) return;
        refreshChannelMap.remove(key);
    }

}
