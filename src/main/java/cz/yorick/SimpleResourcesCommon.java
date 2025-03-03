package cz.yorick;

import cz.yorick.command.ReloadConfigResourcesCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleResourcesCommon implements ModInitializer {
	public static final String MOD_ID = "simple-resources";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			//register the reloadServerConfig command only on the dedicated server
			if(environment == CommandManager.RegistrationEnvironment.DEDICATED) {
				ReloadConfigResourcesCommand.registerServer(dispatcher);
			}
		});
	}
}