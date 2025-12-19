package jokko.xrayalarm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import jokko.xrayalarm.Xrayalarm;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class XrayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Path.of("config/xray-alarm.json");

    public static boolean enabled = true;
    public static boolean useWebhook = false;
    public static boolean useChat = true;
    public static boolean usePingRole = false;
    public static String webhookUrl = "";
    public static String pingRole = "";
    public static String notifyPermission = "antixray.notify";
    public static int opLevel = 2;

    public static final Map<String, OreConfig> trackedBlocks = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void load() {
        try {
            if (Files.notExists(CONFIG_PATH)) {
                saveDefault();
                return;
            }

            String content = Files.readString(CONFIG_PATH);
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = GSON.fromJson(content, mapType);

            Object webhookObj = data.get("webhook");
            Map<String, Object> webhook = null;

            if (webhookObj instanceof Map) {
                webhook = (Map<String, Object>) webhookObj;
            } else {
                // Handle error - webhook config is missing or invalid
                throw new IllegalStateException("Webhook configuration is not a map");
            }

            webhookUrl = (String) webhook.getOrDefault("url", "");
            useWebhook = (Boolean) webhook.getOrDefault("enabled", true);
            usePingRole = (Boolean) webhook.getOrDefault("usePingRole", false);
            pingRole = (String) webhook.getOrDefault("pingRole", "");

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

    public static void save() {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("version", "1.0.0");
            root.put("configId", "xrayalarm");

            Map<String, Object> general = new LinkedHashMap<>();
            general.put("notifyPermission", notifyPermission);
            general.put("permissionLevel", opLevel);
            general.put("opLevel", opLevel);
            root.put("general", general);

            Map<String, Object> alerts = new LinkedHashMap<>();
            alerts.put("continued_alert_prefix", "<red>[Continued]</red> ");
            root.put("alerts", alerts);

            Map<String, Object> webhook = new LinkedHashMap<>();
            webhook.put("enabled", useWebhook);
            webhook.put("url", webhookUrl);
            webhook.put("usePingRole", usePingRole);
            webhook.put("pingRole", pingRole);
            root.put("webhook", webhook);

            List<Map<String, Object>> blocks = new ArrayList<>();
            for (OreConfig cfg : trackedBlocks.values()) {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("block_id", cfg.blockId());
                b.put("alert_threshold", cfg.alertThreshold());
                b.put("time_window_minutes", cfg.timeWindowMinutes());
                b.put("subsequent_alert_threshold", cfg.subsequentAlertThreshold());
                b.put("reset_after_minutes", cfg.resetAfterMinutes());
                b.put("alert_message", cfg.alertMessage());
                blocks.add(b);
            }
            root.put("tracked_blocks", blocks);

            String json = GSON.toJson(root);
            Files.writeString(CONFIG_PATH, json);

            Xrayalarm.LOGGER.info("XRayAlarm config saved.");
        } catch (Exception e) {
            Xrayalarm.LOGGER.error("Fehler beim Speichern der XRayAlarm Config", e);
        }
    }


    public static void saveDefault() throws Exception {
        Files.createDirectories(CONFIG_PATH.getParent());
        try (var in = XrayConfig.class.getResourceAsStream("/default-xray-alarm.json")) {
            if (in == null) throw new RuntimeException("Default JSON nicht gefunden!");
            Files.copy(in, CONFIG_PATH);
        }
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
