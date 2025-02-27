package cz.yorick.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cz.yorick.resources.Util;
import cz.yorick.resources.type.SimpleReloadableResource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ReloadConfigResourcesCommand<S extends CommandSource> {
    private final BiConsumer<S, Text> feedbackSender;
    private ReloadConfigResourcesCommand(CommandDispatcher<S> dispatcher, String commandName, BiConsumer<S, Text> feedbackSender, Predicate<S> canExecute) {
        this.feedbackSender = feedbackSender;
        dispatcher.register(literal(commandName).requires(canExecute)
                .then(literal("all")
                        .executes(context -> reloadAllConfigs(context.getSource()))
                )
                .then(literal("only")
                        .then(argument("config", IdentifierArgumentType.identifier()).suggests(this::suggestConfigResource)
                                .executes(context -> reloadConfig(context.getSource(), context.getArgument("config", Identifier.class)))
                        )
                )
        );
    }

    private LiteralArgumentBuilder<S> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private CompletableFuture<Suggestions> suggestConfigResource(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandUtil.suggestMatching(Util.getReloadableResourceKeys().stream().map(Identifier::toString).toList(), builder);
    }

    private int reloadAllConfigs(S source) {
        Util.getReloadableResources().forEach(reloadableConfig -> reloadableConfig.reload(error -> handleError(source, error)));
        sendSuccess(source, "Reloaded config resources");
        return Command.SINGLE_SUCCESS;
    }

    private int reloadConfig(S source, Identifier id) {
        SimpleReloadableResource<?> config = Util.getReloadableResource(id);
        if(config == null) {
            sendError(source, "Config resource " + id.toString() + " does not exist or is unreloadable");
            return 0;
        }

        config.reload(error -> handleError(source, error));
        sendSuccess(source, "Reloaded the config resource " + id.toString());
        return Command.SINGLE_SUCCESS;
    }

    private void handleError(S source, Exception error) {
        sendError(source, error.getMessage());
        Throwable throwable = error.getCause();
        while (throwable != null) {
            sendError(source, "Caused by " + throwable.getClass().getName() + ":");
            sendError(source, throwable.getMessage());
            throwable = throwable.getCause();
        }
    }

    private void sendSuccess(S source, String message) {
        this.feedbackSender.accept(source, Text.literal(message).formatted(Formatting.GREEN));
    }

    private void sendError(S source, String error) {
        this.feedbackSender.accept(source, Text.literal(error).formatted(Formatting.RED));
    }

    public static void registerServer(CommandDispatcher<ServerCommandSource> dispatcher) {
        new ReloadConfigResourcesCommand<>(dispatcher, "reloadServerConfigs", ServerCommandSource::sendMessage, source -> source.hasPermissionLevel(2));
    }
    public static<S extends CommandSource> void registerClient(CommandDispatcher<S> dispatcher, BiConsumer<S, Text> feedbackConsumer) {
        new ReloadConfigResourcesCommand<>(dispatcher, "reloadConfigs", feedbackConsumer, source -> true);
    }
}
