package me.elian.playtime.object;

import me.elian.playtime.PlaytimePro;

import java.util.concurrent.TimeUnit;

public class OnlineTime {

    private long joinTime;
    private long tempStoredTime = -1; // This is the time stored when the database hasn't updated
    private volatile int allTime = 0,
                monthlyTime = 0,
                weeklyTime = 0,
                seasonTime = 0;

    public OnlineTime() {
        joinTime = System.currentTimeMillis();
    }

    public OnlineTime(final long time) {
        joinTime = time;
    }

    public synchronized void handleLogout() {
        long joinTimeCopy = joinTime;
        joinTime = -1;
        tempStoredTime += (System.currentTimeMillis() - joinTimeCopy);
    }

    public long getOnlinePlaytime() {
        long tempTime = 0;

        if (joinTime != -1)
            tempTime += (System.currentTimeMillis() - joinTime);

        if (tempStoredTime != -1)
            tempTime += tempStoredTime;

        return tempTime;
    }

    public synchronized void login() {
        this.joinTime = System.currentTimeMillis();
    }

    // Returns cached playtime in seconds and resets the cache
    public synchronized int returnAndReset(long currentTime) {
        long tempTime = 0;

        if (joinTime != -1)
            tempTime += (currentTime - joinTime);

        if (tempStoredTime != -1)
            tempTime += tempStoredTime;

        int secondsTime = (int) TimeUnit.MILLISECONDS.toSeconds(tempTime);

        // Catch the time jumps
        if (TimeUnit.SECONDS.toHours(secondsTime) < 1000) {
            addToCachedTime(secondsTime, secondsTime, secondsTime, secondsTime);
        }
        else {
            PlaytimePro.getInstance().getLogger().severe("Time jump occurred for a player! Was about to add " + secondsTime + "!");
        }

        // Reset time
        joinTime = currentTime;
        tempStoredTime = -1;

        return secondsTime;
    }

    // Instead of setters, these add to the time when the playtime is fetched to avoid dealing with concurrency issues
    // Since the value of the read doesn't actually matter, the operations do not need to be atomically in-order

    public void addToCachedTime(int allTime, int monthlyTime, int weeklyTime, int seasonTime) {
        synchronized (this) {
            this.allTime += allTime;
            this.monthlyTime += monthlyTime;
            this.weeklyTime += weeklyTime;
            this.seasonTime += seasonTime;
        }
    }

    public int getAllTime() {
        return allTime;
    }

    public int getMonthlyTime() {
        return monthlyTime;
    }

    public int getWeeklyTime() {
        return weeklyTime;
    }

    public int getSeasonTime() {
        return seasonTime;
    }
}
