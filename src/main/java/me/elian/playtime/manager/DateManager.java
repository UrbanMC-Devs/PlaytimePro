package me.elian.playtime.manager;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.TimeType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.logging.Level;

public class DateManager {

    private final static DateManager instance = new DateManager();

    private int month,
                weekOfYear;

    private long lastCheckedTime = 0;

    private static final long HALF_HOUR_MILLIS =  1_800_000;

    private DateManager() {}

    public static DateManager getInstance() {
        return instance;
    }

    private File getFile() {
       return new File(PlaytimePro.getInstance().getDataFolder(), "last_checked_time.txt");
    }

    private boolean updateWeek() {
        int currentWeek = LocalDateTime.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        if(currentWeek != weekOfYear) {
            // Update day of week
           weekOfYear = currentWeek;
           return true;
        }

        return false;
    }

    private boolean updateMonth() {
        int currentMonth = LocalDateTime.now().getMonthValue();

        if(currentMonth != month) {
            // Update day of week
            month = currentMonth;
            return true;
        }

        return false;
    }

    public void updateTime() {
        // Check if it save time has been greater than 30m
        if (System.currentTimeMillis() - lastCheckedTime < HALF_HOUR_MILLIS)
            return;

        boolean dirty = false;

        if (updateWeek()) {
            DataManager.getInstance().purgeTable(TimeType.WEEKLY);
            dirty = true;
        }

        if (updateMonth()) {
            DataManager.getInstance().purgeTable(TimeType.MONTHLY);
            dirty = true;
        }

        if (dirty) {
            // Save to file
            dumpToTextFile();
        }

        lastCheckedTime = System.currentTimeMillis();
    }

    public void loadFromTextFile() {
        final File file = getFile();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                PlaytimePro.getInstance().getLogger().log(Level.WARNING, "Could not create last_checked_time.txt", e);
            }

            month = 0;
            weekOfYear = 0;
        }
        else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                // Over-zealous error handling
                String currentLine = reader.readLine();

                // File format:
                // Month
                // Week of year
                if (currentLine != null) {
                    try {
                        month = Integer.parseInt(currentLine);
                    } catch (NumberFormatException ex) {
                        PlaytimePro.getInstance().getLogger().warning("Error parsing month from last_checked_time.txt");
                        month = 0;
                    }

                    if ((currentLine = reader.readLine()) != null) {
                        try {
                            weekOfYear = Integer.parseInt(currentLine);
                        } catch (NumberFormatException ex) {
                            PlaytimePro.getInstance().getLogger().warning("Error parsing week of year from last_checked_time.txt");
                            weekOfYear = 0;
                        }
                    } else {
                        PlaytimePro.getInstance().getLogger().warning("Could not read week of year from last_checked_time.txt");
                        weekOfYear = 0;
                    }
                } else {
                    PlaytimePro.getInstance().getLogger().warning("Could not read last_checked_time.txt properly!");
                    month = 0;
                    weekOfYear = 0;
                }
            } catch (IOException ex) {
                PlaytimePro.getInstance().getLogger().log(Level.WARNING,"Error reading last_checked_time.txt properly!", ex);
            }
        }

        updateTime();
    }

    private void dumpToTextFile() {
        final File file = getFile();

        try (PrintWriter printWriter = new PrintWriter(file)) {
            printWriter.println(month);
            printWriter.println(weekOfYear);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
