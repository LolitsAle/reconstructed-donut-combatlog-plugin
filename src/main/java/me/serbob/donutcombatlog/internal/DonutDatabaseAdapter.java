package me.serbob.donutcombatlog.internal;

import java.lang.reflect.Method;

/**
 * Reflection adapter so this repo can compile without the original
 * DonutDatabase plugin on the compile classpath.
 */
public final class DonutDatabaseAdapter {
    private DonutDatabaseAdapter() {}

    public static boolean isAvailable() {
        try {
            Class.forName("me.serbob.donutdatabase.DonutDatabase");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static double getBounty(String playerName) {
        try {
            Object database = getDatabase();
            Method getBounty = database.getClass().getMethod("getBounty", String.class);
            Object result = getBounty.invoke(database, playerName);
            return result instanceof Number number ? number.doubleValue() : 0.0D;
        } catch (Throwable throwable) {
            return 0.0D;
        }
    }

    public static void removeBounty(String playerName) {
        try {
            Object database = getDatabase();
            Method removeBounty = database.getClass().getMethod("removeBounty", String.class);
            removeBounty.invoke(database, playerName);
        } catch (Throwable ignored) {
        }
    }

    private static Object getDatabase() throws Exception {
        Class<?> donutDatabase = Class.forName("me.serbob.donutdatabase.DonutDatabase");
        Method getDatabase = donutDatabase.getMethod("getDatabase");
        return getDatabase.invoke(null);
    }
}
