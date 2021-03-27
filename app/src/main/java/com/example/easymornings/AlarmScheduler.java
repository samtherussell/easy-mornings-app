package com.example.easymornings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.easymornings.alarmreceiver.FadeOnReceiver;
import com.example.easymornings.alarmreceiver.TurnOffReceiver;
import com.example.easymornings.db.Alarm;

import java.util.Calendar;
import java.util.Optional;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlarmScheduler {

    public static final String UID_EXTRA = "uid";
    public static final String COMMAND_EXTRA = "command";
    public static final int SOUND_START_COMMAND = 2;
    public static final int SLEEP_SOUND_COMMAND = 4;
    public static final int ALARM_SLEEP_DELAY = 5 * 60;

    final AlarmManager alarmManager;
    final Context context;

    public static AlarmScheduler create(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return new AlarmScheduler(alarmManager, context);
    }

    static int getFadeOnRequestCode(int uid) {
        return uid * 10;
    }

    static int getTurnOffRequestCode(int uid) {
        return uid * 10 + 1;
    }

    static int getSleepRequestCode(int uid) {
        return uid * 10 + 2;
    }

    static int getAlarmSoundRequestCode(int uid) {
        return uid * 10 + 3;
    }

    void cancelAlarms(Alarm alarm) {
        if (alarm.fadeOnDelay != null) {
            cancelAlarm(new Intent(context, FadeOnReceiver.class), getFadeOnRequestCode(alarm.uid));
        }
        if (alarm.offDelay != null) {
            cancelAlarm(new Intent(context, TurnOffReceiver.class), getTurnOffRequestCode(alarm.uid));
        }
        if (alarm.alarmTime != null) {
            cancelAlarm(new Intent(context, MainActivity.class), getAlarmSoundRequestCode(alarm.uid));
            cancelAlarm(new Intent(context, MainActivity.class), getSleepRequestCode(alarm.uid));
        }
    }

    void cancelAlarm(Intent intent, int requestCode) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null)
            alarmManager.cancel(pendingIntent);
    }

    public Optional<Integer> scheduleNextFadeIn(Alarm alarm) {
        Intent intent = new Intent(context, FadeOnReceiver.class);
        intent.putExtra(UID_EXTRA, alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getFadeOnRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarm.fadeOnDelay != null && alarm.alarmTime != null && alarm.enabled) {
            int onTime = alarm.alarmTime - alarm.fadeOnDelay;
            Optional<Long> nextAlarmMillis = getNextAlarmMillis(onTime, alarm::isDayEnabled);
            return nextAlarmMillis.map(x -> {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, x, pendingIntent);
                return TimeUtils.getSecondsUntil(x);
            });
        } else {
            alarmManager.cancel(pendingIntent);
            return Optional.empty();
        }
    }

    public Optional<Integer> scheduleNextAlarm(Alarm alarm) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(COMMAND_EXTRA, SOUND_START_COMMAND);
        intent.putExtra("uid", alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getAlarmSoundRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarm.enabled && alarm.alarmTime != null) {
            Optional<Long> nextAlarmMillis = getNextAlarmMillis(alarm.alarmTime, alarm::isDayEnabled);
            return nextAlarmMillis.map(x -> {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, x, pendingIntent);
                return TimeUtils.getSecondsUntil(x);
            });
        } else {
            alarmManager.cancel(pendingIntent);
            return Optional.empty();
        }
    }

    public Optional<Integer> scheduleNextOff(Alarm alarm) {
        Intent intent = new Intent(context, TurnOffReceiver.class);
        intent.putExtra("uid", alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, getTurnOffRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarm.offDelay != null && alarm.alarmTime != null && alarm.enabled) {
            int offTime = alarm.alarmTime + alarm.offDelay;
            Optional<Long> nextAlarmMillis = getNextAlarmMillis(offTime, alarm::isDayEnabled);
            return nextAlarmMillis.map(x -> {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, x, pendingIntent);
                return TimeUtils.getSecondsUntil(x);
            });
        } else {
            alarmManager.cancel(pendingIntent);
            return Optional.empty();
        }
    }

    public Optional<Long> getNextAlarmMillis(int timestamp, Function<Integer, Boolean> isDayEnabled) {
        int hour = TimeUtils.getHour(timestamp);
        int minute = TimeUtils.getMinute(timestamp);
        int second = TimeUtils.getSecond(timestamp);
        Calendar now = Calendar.getInstance();
        if (
                now.get(Calendar.HOUR_OF_DAY) > hour ||
                        now.get(Calendar.HOUR_OF_DAY) == hour && now.get(Calendar.MINUTE) > minute ||
                        now.get(Calendar.HOUR_OF_DAY) == hour && now.get(Calendar.MINUTE) == minute && now.get(Calendar.SECOND) >= second
        ) {
            now.add(Calendar.DATE, 1);
        }
        int dayCount = 0;
        while (!isDayEnabled.apply(now.get(Calendar.DAY_OF_WEEK)) && dayCount < 7) {
            now.add(Calendar.DATE, 1);
            dayCount += 1;
        }
        if (dayCount >= 7) {
            return Optional.empty();
        }
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, second);
        return Optional.of(now.getTimeInMillis());
    }

    int scheduleSleepAlarm(Alarm alarm) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(COMMAND_EXTRA, SLEEP_SOUND_COMMAND);
        intent.putExtra("uid", alarm.uid);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getSleepRequestCode(alarm.uid), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtMillis = System.currentTimeMillis() + ALARM_SLEEP_DELAY * 1000;
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        return TimeUtils.getSecondsUntil(triggerAtMillis);
    }

    void cancelSleepAlarm(Alarm alarm) {
        cancelAlarm(new Intent(context, MainActivity.class), getSleepRequestCode(alarm.uid));
    }
}
