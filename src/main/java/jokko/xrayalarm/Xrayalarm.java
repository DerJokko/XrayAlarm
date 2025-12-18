package jokko.xrayalarm;

import jokko.xrayalarm.config.XrayConfig;
import jokko.xrayalarm.detection.OreBreakListener;
import jokko.xrayalarm.commands.XrayAlarmCommand;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Xrayalarm implements ModInitializer {

	public static final String MOD_ID = "x-ray-alarm";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		XrayConfig.load();
		OreBreakListener.register();
		XrayAlarmCommand.register();
		LOGGER.info("XRayAlarm initialized");
	}
}
