package jokko.xrayalarm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jokko.xrayalarm.Xrayalarm;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class XrayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config/xray-alarm.json");

    public static boolean enabled = true;
    public static boolean useWebhook = true;
    public static boolean useChat = true;
    public static String webhookUrl = "";
    public static String notifyPermission = "antixray.notify";
    public static int opLevel = 2;

    public static final Map<String, OreConfig> trackedBlocks = new HashMap<>();

    public static void load() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                saveDefault();
                return;
            }

            String content = Files.readString(CONFIG_PATH);
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = GSON.fromJson(content, mapType);

            Map<String, Object> webhookData = (Map<String, Object>) data.get("webhook");
            webhookUrl = (String) webhookData.getOrDefault("url", "");
            useWebhook = (Boolean) webhookData.getOrDefault("enabled", true);

            Map<String, Object> general = (Map<String, Object>) data.get("general");
            notifyPermission = (String) general.getOrDefault("notifyPermission", "antixray.notify");
            opLevel = ((Double) general.getOrDefault("opLevel", 2.0)).intValue();

            List<Map<String, Object>> blocks = (List<Map<String, Object>>) data.get("tracked_blocks");
            trackedBlocks.clear();
            for (Map<String, Object> b : blocks) {
                String blockId = (String) b.get("block_id");
                OreConfig cfg = new OreConfig(
                        blockId,
                        ((Double) b.get("alert_threshold")).intValue(),
                        ((Double) b.get("time_window_minutes")).intValue(),
                        ((Double) b.get("subsequent_alert_threshold")).intValue(),
                        ((Double) b.get("reset_after_minutes")).intValue(),
                        (String) b.get("alert_message")
                );
                trackedBlocks.put(blockId, cfg);
            }

            Xrayalarm.LOGGER.info("XRayAlarm config loaded for " + trackedBlocks.size() + " blocks");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveDefault() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        String defaultJson = Files.readString(XrayConfig.class.getResource("/default-xray-alarm.json").toURI());
        Files.writeString(CONFIG_PATH, defaultJson);
    }

    public record OreConfig(
            String blockId,
            int alertThreshold,
            int timeWindowMinutes,
            int subsequentAlertThreshold,
            int resetAfterMinutes,
            String alertMessage
    ) {}
}
