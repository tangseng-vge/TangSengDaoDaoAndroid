package com.bage.im.manager;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bage.im.BageIM;
import com.bage.im.db.ChannelMembersDbManager;
import com.bage.im.db.BageDBColumns;
import com.bage.im.entity.BageChannel;
import com.bage.im.entity.BageChannelExtras;
import com.bage.im.entity.BageChannelMember;
import com.bage.im.interfaces.IAddChannelMemberListener;
import com.bage.im.interfaces.IChannelMemberInfoListener;
import com.bage.im.interfaces.IGetChannelMemberInfo;
import com.bage.im.interfaces.IGetChannelMemberList;
import com.bage.im.interfaces.IGetChannelMemberListResult;
import com.bage.im.interfaces.IRefreshChannelMember;
import com.bage.im.interfaces.IRemoveChannelMember;
import com.bage.im.interfaces.ISyncChannelMembers;
import com.bage.im.utils.BageCommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 5/20/21 5:50 PM
 * channel members 管理
 */
public class ChannelMembersManager extends BaseManager {
    private ChannelMembersManager() {
    }

    private static class ChannelMembersManagerBinder {
        static final ChannelMembersManager channelMembersManager = new ChannelMembersManager();
    }

    public static ChannelMembersManager getInstance() {
        return ChannelMembersManagerBinder.channelMembersManager;
    }

    private ConcurrentHashMap<String, IRefreshChannelMember> refreshMemberMap;
    private ConcurrentHashMap<String, IRemoveChannelMember> removeChannelMemberMap;//监听添加频道成员
    private ConcurrentHashMap<String, IAddChannelMemberListener> addChannelMemberMap;
    private ISyncChannelMembers syncChannelMembers;
    //获取频道成员监听
    private IGetChannelMemberInfo iGetChannelMemberInfoListener;
    private IGetChannelMemberList iGetChannelMemberList;


    //最大版本成员
    @Deprecated
    public BageChannelMember getMaxVersionMember(String channelID, byte channelType) {
        return ChannelMembersDbManager.getInstance().queryMaxVersionMember(channelID, channelType);
    }

    public long getMaxVersion(String channelID, byte channelType) {
        return ChannelMembersDbManager.getInstance().queryMaxVersion(channelID, channelType);
    }

    public List<BageChannelMember> getRobotMembers(String channelID, byte channelType) {
        return ChannelMembersDbManager.getInstance().queryRobotMembers(channelID, channelType);
    }

    public List<BageChannelMember> getWithRole(String channelID, byte channelType, int role) {
        return ChannelMembersDbManager.getInstance().queryWithRole(channelID, channelType, role);
    }

    public synchronized void save(BageChannelMember member) {
        if (member == null) {
            return;
        }

        List<BageChannelMember> list = new ArrayList<>();
        list.add(member);
        int handelType = 0; // 修改
        BageChannelMember tempMember = ChannelMembersDbManager.getInstance().query(member.channelID, member.channelType, member.memberUID);
        if (tempMember == null) {
            handelType = 1;// 新增
        } else {
            if (member.isDeleted == 1 && tempMember.isDeleted == 0) {
                handelType = 2;// 删除
            }
            if (member.isDeleted == 0 && tempMember.isDeleted == 1) {
                handelType = 1;// 新增
            }
        }
        ChannelMembersDbManager.getInstance().insert(member);
        if (handelType == 0) {
            setRefreshChannelMember(member, true);
        }
        if (handelType == 1) {
            setOnAddChannelMember(list);
        }
        if (handelType == 2) {
            setOnRemoveChannelMember(list);
        }
    }

