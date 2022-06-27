-keep class com.tencent.wcdb.** {*;}

-keep class * extends com.google.protobuf.GeneratedMessageV3 {
    public static ** parseFrom(com.google.protobuf.ByteString);
}