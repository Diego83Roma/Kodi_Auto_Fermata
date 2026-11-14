-keepattributes LineNumberTable,SourceFile
-keepnames class me.aap.** { *; }
-keep class me.aap.fermata.auto.** { *; }
-keep class org.videolan.libvlc.** { *; }
-keep class me.aap.fermata.vfs.sftp.** { *; }
-keep class me.aap.fermata.vfs.smb.** { *; }
-keep class me.aap.fermata.vfs.gdrive.** { *; }
-keep class androidx.car.app.** { *; }
-keep class org.chromium.net.impl.NativeCronetEngineBuilderImpl { *; }

-dontwarn com.sun.jna.platform.win32.**
-dontwarn com.jcraft.jsch.PageantConnector
-dontwarn okio.*

-keepnames class androidx.media3.exoplayer.ExoPlayerImpl { *; }
-keepnames class androidx.media3.exoplayer.ExoPlayerImplInternal { *; }

# Kodi JNI creates this callback class dynamically through ClassLoader.loadClass/NewObject.
-keep class org.xbmc.kodi.interfaces.** { *; }

# Kodi loads this Java bridge by its fully-qualified name from native JNI.
-keep class org.xbmc.kodi.XBMCMediaSession { *; }