    /**
     * 批量保存成员
     *
     * @param list 成员数据
     */
    public synchronized void save(List<BageChannelMember> list) {
        if (BageCommonUtils.isEmpty(list)) return;
        new Thread(() -> {
            try {
            String channelID = list.get(0).channelID;
            byte channelType = list.get(0).channelType;

            List<BageChannelMember> addList = new ArrayList<>();
            List<BageChannelMember> deleteList = new ArrayList<>();
            List<BageChannelMember> updateList = new ArrayList<>();

            List<BageChannelMember> existList = new ArrayList<>();
            List<String> uidList = new ArrayList<>();
            for (BageChannelMember channelMember : list) {
                if (uidList.size() == 200) {
                    List<BageChannelMember> tempList = ChannelMembersDbManager.getInstance().queryWithUIDs(channelMember.channelID, channelMember.channelType, uidList);
                    if (BageCommonUtils.isNotEmpty(tempList))
                        existList.addAll(tempList);
                    uidList.clear();
                }
                uidList.add(channelMember.memberUID);
            }

            if (BageCommonUtils.isNotEmpty(uidList)) {
                List<BageChannelMember> tempList = ChannelMembersDbManager.getInstance().queryWithUIDs(channelID, channelType, uidList);
                if (BageCommonUtils.isNotEmpty(tempList))
                    existList.addAll(tempList);
                uidList.clear();
            }

            for (BageChannelMember channelMember : list) {
                boolean isNewMember = true;
                for (int i = 0, size = existList.size(); i < size; i++) {
                    if (channelMember.memberUID.equals(existList.get(i).memberUID)) {
                        isNewMember = false;
                        if (channelMember.isDeleted == 1) {
                            deleteList.add(channelMember);
                        } else {
                            if (existList.get(i).isDeleted == 1) {
                                isNewMember = true;
                            } else
                                updateList.add(channelMember);
                        }
                        break;
                    }
                }
                if (isNewMember) {
                    addList.add(channelMember);
                }
            }

            // 先保存或修改成员
            ChannelMembersDbManager.getInstance().insertMembers(list, existList);

            if (BageCommonUtils.isNotEmpty(addList)) {
                setOnAddChannelMember(addList);
            }
            if (BageCommonUtils.isNotEmpty(deleteList))
                setOnRemoveChannelMember(deleteList);

            if (BageCommonUtils.isNotEmpty(updateList)) {
                for (int i = 0, size = updateList.size(); i < size; i++) {
                    setRefreshChannelMember(updateList.get(i), i == updateList.size() - 1);
                }
            }
            } catch (Throwable t) {
                // Bugly#33246 防御：DB 关闭竞态导致的后台线程崩溃
                com.bage.im.utils.BageLoggerUtils.getInstance().e("ChannelMembersManager", "save aborted: " + t.getMessage());
            }
        }).start();

    }

    /**
     * 批量移除频道成员
     *
     * @param list 频道成员
     */
    public void delete(List<BageChannelMember> list) {
        runOnMainThread(() -> ChannelMembersDbManager.getInstance().deleteMembers(list));
    }

    /**
     * 通过状态查询频道成员
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param status      状态
     * @return List<>
     */
    public List<BageChannelMember> getWithStatus(String channelId, byte channelType, int status) {
        return ChannelMembersDbManager.getInstance().queryWithStatus(channelId, channelType, status);
    }

    /**
     * 修改频道成员备注
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param remarkName  备注
     */
    public boolean updateRemarkName(String channelID, byte channelType, String uid, String remarkName) {
        return ChannelMembersDbManager.getInstance().updateWithField(channelID, channelType, uid, BageDBColumns.BageChannelMembersColumns.member_remark, remarkName);
    }

    /**
     * 修改频道成员名称
     *
     * @param channelID   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param name        名称
     */
    public boolean updateMemberName(String channelID, byte channelType, String uid, String name) {
        return ChannelMembersDbManager.getInstance().updateWithField(channelID, channelType, uid, BageDBColumns.BageChannelMembersColumns.member_name, name);
    }

    /**
     * 修改频道成员状态
     *
     * @param channelId   频道ID
     * @param channelType 频道类型
     * @param uid         用户ID
     * @param status      状态
     */
    public boolean updateMemberStatus(String channelId, byte channelType, String uid, int status) {
        return ChannelMembersDbManager.getInstance().updateWithField(channelId, channelType, uid, BageDBColumns.BageChannelMembersColumns.status, String.valueOf(status));
    }

    public void addOnGetChannelMembersListener(IGetChannelMemberList iGetChannelMemberList) {
        this.iGetChannelMemberList = iGetChannelMemberList;
    }

    public void getWithPageOrSearch(String channelID, byte channelType, String searchKey, int page, int limit, @NonNull IGetChannelMemberListResult iGetChannelMemberListResult) {
        List<BageChannelMember> list;
        if (TextUtils.isEmpty(searchKey)) {
            list = getMembersWithPage(channelID, channelType, page, limit);
        } else {
            list = searchMembers(channelID, channelType, searchKey, page, limit);
        }

        iGetChannelMemberListResult.onResult(list, false);
        int groupType = 0;
        BageChannel channel = BageIM.getInstance().getChannelManager().getChannel(channelID, channelType);
        if (channel != null && channel.remoteExtraMap != null && channel.remoteExtraMap.containsKey(BageChannelExtras.groupType)) {
            Object groupTypeObject = channel.remoteExtraMap.get(BageChannelExtras.groupType);
            if (groupTypeObject instanceof Integer) {
                groupType = (int) groupTypeObject;
            }
        }
        if (iGetChannelMemberList != null && groupType == 1) {
            iGetChannelMemberList.request(channelID, channelType, searchKey, page, limit, list1 -> {
                iGetChannelMemberListResult.onResult(list1, true);
                if (BageCommonUtils.isNotEmpty(list1)) {
                  //  ChannelMembersDbManager.getInstance().deleteWithChannel(channelID, channelType);
                    save(list1);
                }
            });
        }
    }

    public void addOnGetChannelMemberListener(IGetChannelMemberInfo iGetChannelMemberInfoListener) {
        this.iGetChannelMemberInfoListener = iGetChannelMemberInfoListener;
    }

