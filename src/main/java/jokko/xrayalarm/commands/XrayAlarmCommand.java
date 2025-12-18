package jokko.xrayalarm.commands;

import jokko.xrayalarm.config.XrayConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class XrayAlarmCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                Commands.literal("xrayalarm")
                    .then(Commands.literal("setwebhook")
                        .then(Commands.argument("url", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                XrayConfig.webhookUrl = StringArgumentType.getString(ctx, "url");
                                XrayConfig.save();
                                ctx.getSource().sendSuccess(() -> Component.literal("§c§l[XrayAlarm] §rWebhook set to: §7" + XrayConfig.webhookUrl), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("usewebhook")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                XrayConfig.useWebhook = BoolArgumentType.getBool(ctx, "enabled");
                                XrayConfig.save();
                                ctx.getSource().sendSuccess(() -> Component.literal("§c§l[XrayAlarm] §rChanged using Webhooks to " + (XrayConfig.useWebhook ? "§aTrue§r!" : "§cFalse§r!")), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("usechat")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                            .executes(ctx -> {
                                XrayConfig.useChat = BoolArgumentType.getBool(ctx, "enabled");
                                XrayConfig.save();
                                ctx.getSource().sendSuccess(() -> Component.literal("§c§l[XrayAlarm] §rSet chat notifications to " + (XrayConfig.useChat ? "§aTrue§r!" : "§cFalse§r!")), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("toggle")
                        .executes(ctx -> {
                            XrayConfig.enabled = !XrayConfig.enabled;
                            XrayConfig.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§c§l[XrayAlarm] §rXRayAlarm " + (XrayConfig.enabled ? "§aenabled§r!" : "§cdisabled§r!")), false);
                            return 1;
                        })
                    )
                    .then(Commands.literal("info")
                        .executes(ctx -> {
                            String webhookUrl = XrayConfig.webhookUrl.isEmpty() ? "Not set" : XrayConfig.webhookUrl;
                            String info = "\n" +
                                "§6========== XRayAlarm Settings ==========§r\n" +
                                "§7Enabled: §r" + (XrayConfig.enabled ? "§aTrue" : "§cFalse") + "\n" +
                                "§7Chat Notifications: §r" + (XrayConfig.useChat ? "§aTrue" : "§cFalse") + "\n" +
                                "§7Webhook Enabled: §r" + (XrayConfig.useWebhook ? "§aTrue" : "§cFalse") + "\n" +
                                "§7Webhook URL: §r" + webhookUrl + "\n" +
                                "§6=========================================§r";
                            ctx.getSource().sendSuccess(() -> Component.literal(info), false);
                            return 1;
                        })
                    )
            );
        });
    }
}
