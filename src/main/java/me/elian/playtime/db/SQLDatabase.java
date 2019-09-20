package me.elian.playtime.db;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.OnlineTime;
import me.elian.playtime.object.SignHead;
import me.elian.playtime.object.TimeType;
import me.elian.playtime.object.TopListItem;
import me.elian.playtime.runnable.NullNameUpdater;
import me.elian.playtime.util.NameUtil;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public abstract class SQLDatabase {

    public abstract Connection getConnection();

    public abstract PlaytimePro getPlugin();

    protected void createTables() {
        try {
            Connection con = getConnection();

            if (con == null)
                return;

            Statement statement = con.createStatement();

            statement.execute(SQLMessages.get("create_table_all_time"));
            statement.execute(SQLMessages.get("create_table_monthly"));
            statement.execute(SQLMessages.get("create_table_weekly"));
            statement.execute(SQLMessages.get("create_table_heads"));

            statement.close();
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Error creating tables", e);
        }
    }

    public void getTimes(Statement statement, TimeType type, Map<UUID, Integer> timeMap) throws SQLException {
        String sql = "";

        if (type == TimeType.ALL_TIME) {
            sql = SQLMessages.get("select_all_time");
        } else if (type == TimeType.MONTHLY) {
            sql = SQLMessages.get("select_monthly", getMonthString());
        } else if (type == TimeType.WEEKLY) {
            sql = SQLMessages.get("select_weekly", getWeekString());
        }

        ResultSet rs = statement.executeQuery(sql);

        while (rs.next()) {
            UUID player = UUID.fromString(rs.getString("player"));
            int time = rs.getInt("time");

            timeMap.put(player, time);
        }

        statement.close();
    }

    public List<TopListItem> getSortedTimes(String table, int minTime, AtomicLong totalHours) {
        try {
            Connection con = getConnection();

            if (con == null) return null;

            Statement statement = con.createStatement();

            ResultSet results = statement.executeQuery(SQLMessages.get("toplist_" + table, minTime));

            List<TopListItem> topList = new ArrayList<>();

            int thours = 0;
            while (results.next()) {
                int hours = results.getInt("time") / 3600;

                thours += hours;

                topList.add(new TopListItem(results.getString("last_name"), hours));
            }

            if (totalHours != null)
                totalHours.addAndGet(thours);

            statement.close();

            return topList;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public synchronized void updateTimes(TimeType type, Map<UUID, OnlineTime> playerJoins, Map<UUID, Integer> oldTimes) {
        String databaseType = this instanceof MySQL ? "mysql" : "sqlite";

        if (playerJoins == null || playerJoins.isEmpty())
            return;

        try {
            Connection con = getConnection();

            if (con == null)
                return;

            con.setAutoCommit(false);

            PreparedStatement statement;

            if (type == TimeType.ALL_TIME) {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_all_time_" + databaseType));

                for (Entry<UUID, OnlineTime> entry : playerJoins.entrySet()) {
                    UUID id = entry.getKey();
                    int seconds = calculateSeconds(entry.getValue()) + oldTimes.getOrDefault(id, 0);

                    oldTimes.put(id, seconds); // Update times locally w/o having to wipe the entire map and fill it from the DB

                    statement.setString(1, id.toString());
                    statement.setInt(2, seconds);

                    statement.setInt(3, seconds);

                    statement.addBatch();
                }
            } else {
                final String timeString;

                if (type == TimeType.MONTHLY) {
                    statement = con.prepareStatement(SQLMessages.get("prepared_insert_monthly_" + databaseType));
                    timeString = getMonthString();
                } else {
                    statement = con.prepareStatement(SQLMessages.get("prepared_insert_weekly_" + databaseType));
                    timeString = getWeekString();
                }

                for (Entry<UUID, OnlineTime> entry : playerJoins.entrySet()) {
                    UUID id = entry.getKey();
                    int seconds = calculateSeconds(entry.getValue()) + oldTimes.getOrDefault(id, 0);

                    oldTimes.put(id, seconds); // Update times locally w/o having to wipe the entire map and fill it from the DB

                    statement.setString(1, id.toString());
                    statement.setInt(2, seconds);
                    statement.setString(3, timeString);

                    statement.setInt(4, seconds);
                    statement.setString(5, timeString);

                    statement.addBatch();
                }
            }

            statement.executeBatch();
            statement.close();

            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean updateNullNames() {
        long startTime = System.currentTimeMillis(); // Time method

        String databaseType = this instanceof MySQL ? "mysql" : "sqlite";

        try {
            Connection con = getConnection();

            if (con == null) return true;

            PreparedStatement updateStatement = con.prepareStatement(SQLMessages.get("name_update_" + databaseType));

            Statement fetchStatement = con.createStatement();

            ResultSet rs = fetchStatement.executeQuery(SQLMessages.get("nullnames_get"));

            int resultSize = 0;

            while (rs.next()) {
                resultSize++;

                UUID id = UUID.fromString(rs.getString("player"));

                String name = NameUtil.getNameByUniqueId(id);

                if (name.equals("_playtime_not_found_"))
                    continue;

                if (name.equals("_playtime_limit_reached_")) {
                    resultSize = 600; // Fool the counter
                    break;
                }

                updateStatement.setString(1, id.toString());
                updateStatement.setString(2, name);
                updateStatement.setString(3, name);

                updateStatement.addBatch();
            }

            updateStatement.executeBatch();
            updateStatement.close();

            fetchStatement.close();

            Bukkit.getLogger().info("[PlaytimePro] Updating empty names took " + (System.currentTimeMillis() - startTime) + "ms! Updated " + resultSize +  " names!");

            return resultSize == 600;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return true;
        }
    }

    private int calculateSeconds(OnlineTime time) {
        return (int) TimeUnit.MILLISECONDS.toSeconds(time.getUnstoredPlaytime());
    }

    public synchronized void updateTimesMigration(TimeType type, Map<UUID, Integer> times) {
        String databaseType = this instanceof MySQL ? "mysql" : "sqlite";

        if (times == null || times.isEmpty())
            return;

        try {
            Connection con = getConnection();

            if (con == null)
                return;

            con.setAutoCommit(false);

            PreparedStatement statement;

            if (type == TimeType.ALL_TIME) {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_all_time_" + databaseType));

                for (Entry<UUID, Integer> entry : times.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setInt(2, entry.getValue());

                    statement.setInt(3, entry.getValue());

                    statement.addBatch();
                }
            } else if (type == TimeType.MONTHLY) {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_monthly_" + databaseType));

                for (Entry<UUID, Integer> entry : times.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setInt(2, entry.getValue());
                    statement.setString(3, getMonthString());

                    statement.setInt(4, entry.getValue());
                    statement.setString(5, getMonthString());

                    statement.addBatch();
                }
            } else {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_weekly_" + databaseType));

                for (Entry<UUID, Integer> entry : times.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setInt(2, entry.getValue());
                    statement.setString(3, getWeekString());

                    statement.setInt(4, entry.getValue());
                    statement.setString(5, getWeekString());

                    statement.addBatch();
                }
            }

            statement.executeBatch();
            statement.close();

            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setLastName(UUID id, String name) {
        Connection con = getConnection();

        if (con == null)
            return;

        try {
            final String databaseType = this instanceof MySQL ? "mysql" : "sqlite";

            final PreparedStatement updateStatement = con.prepareStatement(SQLMessages.get("name_update_" + databaseType));

            updateStatement.setString(1, id.toString());
            updateStatement.setString(2, name);
            updateStatement.setString(3, name);

            updateStatement.execute();

            updateStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateNames(Map<String, String> names) {
        Connection con = getConnection();

        if (con == null)
            return;

        try {
            String databaseType = this instanceof MySQL ? "mysql" : "sqlite";

            con.setAutoCommit(false);

            PreparedStatement statement =
                    con.prepareStatement(SQLMessages.get("prepared_insert_all_time_name_" + databaseType));

            for (Entry<String, String> e : names.entrySet()) {
                statement.setString(1, e.getKey());
                statement.setString(2, e.getValue());

                statement.setString(3, e.getValue());

                statement.addBatch();
            }

            statement.executeBatch();

            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int purge(int time) {
        try {
            Connection con = getConnection();

            if (con == null)
                return 0;

            Statement statement = con.createStatement();

            ResultSet rs = statement.executeQuery(SQLMessages.get("count_all_time"));

            rs.next();
            int starting = rs.getInt(1);

            statement.execute(SQLMessages.get("purge_all_time", Integer.toString(time)));
            statement.execute(SQLMessages.get("purge_monthly", getMonthString()));
            statement.execute(SQLMessages.get("purge_weekly", getWeekString()));

            rs = statement.executeQuery(SQLMessages.get("count_all_time"));

            rs.next();
            int finish = rs.getInt(1);

            statement.close();

            return starting - finish;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getMonthString() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());

        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1);
    }

    public String getWeekString() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());

        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.WEEK_OF_YEAR);
    }

    public List<SignHead> getHeads() {
        List<SignHead> heads = new ArrayList<>();

        try {
            Connection con = getConnection();

            if (con == null)
                return heads;

            Statement statement = con.createStatement();

            ResultSet rs = statement.executeQuery(SQLMessages.get("select_heads"));

            while (rs.next()) {
                String location = rs.getString("location");
                String headLocation = rs.getString("head_location");
                int position = rs.getInt("position");
                TimeType type = TimeType.valueOf(rs.getString("type"));

                if ("null".equals(headLocation))
                    headLocation = null;

                heads.add(new SignHead(location, headLocation, position, type));
            }

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return heads;
    }

    public void addHead(SignHead head) {
        try {
            Connection con = getConnection();

            if (con == null)
                return;

            Statement statement = con.createStatement();

            String sql = SQLMessages.get("heads_add",
                    head.getLocation(), head.getHeadLocation(), head.getPosition(), head.getType().toString());
            statement.execute(sql);

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeHead(SignHead head) {
        try {
            Connection con = getConnection();

            if (con == null)
                return;

            Statement statement = con.createStatement();
            statement.execute(SQLMessages.get("heads_remove", head.getLocation()));

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
