package me.elian.playtime.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

public class NameUtil {

    public static String getNameByUniqueId(UUID id) {
        String formattedId = id.toString().replace("-", "");
        String link = "https://api.mojang.com/user/profiles/" + formattedId + "/names";

        try {
            URL url = new URL(link);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            Scanner scanner = new Scanner(con.getInputStream());
            String json = scanner.nextLine();

            scanner.close();
            con.disconnect();

            JsonElement element = new Gson().fromJson(json, JsonElement.class);
            JsonArray array = element.getAsJsonArray();

            JsonObject obj = array.get(array.size() - 1).getAsJsonObject();
            return obj.get("name").getAsString();
        } catch (Exception e) {
            if (e instanceof NoSuchElementException)
                return "_playtime_not_found_";
            else
                return "_playtime_limit_reached_";
        }
    }
}
