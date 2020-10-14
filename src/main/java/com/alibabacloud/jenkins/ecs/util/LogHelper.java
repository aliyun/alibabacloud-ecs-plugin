package com.alibabacloud.jenkins.ecs.util;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import hudson.model.TaskListener;
import org.slf4j.Logger;

/**
 * Created by kunlun.ykl on 2020/9/25.
 */
public class LogHelper {
    private static final SimpleFormatter sf = new SimpleFormatter();

    public static void debug(Logger logger, TaskListener listener, String message, Throwable e) {
        logger.debug(message, e);
        remoteLog(logger.getName(), Level.FINEST, listener, message, e);
    }

    public static void info(Logger logger, TaskListener listener, String message, Throwable e) {
        logger.info(message, e);
        remoteLog(logger.getName(), Level.INFO, listener, message, e);
    }

    public static void warn(Logger logger, TaskListener listener, String message, Throwable e) {
        logger.warn(message, e);
        remoteLog(logger.getName(), Level.WARNING, listener, message, e);
    }

    public static void error(Logger logger, TaskListener listener, String message, Throwable e) {
        logger.error(message, e);
        remoteLog(logger.getName(), Level.SEVERE, listener, message, e);
    }

    public static void remoteLog(String loggerName, Level level, TaskListener listener, String message, Throwable e) {
        if (listener != null) {
            if (e != null) {
                message += " Exception: " + e;
            }
            LogRecord lr = new LogRecord(level, message);
            lr.setLoggerName(loggerName);
            PrintStream printStream = listener.getLogger();
            printStream.print(sf.format(lr));
        }
    }
}
