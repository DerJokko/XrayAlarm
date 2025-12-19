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
                // continue to load the newly created default file
            }

            String content = Files.readString(CONFIG_PATH);
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = GSON.fromJson(content, mapType);

            // load enabled and useChat (robustly handle boolean/number/string)
            Object enabledObj = data.get("enabled");
            if (enabledObj instanceof Boolean) {
                enabled = (Boolean) enabledObj;
            } else if (enabledObj instanceof Number) {
                enabled = ((Number) enabledObj).intValue() != 0;
            } else if (enabledObj instanceof String) {
                enabled = Boolean.parseBoolean((String) enabledObj);
            }

            Object useChatObj = data.get("useChat");
            if (useChatObj instanceof Boolean) {
                useChat = (Boolean) useChatObj;
            } else if (useChatObj instanceof Number) {
                useChat = ((Number) useChatObj).intValue() != 0;
            } else if (useChatObj instanceof String) {
                useChat = Boolean.parseBoolean((String) useChatObj);
            }

            Object webhookObj = data.get("webhook");
            Map<String, Object> webhook;
            if (webhookObj instanceof Map) {
                webhook = (Map<String, Object>) webhookObj;
            } else {
                // missing or invalid webhook config -> use defaults
                webhook = new HashMap<>();
            }

            webhookUrl = (String) webhook.getOrDefault("url", "");
            // parse booleans robustly
            Object useWebhookObj = webhook.get("enabled");
            if (useWebhookObj instanceof Boolean) {
                useWebhook = (Boolean) useWebhookObj;
            } else if (useWebhookObj instanceof Number) {
                useWebhook = ((Number) useWebhookObj).intValue() != 0;
            } else if (useWebhookObj instanceof String) {
                useWebhook = Boolean.parseBoolean((String) useWebhookObj);
            } else {
                useWebhook = false;
            }

            Object usePingRoleObj = webhook.get("usePingRole");
            if (usePingRoleObj instanceof Boolean) {
                usePingRole = (Boolean) usePingRoleObj;
            } else if (usePingRoleObj instanceof Number) {
                usePingRole = ((Number) usePingRoleObj).intValue() != 0;
            } else if (usePingRoleObj instanceof String) {
                usePingRole = Boolean.parseBoolean((String) usePingRoleObj);
            } else {
                usePingRole = false;
            }

            pingRole = (String) webhook.getOrDefault("pingRole", "");

            Map<String, Object> general = (Map<String, Object>) data.get("general");
            if (general == null) general = new HashMap<>();
            notifyPermission = (String) general.getOrDefault("notifyPermission", "antixray.notify");
            Object opLevelObj = general.getOrDefault("opLevel", 2);
            if (opLevelObj instanceof Number) {
                opLevel = ((Number) opLevelObj).intValue();
            } else {
                try {
                    opLevel = Integer.parseInt(opLevelObj.toString());
                } catch (Exception ex) {
                    opLevel = 2;
                }
            }

            List<Map<String, Object>> blocks = (List<Map<String, Object>>) data.get("tracked_blocks");
            if (blocks == null) blocks = Collections.emptyList();
            trackedBlocks.clear();
            for (Map<String, Object> b : blocks) {
                String blockId = (String) b.get("block_id");
                int alertThreshold = toInt(b.get("alert_threshold"), 0);
                int timeWindow = toInt(b.get("time_window_minutes"), 0);
                int subsequentAlert = toInt(b.get("subsequent_alert_threshold"), 0);
                int resetAfter = toInt(b.get("reset_after_minutes"), 0);
                String alertMessage = (String) b.getOrDefault("alert_message", "");
                OreConfig cfg = new OreConfig(
                        blockId,
                        alertThreshold,
                        timeWindow,
                        subsequentAlert,
                        resetAfter,
                        alertMessage
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
            root.put("enabled", enabled);
            root.put("useChat", useChat);

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


    private static int toInt(Object obj, int def) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt((String) obj); } catch (Exception e) { return def; }
        }
        return def;
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
