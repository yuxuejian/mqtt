package com.yxj.mqtt.utils;

import io.netty.channel.epoll.Native;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.internal.PlatformDependent;

import java.io.IOException;

public class Epoll {
    private static final Throwable EXCEPTION_CAUSE;

    static {
        Throwable cause = null;
        FileDescriptor epollFd = null;
        FileDescriptor eventFd = null;

        try {
            epollFd = Native.newEpollCreate();
            eventFd = Native.newEventFd();
        } catch (Exception e) {
            cause = e;
        } finally {
            if (epollFd != null) {
                try {
                    epollFd.close();
                } catch (IOException e) {
                    ;
                }
            }
            if (eventFd != null) {
                try {
                    eventFd.close();
                } catch (IOException e) {
                    ;
                }
            }
        }
        if (cause != null) {
            EXCEPTION_CAUSE = cause;
        } else {
            // 用来检测运行时系统的属性的工具类。如果classpath下sun.misc.Unsafe可用，那么返回True,用来提高访问direct内存的性能
            EXCEPTION_CAUSE = PlatformDependent.hasUnsafe() ? null : new IllegalStateException("sun.misc.Unsafe not available");
        }
    }

    public Epoll() {

    }

    public static boolean isAvailable() {
        return EXCEPTION_CAUSE == null;
    }
}
