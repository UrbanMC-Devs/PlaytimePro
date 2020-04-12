package me.elian.playtime.object;

import java.util.concurrent.TimeUnit;

public class OnlineTime {

    private long joinTime;
    private long tempStoredTime = -1; // This is the time stored when the database hasn't updated
    private volatile int allTime = 0,
                monthlyTime = 0,
                weeklyTime = 0;

    public OnlineTime() {
        joinTime = System.currentTimeMillis();
    }

    public OnlineTime(final long time) {
        joinTime = time;
    }

    public void handleLogout() {
        tempStoredTime += (System.currentTimeMillis() - joinTime);
    }

    public long getOnlinePlaytime() {
        long tempTime = 0;

        if (joinTime != -1)
            tempTime += (System.currentTimeMillis() - joinTime);

        if (tempStoredTime != -1)
            tempTime += tempStoredTime;

        return tempTime;
    }

    public void login() {
        this.joinTime = System.currentTimeMillis();
    }

    // Returns cached playtime in seconds and resets the cache
    public int returnAndReset(long currentTime) {
        long tempTime = 0;

        if (joinTime != -1)
            tempTime += (currentTime - joinTime);

        if (tempStoredTime != -1)
            tempTime += tempStoredTime;

        int secondsTime = (int) TimeUnit.MILLISECONDS.toSeconds(tempTime);

        updateLocalTime(secondsTime);

        // Reset time
        joinTime = currentTime;
        tempStoredTime = -1;

        return secondsTime;
    }

    private void updateLocalTime(int addedSeconds) {
        allTime += addedSeconds;
        monthlyTime += addedSeconds;
        weeklyTime += addedSeconds;
    }

    // Instead of setters, these add to the time when the playtime is fetched to avoid dealing with concurrency issues
    // Since the value of the read doesn't actually matter, the operations do not need to be atomically in-order

    public int getAllTime() {
        return allTime;
    }

    public void addToAllTime(int seconds) {
        allTime += seconds;
    }

    public int getMonthlyTime() {
        return monthlyTime;
    }

    public void addToMonthlyTime(int seconds) {
        this.monthlyTime += seconds;
    }

    public int getWeeklyTime() {
        return weeklyTime;
    }

    public void addToWeeklyTime(int seconds) {
        this.weeklyTime += seconds;
    }



}
