package cz.yorick;

import cz.yorick.command.SimpleResourcesClientCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class SimpleResourcesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> new SimpleResourcesClientCommand(dispatcher));
	}
}