package me.elian.playtime.object;

import me.elian.playtime.PlaytimePro;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class OnlineTime {

    // The accuracy of these do matter
    // because they correlate to info
    // persisted in the database.
    private final AtomicLong joinTime;
    private final AtomicLong tempStoredTime;

    // The accuracy of these does not matter
    // because they are only read locally
    // and not persisted in the database.
    private volatile int allTime = 0,
                monthlyTime = 0,
                weeklyTime = 0,
                seasonTime = 0;

    public OnlineTime() {
        this(System.currentTimeMillis());
    }

    public OnlineTime(final long time) {
        joinTime = new AtomicLong(time);
        // Temp stored time is used to keep track of time played during
        // sessions and between the database being updated.
        // Sessions are defined as when a player is online playing.
        tempStoredTime = new AtomicLong(-1);
    }

    public synchronized void handleLogout() {
        long joinTimeCopy = joinTime.getAndSet(-1);

        // This function can trigger twice for player kick and player quit
        if (joinTimeCopy < 0)
            return;

        // Get the ms that the player was online
        long msOnline = System.currentTimeMillis() - joinTimeCopy;
        long tempTime = tempStoredTime.addAndGet(msOnline);
        PlaytimePro.debug("Added session time (" + msOnline +
                        " ms) to online temp time (" + tempTime + " ms)."
        );
    }

    public long getOnlinePlaytime() {
        long tempTime = 0;
        long joinTimeCopy = joinTime.get();
        if (joinTimeCopy > 0) {
            long msOnline = System.currentTimeMillis() - joinTimeCopy;
            tempTime += (msOnline);
        }

        long tempStoredTimeCopy = tempStoredTime.get();
        if (tempStoredTimeCopy > 0) {
            tempTime += tempStoredTimeCopy;
        }

        return tempTime;
    }

    public synchronized void login() {
        this.joinTime.set(System.currentTimeMillis());
    }

    // Returns cached playtime in seconds and resets the cache
    public synchronized int returnAndReset(long currentTime) {
        long tempTime = 0;

        long joinTimeCopy = joinTime.getAndSet(currentTime);
        if (joinTimeCopy > 0) {
            // Perform some time validations
            if (currentTime > joinTimeCopy)
                tempTime += (currentTime - joinTimeCopy);
        }

        long tempStoredTimeCopy = tempStoredTime.getAndSet(-1);
        if (tempStoredTimeCopy > 0)
            tempTime += tempStoredTimeCopy;

        int secondsTime = (int) TimeUnit.MILLISECONDS.toSeconds(tempTime);

        // Catch the time jumps
        if (TimeUnit.SECONDS.toHours(secondsTime) < 1000) {
            addToCachedTime(secondsTime, secondsTime, secondsTime, secondsTime);
        }
        else {
            final String errorMsg = String.format("Time jump occurred for a player! Was about to add %d secs (%d ms)!", secondsTime, tempTime);
            PlaytimePro.getInstance().getLogger().severe(errorMsg);
        }

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
