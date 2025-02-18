package cz.yorick;

import cz.yorick.command.ReloadConfigResourcesCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public class SimpleResourcesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				ReloadConfigResourcesCommand.registerClient(dispatcher, (source, text) -> MinecraftClient.getInstance().player.sendMessage(text, false))
		);
	}
}