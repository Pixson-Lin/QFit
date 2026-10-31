# QFit release R8 rules (Compose + Room + Health Connect).

# Room entities / DB
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# Keep enum names used with valueOf / Room string columns (intensity, status).
-keepclassmembers enum com.pixsonlin.qfit.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Health Connect client (AAR may reference optional APIs).
-dontwarn androidx.health.connect.**
