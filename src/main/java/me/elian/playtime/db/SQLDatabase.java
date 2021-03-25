package me.elian.playtime.db;

import me.elian.playtime.PlaytimePro;
import me.elian.playtime.object.OnlineTime;
import me.elian.playtime.object.SignHead;
import me.elian.playtime.object.TimeType;
import me.elian.playtime.object.TopListItem;
import me.elian.playtime.util.NameUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public abstract class SQLDatabase {

    public abstract String getSimpleName();

    public abstract Connection getConnection();

    public abstract boolean establishConnection();

    public void closeConnection() {}

    protected boolean createTables() {
        try {
            Connection con = getConnection();

            if (con == null)
                return false;

            Statement statement = con.createStatement();

            statement.execute(SQLMessages.get("create_table_all_time"));
            statement.execute(SQLMessages.get("create_table_monthly"));
            statement.execute(SQLMessages.get("create_table_weekly"));
            statement.execute(SQLMessages.get("create_table_heads"));

            statement.close();
        } catch (SQLException e) {
            PlaytimePro.getInstance().getLogger().log(Level.SEVERE, "Error creating tables", e);
            return false;
        }

        return true;
    }

    public void localFillTime(UUID player, String lastName, OnlineTime onlineTime) {
        // We are only going to fetch the playtime if it exists
        // The creation and update will be when the times are saved

        try (Connection con = getConnection()) {
            if (con == null)
                return;

            Statement statement = con.createStatement();

            int allTime = 0;

            ResultSet rs = statement.executeQuery(SQLMessages.get("select_all_time_player", player));

            if (rs.next()) {
                allTime = rs.getInt("time");
            }

            rs.close();

            int monthlyTime = 0;
            int weeklyTime = 0;

            // If all time doesn't exist, no point in fetching the others because they sure don't exist...unless we broke
            if (allTime != 0) {
                // Fetch monthly time
                rs = statement.executeQuery(SQLMessages.get("select_monthly_player", player));

                if (rs.next()) {
                    monthlyTime = rs.getInt("time");
                }

                rs.close();

                // Fetch weekly time
                rs = statement.executeQuery(SQLMessages.get("select_weekly_player", player));

                if (rs.next()) {
                    weeklyTime = rs.getInt("time");
                }

                rs.close();
            }

            statement.execute(SQLMessages.get("name_update", player, lastName));

            PlaytimePro.debug("Fetched login playtime for " + lastName +
                    ". Filling with " + allTime + " all time, " + monthlyTime + " monthly, " + weeklyTime + " weekly.");

            onlineTime.addToCachedTime(allTime, monthlyTime, weeklyTime);
        } catch (SQLException ex) {
            PlaytimePro.getInstance().getLogger().log(Level.SEVERE, "Error thrown while trying to fetch playtime for " + player, ex);
        }
    }

    public int getTime(Statement statement, TimeType type, UUID playerUUID) throws SQLException {
        String sqlQuery = "";

        if (type == TimeType.ALL_TIME) {
            sqlQuery = SQLMessages.get("select_all_time_player", playerUUID);
        } else if (type == TimeType.MONTHLY) {
            sqlQuery = SQLMessages.get("select_monthly_player", playerUUID);
        } else if (type == TimeType.WEEKLY) {
            sqlQuery = SQLMessages.get("select_weekly_player", playerUUID);
        }

        ResultSet rs = statement.executeQuery(sqlQuery);

        int time = -1;

        while (rs.next()) {
            time = rs.getInt("time");
        }

        statement.close();

        return time;
    }

    // Dump Method to dump all times onto one map.
    public void fillTimesToMap(Map<UUID, Integer> allTimeMap, Map<UUID, Integer> monthlyTimeMap, Map<UUID, Integer> weeklyTimeMap) {
        try (Connection con = getConnection()) {
            if (con == null)
                return;

            Statement stmt = con.createStatement();

            getTimes(stmt, TimeType.ALL_TIME, allTimeMap);
            getTimes(stmt, TimeType.MONTHLY, monthlyTimeMap);
            getTimes(stmt, TimeType.WEEKLY, weeklyTimeMap);
        } catch (SQLException ex) {
            PlaytimePro.getInstance().getLogger().log(Level.SEVERE, "Error fetching times from database!", ex);
        }
    }

    public void getTimes(Statement statement, TimeType type, Map<UUID, Integer> timeMap) throws SQLException {
        String sql = "";

        if (type == TimeType.ALL_TIME) {
            sql = SQLMessages.get("select_all_time");
        } else if (type == TimeType.MONTHLY) {
            sql = SQLMessages.get("select_monthly");
        } else if (type == TimeType.WEEKLY) {
            sql = SQLMessages.get("select_weekly");
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
        try (Connection con = getConnection()) {
            if (con == null)
                return null;

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

    public synchronized void updateTimes(Map<UUID, Integer> cachedTimes) {
        if(cachedTimes.isEmpty())
            return;

        try (Connection con = getConnection()) {
            if (con == null)
                return;

            con.setAutoCommit(false);

            // The SQL statements will update the time if the player exists or insert the time

            PreparedStatement allTimeUpdate = con.prepareStatement(SQLMessages.get("prepared_insert_all_time_" + getSimpleName())),
                              monthlyUpdate = con.prepareStatement(SQLMessages.get("prepared_insert_monthly_" + getSimpleName())),
                              weeklyUpdate =  con.prepareStatement(SQLMessages.get("prepared_insert_weekly_" + getSimpleName()));


            for (Entry<UUID, Integer> timeEntry : cachedTimes.entrySet()) {
                String playerUUID = timeEntry.getKey().toString();
                // Insert Player UUIDs to statements
                allTimeUpdate.setString(1, playerUUID);
                monthlyUpdate.setString(1, playerUUID);
                weeklyUpdate.setString(1, playerUUID);

                int time = timeEntry.getValue();
                PlaytimePro.debug("Adding " + time + " to db for UUID " + playerUUID);
                // Insert time
                allTimeUpdate.setInt(2, time);
                allTimeUpdate.setInt(3, time);
                monthlyUpdate.setInt(2, time);
                monthlyUpdate.setInt(3, time);
                weeklyUpdate.setInt(2, time);
                weeklyUpdate.setInt(3, time);

                // Add to batch
                allTimeUpdate.addBatch();
                monthlyUpdate.addBatch();
                weeklyUpdate.addBatch();
            }

            // Execute batch
            allTimeUpdate.executeBatch();
            monthlyUpdate.executeBatch();
            weeklyUpdate.executeBatch();

            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean updateNullNames() {
        long startTime = System.currentTimeMillis(); // Time method

        try (Connection con = getConnection()) {
            if (con == null)
                return true;

            PreparedStatement updateStatement = con.prepareStatement(SQLMessages.get("name_update_" + getSimpleName()));

            Statement fetchStatement = con.createStatement();

            ResultSet rs = fetchStatement.executeQuery(SQLMessages.get("nullnames_get"));

            int resultSize = 0;
            boolean hitRateLimit = false;

            while (rs.next()) {
                resultSize++;

                UUID id = UUID.fromString(rs.getString("player"));

                String name = NameUtil.getNameByUniqueId(id);

                if (name.equals("_playtime_not_found_"))
                    continue;

                if (name.equals("_playtime_limit_reached_")) {
                    hitRateLimit = true;
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

            PlaytimePro.debug("Updating empty names took " + (System.currentTimeMillis() - startTime) + "ms! Updated " + resultSize +  " names!");

            return hitRateLimit || resultSize == 600;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return true;
        }
    }

    public synchronized void updateTimesMigration(TimeType type, Map<UUID, Integer> times) {
        if (times == null || times.isEmpty())
            return;

        try (Connection con = getConnection()) {
            if (con == null)
                return;

            con.setAutoCommit(false);

            PreparedStatement statement;

            if (type == TimeType.ALL_TIME) {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_all_time_" + getSimpleName()));

                for (Entry<UUID, Integer> entry : times.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setInt(2, entry.getValue());

                    statement.setInt(3, entry.getValue());

                    statement.addBatch();
                }
            } else if (type == TimeType.MONTHLY) {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_monthly_" + getSimpleName()));

                for (Entry<UUID, Integer> entry : times.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setInt(2, entry.getValue());
                    statement.setInt(3, entry.getValue());

                    statement.addBatch();
                }
            } else {
                statement = con.prepareStatement(SQLMessages.get("prepared_insert_weekly_" + getSimpleName()));

                for (Entry<UUID, Integer> entry : times.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setInt(2, entry.getValue());
                    statement.setInt(3, entry.getValue());

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

    public synchronized void updateNames(Map<String, String> names) {
        try (Connection con = getConnection()) {
            if (con == null)
                return;

            con.setAutoCommit(false);

            PreparedStatement statement =
                    con.prepareStatement(SQLMessages.get("prepared_insert_all_time_name_" + getSimpleName()));

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

    public synchronized void setTime(UUID uuid, TimeType type, int time) {
        try (Connection con = getConnection()) {
            if (con == null)
                return;

            String stmt = null;

            if (type == TimeType.ALL_TIME)
                stmt = "prepared_set_all";
            else if (type == TimeType.MONTHLY)
                stmt = "prepared_set_monthly";
            else if (type == TimeType.WEEKLY)
                stmt = "prepared_set_weekly";

            try (PreparedStatement statement = con.prepareStatement(SQLMessages.get(stmt))) {

                statement.setInt(1, time);
                statement.setString(2, uuid.toString());

                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean purgeTable(TimeType type) {
        try (Connection con = getConnection()) {
            if (con == null)
                return false;

            String key = null;

            if (type == TimeType.ALL_TIME)
                key = "full_purge_all_time";
            else if (type == TimeType.MONTHLY)
                key = "full_purge_monthly";
            else if (type == TimeType.WEEKLY)
                key = "full_purge_weekly";

            Statement stmt = con.createStatement();

            stmt.execute(SQLMessages.get(key));
            stmt.close();

            return true;
        } catch (SQLException ex) {
            PlaytimePro.getInstance().getLogger().log(Level.SEVERE, "Error purging table " + type.name(), ex);
            return false;
        }
    }

    public int purge(int time) {
        try (Connection con = getConnection()) {
            if (con == null)
                return 0;

            Statement statement = con.createStatement();

            ResultSet rs = statement.executeQuery(SQLMessages.get("count_all_time"));

            rs.next();
            int starting = rs.getInt(1);

            statement.execute(SQLMessages.get("purge_all_time", Integer.toString(time)));

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

    public List<SignHead> getHeads() {
        List<SignHead> heads = new ArrayList<>();

        try (Connection con = getConnection()) {
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
        try (Connection con = getConnection()) {
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
        try (Connection con = getConnection()) {
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
