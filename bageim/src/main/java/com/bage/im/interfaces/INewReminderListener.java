package com.bage.im.interfaces;

import com.bage.im.entity.BageReminder;

import java.util.List;

public interface INewReminderListener {
    void newReminder(List<BageReminder> list);
}
