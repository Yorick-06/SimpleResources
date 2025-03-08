package cz.yorick;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.yorick.api.resources.ReloadableResourceKey;
import cz.yorick.api.resources.SimpleResources;
import cz.yorick.command.SimpleResourcesServerCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleResourcesCommon implements ModInitializer {
	public static final String MOD_ID = "simple-resources";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static ReloadableResourceKey<Pair<String, Boolean>> CONFIG;
	private static boolean loadedConfig = false;

	@Override
	public void onInitialize() {
		ensureRegistered();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			//register the reloadServerConfig command only on the dedicated server
			if(environment == CommandManager.RegistrationEnvironment.DEDICATED) {
				new SimpleResourcesServerCommand(dispatcher);
			}
		});
	}

	public static void ensureRegistered() {
		if(!loadedConfig) {
			loadedConfig = true;
			MapCodec<Pair<String, Boolean>> codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codecs.NON_EMPTY_STRING.fieldOf("preferred_format").forGetter(Pair::getFirst),
				Codec.BOOL.fieldOf("broadcast_reload_errors").forGetter(Pair::getSecond)
			).apply(instance, Pair::new));
			CONFIG = SimpleResources.reloadableConfig(Identifier.of(MOD_ID, "config"), () -> Pair.of("json", true), codec.codec());
		}
	}

	public static String getPreferredFormat() {
		return CONFIG != null ? CONFIG.getValue().getFirst() : "json";
	}

	public static boolean shouldBroadcastErrors() {
		return CONFIG != null ? CONFIG.getValue().getSecond() : true;
	}
}