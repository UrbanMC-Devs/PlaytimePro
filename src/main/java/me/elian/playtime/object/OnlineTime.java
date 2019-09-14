package me.elian.playtime.object;

public class OnlineTime {

    private long joinTime;
    private long tempStoredTime = -1; // This is the time stored when the database hasn't updated

    public OnlineTime() {
        joinTime = System.currentTimeMillis();
    }

    public OnlineTime(final long time) {
        joinTime = time;
    }

    public void handleLogout() {
        tempStoredTime += System.currentTimeMillis() - joinTime;
    }

    public long getUnstoredPlaytime() {
        long tempTime = 0;

        if (joinTime != -1) tempTime += System.currentTimeMillis() - joinTime;

        if (tempStoredTime != -1) tempTime += tempStoredTime;

        return tempTime;
    }

    public void login() {
        this.joinTime = System.currentTimeMillis();
    }

}
