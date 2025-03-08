package cz.yorick.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class SimpleResourcesClientCommand extends SimpleResourcesCommand<FabricClientCommandSource> {
    public SimpleResourcesClientCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        super(dispatcher, "simpleResources", (source, text) -> source.getPlayer().sendMessage(text, false), source -> true);
    }
}
