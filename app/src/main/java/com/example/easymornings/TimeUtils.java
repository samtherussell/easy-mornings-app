package com.example.easymornings;

import java.util.ArrayList;
import java.util.Calendar;

public class TimeUtils {
    static int getSecond(long time) {
        return (int) (time % 60);
    }

    static int getMinute(long time) {
        return (int) ((time / 60) % 60);
    }

    static int getHour(long time) {
        return (int) (time / (60 * 60));
    }

    static int getDelay(int minute, int second) {
        return (minute * 60 + second);
    }

    static int getTimestamp(int hour, int minute) {
        return (hour * 60 + minute) * 60;
    }

    public static int getNowTimestamp() {
        Calendar calendar = Calendar.getInstance();
        return getTimestamp(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    static int getSecondsUntil(long epocMilli) {
        return (int) ((epocMilli + 500 - System.currentTimeMillis()) / 1000);
    }

    static String getAbsoluteTimeString(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    static String getDelayString(int minute, int second) {
        return String.format("%02dm%02ds", minute, second);
    }

    static String getTimeIntervalString(int seconds) {
        int days = seconds / (60*60*24);
        seconds -= days * (60*60*24);
        int hours = seconds / (60*60);
        seconds -= hours * (60*60);
        int minutes = seconds / 60;
        seconds -= minutes * 60;
        ArrayList<String> strings = new ArrayList<>();
        if (days > 0)
            strings.add(String.format("%s days", days));
        if (hours > 0)
            strings.add(String.format("%s hours", hours));
        if (days == 0)
            strings.add(String.format("%s min", minutes));
        if (days == 0 && hours == 0)
            strings.add(String.format("%s sec", seconds));
        return String.join(", ", strings);
    }

    static String getDelayTimeString(int seconds) {
        if (seconds == 0)
            return "instantly";
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0 && seconds > 0)
            return String.format("%d:%02d", minutes, seconds);
        else if (minutes > 0)
            return String.format("%d min", minutes);
        else
            return String.format("%d sec", seconds);
    }

    static String getTimeLeftString(int seconds) {
        if (seconds < 0)
            return "#ERROR";
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 1) {
            if (seconds > 45)
                minutes += 1;
            return String.format("%d min", minutes);
        } else if (minutes > 0 || seconds > 45)
            return "a min";
        else if (seconds > 25)
            return "30 sec";
        else if (seconds > 10)
            return "15 sec";
        else
            return "few sec";
    }

    static String getDateTimeString(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return String.format("%d-%02d-%02d %02d:%02d:%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH)+1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
}
