package cz.yorick.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

public class SimpleResourcesServerCommand extends SimpleResourcesCommand<ServerCommandSource> {
    public SimpleResourcesServerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        super(dispatcher, "simpleResourcesServer", ServerCommandSource::sendMessage, source -> source.hasPermissionLevel(2));
    }
}
