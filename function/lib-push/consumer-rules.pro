# 友盟推送相关
-dontwarn com.umeng.**
-dontwarn com.taobao.**
-dontwarn anet.channel.**
-dontwarn anetwork.channel.**
-dontwarn org.android.**
-dontwarn org.apache.thrift.**
-dontwarn com.xiaomi.**
-dontwarn com.huawei.**
-dontwarn com.meizu.**

-keepattributes *Annotation*

-keep class com.taobao.** {*;}
-keep class org.android.** {*;}
-keep class anet.channel.** {*;}
-keep class com.umeng.** {*;}
-keep class com.xiaomi.** {*;}
-keep class com.huawei.** {*;}
-keep class com.meizu.** {*;}
-keep class org.apache.thrift.** {*;}

-keep class com.alibaba.sdk.android.** {*;}
-keep class com.ut.** {*;}
-keep class com.uc.** {*;}
-keep class com.ta.** {*;}

-keep public class **.R$* {
    public static final int *;
}

# 小米
-keep class org.android.agoo.xiaomi.MiPushBroadcastReceiver {*;}
-dontwarn com.xiaomi.push.**

# 华为
-ignorewarnings
-keepattributes *Annotation*, Exceptions, InnerClasses, Signature, SourceFile, LineNumberTable
-keep class com.hianalytics.android.** {*;}
-keep class com.huawei.updatesdk.** {*;}
-keep class com.huawei.hms.** {*;}

# 魅族
-keep class com.meizu.cloud.** {*;}
-dontwarn com.meizu.cloud.**

# Oppo
-keep public class * extends android.app.Service

# Vivo
-dontwarn com.vivo.push.**
-keep class com.vivo.push.** {*;}
-keep class com.vivo.vms.** {*;}
