package com.fzm.chat.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.fzm.chat.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhengjy
 * @since 2018/12/28
 * Description:
 */
public class StringUtils {
    /**
     * 判断是不是一个1开头11位的手机号码
     *
     * @param mobiles 手机号
     */
    public static boolean isMobileNO(String mobiles) {
        Pattern p = Pattern.compile("^((1[0-9][0-9]))\\d{8}$");
        Matcher m = p.matcher(mobiles);
        return m.matches();
    }

    public static String formatMutedTime(long millisUntilFinished) {
        StringBuilder builder = new StringBuilder();
        String str = "";
        long hour = millisUntilFinished / (3600 * 1000);
        String hourStr = "";
        if (hour == 0) {
            hourStr = "00";
        } else if (hour > 0 && hour < 10) {
            hourStr = "0" + hour;
        } else {
            hourStr = "" + hour;
        }

        long min = (millisUntilFinished / (60 * 1000)) % 60;
        String minStr = "";
        if (min == 0) {
            minStr = "00";
        } else if (min > 0 && min < 10) {
            minStr = "0" + min;
        } else {
            minStr = "" + min;
        }

        long mil = (millisUntilFinished % (60 * 1000)) / 1000;
        String milStr = "";
        if (mil == 0) {
            milStr = "00";
        } else if (mil > 0 && mil < 10) {
            milStr = "0" + mil;
        } else {
            milStr = "" + mil;
        }
        builder.append(hourStr);
        builder.append(":");
        builder.append(minStr);
        builder.append(":");
        builder.append(milStr);
        str = builder.toString();
        return str;
    }

    @SuppressLint("SimpleDateFormat")
    public static String timeFormat(Context context, long time) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);
        Calendar current = Calendar.getInstance();
        int delta;
        if ((current.get(Calendar.YEAR) - date.get(Calendar.YEAR)) > 0) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(time);
        } else if ((delta = (current.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR))) > 0) {
            if (delta == 1) {
                return context.getString(R.string.chat_time_tips_yesterday, new SimpleDateFormat("HH:mm").format(time));
            }
            if (current.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)) {
                int day = date.get(Calendar.DAY_OF_WEEK);
                int id = context.getResources().getIdentifier("chat_time_tips_week" + day, "string", context.getPackageName());
                return context.getString(id, new SimpleDateFormat("HH:mm").format(time));
            } else {
                return new SimpleDateFormat("MM-dd HH:mm").format(time);
            }
        } else if ((current.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR))) {
            return new SimpleDateFormat("HH:mm").format(time);
        } else {
            return new SimpleDateFormat("MM-dd HH:mm").format(time);
        }
    }

    public static String getDisplayAddress(String address) {
        if (address.length() <= 8) {
            return address;
        }
        String start = address.substring(0, 4);
        String end = address.substring(address.length() - 4);
        return start + "****" + end;
    }

    private static boolean checkPasswordLength(String password){
        if (password.length() > 16 || password.length() < 8) {
            return false;
        }
        return true;
    }

    public static boolean isEncryptPassword(String password) {
        if(!checkPasswordLength(password)) {
            return false;
        }
        Pattern p = Pattern.compile("^(?![0-9]+$)(?![a-zA-Z]+$)(?![^A-Za-z0-9]+$)([\\w^\\\\|<>\\[\\]{}#%+-=~/:;()$&\"`?!*@,.']){8,16}$");

        Matcher m = p.matcher(password);

        return m.matches();
    }
}
