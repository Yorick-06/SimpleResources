package cz.yorick.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cz.yorick.SimpleResourcesCommon;
import net.minecraft.command.CommandExecutionContext;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.Procedure;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CommandUtil {
    public static CompletableFuture<Suggestions> suggestMatching(Collection<String> options, SuggestionsBuilder builder) {
        String input = builder.getInput();
        if(input.endsWith(" ")) {
            options.forEach(builder::suggest);
            return builder.buildFuture();
        }

        String[] splitInput = input.split(" ");
        String typing = splitInput[splitInput.length - 1];
        options.stream().filter(key -> key.startsWith(typing)).forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static boolean executeMacroFunction(ServerCommandSource source, Identifier functionId, NbtCompound data) {
        Optional<CommandFunction<ServerCommandSource>> function = source.getServer().getCommandFunctionManager().getFunction(functionId);
        return function.isEmpty() ? false : executeMacroFunction(source, function.get(), data);
    }

    public static boolean executeMacroFunction(ServerCommandSource source, CommandFunction<ServerCommandSource> function, NbtCompound data) {
        try {
            Procedure<ServerCommandSource> procedure = function.withMacroReplaced(data, source.getDispatcher());
            CommandManager.callWithContext(source, (context) -> CommandExecutionContext.enqueueProcedureCall(context, procedure, source, ReturnValueConsumer.EMPTY));
            return true;
        } catch (Exception e) {
            source.sendError(Text.literal("Something went wrong trying to execute that..."));
            SimpleResourcesCommon.LOGGER.warn("Failed to execute function '" + function.id() + "' with data " + data, e);
            return false;
        }
    }
}