    public void refreshChannelMemberCache(BageChannelMember channelMember) {
        if (channelMember == null) return;
        List<BageChannelMember> list = new ArrayList<>();
        list.add(channelMember);
        ChannelMembersDbManager.getInstance().insertMembers(list);
    }

    /**
     * 添加加入频道成员监听
     *
     * @param listener 回调
     */
    public void addOnAddChannelMemberListener(String key, IAddChannelMemberListener listener) {
        if (TextUtils.isEmpty(key) || listener == null) return;
        if (addChannelMemberMap == null)
            addChannelMemberMap = new ConcurrentHashMap<>();
        addChannelMemberMap.put(key, listener);
    }

    public void removeAddChannelMemberListener(String key) {
        if (TextUtils.isEmpty(key) || addChannelMemberMap == null) return;
        addChannelMemberMap.remove(key);
    }

    public void setOnAddChannelMember(List<BageChannelMember> list) {
        if (addChannelMemberMap != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IAddChannelMemberListener> entry : addChannelMemberMap.entrySet()) {
                    entry.getValue().onAddMembers(list);
                }
            });
        }
    }

    /**
     * 获取频道成员信息
     *
     * @param channelId                  频道ID
     * @param uid                        成员ID
     * @param iChannelMemberInfoListener 回调
     */
    public BageChannelMember getMember(String channelId, byte channelType, String uid, IChannelMemberInfoListener iChannelMemberInfoListener) {
        if (iGetChannelMemberInfoListener != null && !TextUtils.isEmpty(channelId) && !TextUtils.isEmpty(uid) && iChannelMemberInfoListener != null) {
            return iGetChannelMemberInfoListener.onResult(channelId, channelType, uid, iChannelMemberInfoListener);
        } else return null;
    }

    public BageChannelMember getMember(String channelID, byte channelType, String uid) {
        return ChannelMembersDbManager.getInstance().query(channelID, channelType, uid);
    }

    public List<BageChannelMember> getMembers(String channelID, byte channelType) {
        return ChannelMembersDbManager.getInstance().query(channelID, channelType);
    }

    private List<BageChannelMember> searchMembers(String channelId, byte channelType, String keyword, int page, int size) {
        return ChannelMembersDbManager.getInstance().search(channelId, channelType, keyword, page, size);
    }

    private List<BageChannelMember> getMembersWithPage(String channelId, byte channelType, int page, int size) {
        return ChannelMembersDbManager.getInstance().queryWithPage(channelId, channelType, page, size);
    }

    public List<BageChannelMember> getDeletedMembers(String channelID, byte channelType) {
        return ChannelMembersDbManager.getInstance().queryDeleted(channelID, channelType);
    }

    //成员数量
    public int getMemberCount(String channelID, byte channelType) {
        return ChannelMembersDbManager.getInstance().queryCount(channelID, channelType);
    }

    public void addOnRefreshChannelMemberInfo(String key, IRefreshChannelMember iRefreshChannelMemberListener) {
        if (TextUtils.isEmpty(key) || iRefreshChannelMemberListener == null) return;
        if (refreshMemberMap == null)
            refreshMemberMap = new ConcurrentHashMap<>();
        refreshMemberMap.put(key, iRefreshChannelMemberListener);
    }

    public void removeRefreshChannelMemberInfo(String key) {
        if (TextUtils.isEmpty(key) || refreshMemberMap == null) return;
        refreshMemberMap.remove(key);
    }

    public void setRefreshChannelMember(BageChannelMember channelMember, boolean isEnd) {
        if (refreshMemberMap != null && channelMember != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRefreshChannelMember> entry : refreshMemberMap.entrySet()) {
                    entry.getValue().onRefresh(channelMember, isEnd);
                }
            });
        }
    }

    public void addOnRemoveChannelMemberListener(String key, IRemoveChannelMember listener) {
        if (listener == null || TextUtils.isEmpty(key)) return;
        if (removeChannelMemberMap == null) removeChannelMemberMap = new ConcurrentHashMap<>();
        removeChannelMemberMap.put(key, listener);
    }

    public void removeRemoveChannelMemberListener(String key) {
        if (TextUtils.isEmpty(key) || removeChannelMemberMap == null) return;
        removeChannelMemberMap.remove(key);
    }

    public void setOnRemoveChannelMember(List<BageChannelMember> list) {
        if (removeChannelMemberMap != null) {
            runOnMainThread(() -> {
                for (Map.Entry<String, IRemoveChannelMember> entry : removeChannelMemberMap.entrySet()) {
                    entry.getValue().onRemoveMembers(list);
                }
            });
        }
    }

    public void addOnSyncChannelMembers(ISyncChannelMembers syncChannelMembersListener) {
        this.syncChannelMembers = syncChannelMembersListener;
    }

    public void setOnSyncChannelMembers(String channelID, byte channelType) {
        if (syncChannelMembers != null) {
            runOnMainThread(() -> {
                syncChannelMembers.onSyncChannelMembers(channelID, channelType);
            });
        }
    }
}
