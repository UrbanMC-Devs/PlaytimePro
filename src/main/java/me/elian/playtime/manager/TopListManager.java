package me.elian.playtime.manager;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.PaginalList;
import me.elian.playtime.object.TimeType;
import me.elian.playtime.object.TopListItem;
import me.elian.playtime.util.NameUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class TopListManager {

    private static TopListManager instance = new TopListManager();

    private DataManager data = DataManager.getInstance();

    private List<TopListItem> timesConverted, monthlyTimesConverted, weeklyTimesConverted;
    private PaginalList<TopListItem> topList, monthlyTopList, weeklyTopList;

    private AtomicLong totalHoursOnServer, totalHoursMonth, totalHoursWeek;

    private TopListManager() {
    }

    public static TopListManager getInstance() {
        return instance;
    }

    public List<TopListItem> getTimesConverted() {
        return timesConverted;
    }

    public List<TopListItem> getMonthlyTimesConverted() {
        return monthlyTimesConverted;
    }

    public List<TopListItem> getWeeklyTimesConverted() {
        return weeklyTimesConverted;
    }

    // Run Async
    public void updateTopList() {
        long startTime = System.currentTimeMillis(); // Start timing

        FileConfiguration config = ConfigManager.getConfig();

        totalHoursOnServer = new AtomicLong();
        totalHoursMonth = new AtomicLong();
        totalHoursWeek = new AtomicLong();

        int minimum = config.getInt("top-list-minimum-hours");

        final Map<UUID, Integer> allTimeMap = new HashMap<>(), monthlyMap = new HashMap<>(), weeklyMap = new HashMap<>();

        PlaytimePro.waitToExecuteSync(() -> {
            allTimeMap.putAll(data.getTimesAllTime());
            monthlyMap.putAll(data.getTimesMonthly());
            weeklyMap.putAll(data.getTimesWeekly());
        },200);

        final List<TopListItem> allTimes, monthlyTimes, weeklyTimes;

        allTimes = getTimesSorted(allTimeMap, totalHoursOnServer, minimum);
        topList = new PaginalList<>(allTimes, 10);

        monthlyTimes = getTimesSorted(monthlyMap, totalHoursMonth, 0);
        monthlyTopList = new PaginalList<>(monthlyTimes, 10);

        weeklyTimes = getTimesSorted(weeklyMap, totalHoursWeek, 0);
        weeklyTopList = new PaginalList<>(weeklyTimes, 10);

        PlaytimePro.executeSync(() -> {
            timesConverted = allTimes;
            monthlyTimesConverted = monthlyTimes;
            weeklyTimesConverted = weeklyTimes;
        });

        // Help GC
        allTimeMap.clear();
        monthlyMap.clear();
        weeklyMap.clear();

        Bukkit.getLogger().info("[PlaytimePro] Updating top list took " + (System.currentTimeMillis() - startTime) + "ms to complete!");
    }

    private List<TopListItem> getTimesSorted(Map<UUID, Integer> unsortedMap, AtomicLong hoursAdd,
                                             int minimumInclude) {
        int minimumFetch = ConfigManager.getConfig().getInt("top-list-fetch-minimum-hours");

        List<TopListItem> converted = new ArrayList<>();

        boolean limit = false;

        for (Map.Entry<UUID, Integer> e : unsortedMap.entrySet()) {
            int hours = e.getValue() / 3600;

            hoursAdd.addAndGet(hours);

            if (hours < minimumInclude)
                continue;

            UUID id = e.getKey();
            String name = data.getLastName(id);

            if (name == null && hours >= minimumFetch && !limit) {
                name = NameUtil.getNameByUniqueId(id);

                if (name.equals("_playtime_not_found_")) {
                    name = null;
                } else if (name.equals("_playtime_limit_reached_")) {
                    limit = true;
                    name = null;
                } else {
                    data.setLastName(id, name);
                }
            }

            if (name == null) {
                name = Messages.getString("top_list_unknown");
            }

            converted.add(new TopListItem(name, hours));
        }

        converted.sort(Comparator.comparing(TopListItem::getTime));
        Collections.reverse(converted);

        return converted;
    }

    public String getTopListPage(TimeType type, int pageNumber) {
        PaginalList<TopListItem> list = null;
        AtomicLong totalHours = null;

        switch (type) {
            case ALL_TIME:
                list = topList;
                totalHours = totalHoursOnServer;
                break;
            case MONTHLY:
                list = monthlyTopList;
                totalHours = totalHoursMonth;
                break;
            case WEEKLY:
                list = weeklyTopList;
                totalHours = totalHoursWeek;
                break;
        }

        int amountOfPages = topList.getAmountOfPages();

        if (amountOfPages < pageNumber || pageNumber < 1)
            pageNumber = amountOfPages;

        List<TopListItem> page = list.getPage(pageNumber);

        List<String> lines = new ArrayList<>();

        String topLine = Messages.getString("top_list_" + type.toString().toLowerCase() + "_top", pageNumber,
                amountOfPages, totalHours.get());
        lines.add(topLine);

        for (int i = 0; i < page.size(); i++) {
            TopListItem listItem = page.get(i);

            String name = listItem.getName();
            int hours = listItem.getTime();

            int position = (i + 1 + pageNumber * 10) - 10;

            String formatted = Messages.getString("top_list_" + type.toString().toLowerCase() + "_item", position,
                    name, hours);
            lines.add(formatted);
        }

        return String.join("\n", lines);
    }
}
