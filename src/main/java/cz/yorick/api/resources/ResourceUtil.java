package cz.yorick.api.resources;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cz.yorick.command.CommandUtil;
import cz.yorick.resources.Util;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface ResourceUtil {
    /**
     * Gets the file extension from the path of the identifier
     * */
    static String getFileExtension(Identifier id) {
        return getFileExtension(id.getPath());
    }

    /**
     * Gets the file extension of a path, expected file separator is "/"
     * */
    static String getFileExtension(String path) {
        return Util.getFileExtension(path);
    }

    /**
     * Removes the file extension from the path of the identifier
     * */
    static Identifier removeFileExtension(Identifier id) {
        return id.withPath(removeFileExtension(id.getPath()));
    }

    /**
     * Removes file extension from a path, expected file separator is "/"
     * */
    static String removeFileExtension(String path) {
        return Util.removeFileExtension(path);
    }

    /**
     * Suggests matching from the list
     * @param options The possible options for this argument
     * @param builder The builder provided by {@link com.mojang.brigadier.builder.RequiredArgumentBuilder#suggests(SuggestionProvider)}
     * @return The possible options based on the inputted text
     * */
    static CompletableFuture<Suggestions> suggestMatching(Collection<String> options, SuggestionsBuilder builder) {
        return CommandUtil.suggestMatching(options, builder);
    }

    /**
     * Executes a function - adds withLevel(2) and withSilent()
     * @param source Source to execute as
     * @param functionId Id of the function
     * @return true if the function was executed, false otherwise
     * */
    static boolean executeFunction(ServerCommandSource source, Identifier functionId) {
        return executeFunction(source, functionId, ReturnValueConsumer.EMPTY);
    }

    /**
     * Executes a function - adds withLevel(2) and withSilent()
     * @param source Source to execute as
     * @param functionId Id of the function
     * @param returnValueConsumer Consumer for the return value of the function
     * @return true if the function was executed, false otherwise
     * */
    static boolean executeFunction(ServerCommandSource source, Identifier functionId, ReturnValueConsumer returnValueConsumer) {
        return executeRawFunction(source.withLevel(2).withSilent().mergeReturnValueConsumers(returnValueConsumer, ReturnValueConsumer::chain), functionId);
    }

    /**
     * Executes a function
     * @param source Source to execute as
     * @param functionId Id of the function
     * @return true if the function was executed, false otherwise
     * */
    static boolean executeRawFunction(ServerCommandSource source, Identifier functionId) {
        return executeRawMacroFunction(source, functionId, null);
    }

    /**
     * Executes a macro function - adds withLevel(2) and withSilent()
     * @param source Source to execute as
     * @param functionId Id of the function
     * @param data The data to execute the function with
     * @return true if the function was executed, false otherwise
     * */
    static boolean executeMacroFunction(ServerCommandSource source, Identifier functionId, NbtCompound data) {
        return executeMacroFunction(source, functionId, data, ReturnValueConsumer.EMPTY);
    }

    /**
     * Executes a macro function - adds withLevel(2) and withSilent()
     * @param source Source to execute as
     * @param functionId Id of the function
     * @param data The data to execute the function with
     * @param returnValueConsumer Consumer for the return value of the function
     * @return true if the function was executed, false otherwise
     * */
    static boolean executeMacroFunction(ServerCommandSource source, Identifier functionId, NbtCompound data, ReturnValueConsumer returnValueConsumer) {
        return executeRawMacroFunction(source.withLevel(2).withSilent().mergeReturnValueConsumers(returnValueConsumer, ReturnValueConsumer::chain), functionId, data);
    }

    /**
     * Executes a macro function
     * @param source Source to execute as
     * @param functionId Id of the function
     * @param data The data to execute the function with
     * @return true if the function was executed, false otherwise
     * */
    static boolean executeRawMacroFunction(ServerCommandSource source, Identifier functionId, NbtCompound data) {
        return CommandUtil.executeMacroFunction(source, functionId, data);
    }

    /**
     * Executes a command - adds withLevel(2) and withSilent()
     * @param source Source to execute as
     * @param command The command
     * */
    static void executeCommand(ServerCommandSource source, String command) {
        executeCommand(source, command, ReturnValueConsumer.EMPTY);
    }

    /**
     * Executes a command - adds withLevel(2) and withSilent()
     * @param source Source to execute as
     * @param command The command
     * @param returnValueConsumer Consumer for the return value of the function
     * */
    static void executeCommand(ServerCommandSource source, String command, ReturnValueConsumer returnValueConsumer) {
        executeRawCommand(source.withLevel(2).withSilent().mergeReturnValueConsumers(returnValueConsumer, ReturnValueConsumer::chain), command);
    }

    /**
     * Executes a command
     * @param source Source to execute as
     * @param command The command
     * */
    static void executeRawCommand(ServerCommandSource source, String command) {
        source.getServer().getCommandManager().executeWithPrefix(source, command);
    }
}
