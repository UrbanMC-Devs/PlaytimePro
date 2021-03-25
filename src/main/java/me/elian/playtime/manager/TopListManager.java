package me.elian.playtime.manager;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.PaginalList;
import me.elian.playtime.object.TimeType;
import me.elian.playtime.object.TopListItem;
import me.elian.playtime.runnable.NullNameUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TopListManager {

    private static final TopListManager instance = new TopListManager();

    private List<TopListItem> timesConverted, monthlyTimesConverted, weeklyTimesConverted;
    private PaginalList<TopListItem> topList, monthlyTopList, weeklyTopList;

    private AtomicLong totalHoursOnServer = new AtomicLong(),
                       totalHoursMonth = new AtomicLong(),
                       totalHoursWeek = new AtomicLong();

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

    // Silver - Run Async
    public synchronized void updateTopListSorted() {
        long startTime = System.currentTimeMillis(); // Start timing

        totalHoursOnServer.set(0);
        totalHoursMonth.set(0);
        totalHoursWeek.set(0);

        int minimum = ConfigManager.getTopMinimumHours();

        final List<TopListItem> allTimes, monthlyTimes, weeklyTimes;

        allTimes = getTimesSorted("all_time", minimum, totalHoursOnServer);
        topList = new PaginalList<>(allTimes, 10);

        monthlyTimes = getTimesSorted("monthly", 0, totalHoursMonth);
        monthlyTopList = new PaginalList<>(monthlyTimes, 10);

        weeklyTimes = getTimesSorted("weekly", 0, totalHoursWeek);
        weeklyTopList = new PaginalList<>(weeklyTimes, 10);

        // We just atomically assign the variables, no need for more concurrency handling
        // Pretty sure this is fine since COW does it too
        timesConverted = allTimes;
        monthlyTimesConverted = monthlyTimes;
        weeklyTimesConverted = weeklyTimes;

        NullNameUpdater.runTask();

        PlaytimePro.debug("Updating top list took " + (System.currentTimeMillis() - startTime) + "ms to complete!");
    }

    // Silver Start - Rewrite sort method fetching list sorted from SQL
    private List<TopListItem> getTimesSorted(String table, int minimumInclude, AtomicLong hoursAdd) {
        return DataManager.getInstance().getSortedTimes(table, minimumInclude, hoursAdd);
    }
    // Silver End

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

        List<String> lines = new ArrayList<>();

        String topLine = Messages.getString("top_list_" + type.toString().toLowerCase() + "_top", pageNumber,
                amountOfPages, totalHours.get());
        lines.add(topLine);

        // On a fresh install, the top list will have 0 player entries and thus 0 pages.
        if (pageNumber > 0) {
            List<TopListItem> page = list.getPage(pageNumber);

            for (int i = 0; i < page.size(); i++) {
                TopListItem listItem = page.get(i);

                String name = listItem.getName();
                int hours = listItem.getTime();

                int position = (i + 1 + pageNumber * 10) - 10;

                String formatted = Messages.getString("top_list_" + type.toString().toLowerCase() + "_item", position,
                        name, hours);
                lines.add(formatted);
            }
        }

        return String.join("\n", lines);
    }
}
