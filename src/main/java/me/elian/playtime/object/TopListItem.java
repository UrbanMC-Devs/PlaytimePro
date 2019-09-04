package me.elian.playtime.object;

public class TopListItem {

    private String name;
    private int time;

    public TopListItem(String name, int time) {
        this.name = name;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public int getTime() {
        return time;
    }
}
