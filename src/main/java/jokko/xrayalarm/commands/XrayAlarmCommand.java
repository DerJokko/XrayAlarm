package jokko.xrayalarm.commands;

import jokko.xrayalarm.Xrayalarm;
import jokko.xrayalarm.config.XrayConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class XrayAlarmCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("xrayalarm")
                    .requires(src -> src.hasPermissionLevel(XrayConfig.opLevel))
                    .then(CommandManager.literal("setWebhook")
                            .then(CommandManager.argument("url", StringArgumentType.string())
                                    .executes(ctx -> {
                                        XrayConfig.webhookUrl = StringArgumentType.getString(ctx, "url");
                                        XrayConfig.save(); // <-- Änderungen speichern
                                        ctx.getSource().sendFeedback(Text.of("Webhook gesetzt!"), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("useWebhook")
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        XrayConfig.useWebhook = BoolArgumentType.getBool(ctx, "enabled");
                                        XrayConfig.save(); // <-- Änderungen speichern
                                        ctx.getSource().sendFeedback(Text.of("Webhook Nutzung auf " + XrayConfig.useWebhook + " gesetzt"), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("useChat")
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        XrayConfig.useChat = BoolArgumentType.getBool(ctx, "enabled");
                                        XrayConfig.save(); // <-- Änderungen speichern
                                        ctx.getSource().sendFeedback(Text.of("Chat Notifications auf " + XrayConfig.useChat + " gesetzt"), false);
                                        return 1;
                                    })
                            )
                    )
                    .then(CommandManager.literal("enable")
                            .executes(ctx -> {
                                XrayConfig.enabled = true;
                                XrayConfig.save(); // <-- Änderungen speichern
                                ctx.getSource().sendFeedback(Text.of("XRayAlarm aktiviert"), false);
                                return 1;
                            })
                    )
                    .then(CommandManager.literal("disable")
                            .executes(ctx -> {
                                XrayConfig.enabled = false;
                                XrayConfig.save(); // <-- Änderungen speichern
                                ctx.getSource().sendFeedback(Text.of("XRayAlarm deaktiviert"), false);
                                return 1;
                            })
                    )

            );
        });
    }
}
