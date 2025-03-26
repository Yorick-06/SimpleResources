package cz.yorick.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import cz.yorick.api.resources.ResourceReadWriter;
import cz.yorick.resources.ErrorUtil;
import cz.yorick.resources.ResourceParseException;
import cz.yorick.resources.Util;
import cz.yorick.resources.loader.CodecResourceReadWriter;
import cz.yorick.resources.type.SimpleReloadableResource;
import cz.yorick.resources.type.SimpleResource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class SimpleResourcesCommand<S extends CommandSource> {
    private final BiConsumer<S, Text> feedbackSender;
    protected SimpleResourcesCommand(CommandDispatcher<S> dispatcher, String commandName, BiConsumer<S, Text> feedbackSender, Predicate<S> canExecute) {
        this.feedbackSender = feedbackSender;
        dispatcher.register(literal(commandName).requires(canExecute)
            .then(literal("reload")
                .executes(context -> executeReloadAll(context.getSource()))
                .then(argument("config", IdentifierArgumentType.identifier()).suggests(this::suggestReloadableConfigResources)
                    .executes(context -> executeReload(context.getSource(), context.getArgument("config", Identifier.class)))
                )
            )
            .then(literal("convert")
                .then(argument("format", StringArgumentType.string()).suggests(this::suggestFormat)
                    .then(argument("id", IdentifierArgumentType.identifier()).suggests(this::suggestConfigResources)
                        .executes(context -> convertConfig(context.getSource(), StringArgumentType.getString(context, "format"), context.getArgument("id", Identifier.class), null))
                        .then(argument("path", StringArgumentType.string())
                            .executes(context -> convertConfig(context.getSource(), StringArgumentType.getString(context, "format"), context.getArgument("id", Identifier.class), StringArgumentType.getString(context, "path")))
                        )
                    )
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

    private CompletableFuture<Suggestions> suggestReloadableConfigResources(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandUtil.suggestMatching(Util.getReloadableResourceKeys().stream().map(Identifier::toString).toList(), builder);
    }

    private CompletableFuture<Suggestions> suggestConfigResources(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandUtil.suggestMatching(Util.getResourceKeys().stream().map(Identifier::toString).toList(), builder);
    }

    private CompletableFuture<Suggestions> suggestFormat(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandUtil.suggestMatching(CodecResourceReadWriter.getRegisteredExtensions(), builder);
    }

    private int executeReloadAll(S source) {
        Util.getReloadableResources().forEach(reloadableConfig -> reloadableConfig.reload(error -> handleError(source, error)));
        sendSuccess(source, "Reloaded config resources");
        return Command.SINGLE_SUCCESS;
    }

    private int executeReload(S source, Identifier id) {
        SimpleReloadableResource<?> config = Util.getReloadableResource(id);
        if(config == null) {
            sendError(source, "Config resource " + id + " does not exist or is unreloadable");
            return 0;
        }

        config.reload(error -> handleError(source, error));
        sendSuccess(source, "Reloaded the config resource " + id);
        return Command.SINGLE_SUCCESS;
    }

    protected int convertConfig(S source, String format, Identifier id, String path) {
        SimpleResource<?> config = Util.getResource(id);
        if(config == null) {
            sendError(source, "Config resource " + id + " does not exist");
            return 0;
        }

        ResourceReadWriter<?> readWriter = config.getReadWriter();
        if(readWriter instanceof CodecResourceReadWriter<?> codecResourceReadWriter) {
            return convertPath(source, format,  config.getFile().toPath(), path);
        }

        sendError(source, "Config resource " + id + " is not a codec resource and cannot be converted");
        return 0;
    }

    protected int convertPath(S source, String format, Path startPath, String path) {
        CodecResourceReadWriter.DynamicOpsParser<?> requiredParser = CodecResourceReadWriter.getParser(format);
        if(requiredParser == null) {
            sendError(source, "No parser registered for format " + format);
            return 0;
        }

        if(path != null) {
            for (String pathPart : path.split("/")) {
                startPath = startPath.resolve(pathPart);
            }
        }

        File file = startPath.toFile();
        if(!file.exists()) {
            sendError(source, "The specified file does not exist");
            return 0;
        }

        return convert(source, file, format, requiredParser);
    }

    private int convert(S source, File original, String requiredExtension, CodecResourceReadWriter.DynamicOpsParser<?> requiredFormatParser) {
        //if it is a directory, try to convert all files inside
        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        if(original.isDirectory()) {
            try {
                Files.walkFileTree(original.toPath(), new FileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        total.addAndGet(1);
                        File original = file.toFile();
                        success.addAndGet(convertFile(source, original, getFile(original, requiredExtension), requiredFormatParser));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                ErrorUtil.sendStackTrace(new ResourceParseException("Fatal error while converting the resource", e), message -> sendError(source, message));
            }

            sendSuccess(source, "Converted " + success.get() + "/" + total.get() + " files");
            return success.get();
        //if it is a file, try to convert it
        } else if(original.isFile()) {
            return convertFile(source, original, getFile(original, requiredExtension), requiredFormatParser);
        } else {
            sendError(source, "File is not a directory or a file");
            return 0;
        }
    }

    private File getFile(File original, String requiredExtension) {
        return new File(original.getParent(), Util.removeFileExtension(original.getName()) + "." + requiredExtension);
    }

    private int convertFile(S source, File original, File destination, CodecResourceReadWriter.DynamicOpsParser<?> requiredFormatParser) {
        try {
            if (original.equals(destination)) {
                sendSuccess(source, "File " + original.getName() + " is already in the requested format");
                return 1;
            }

            if (!destination.exists()) {
                if (!destination.createNewFile()) {
                    sendError(source, "Failed to create file" + original.getName());
                    return 0;
                }
            }

            String originalExtension = Util.getFileExtension(original.getName());
            CodecResourceReadWriter.DynamicOpsParser<?> originalParser = CodecResourceReadWriter.getParser(originalExtension);
            if (originalParser == null) {
                sendError(source, "Cannot convert the file " + original.getName() + " since no parser is registered for extension " + originalExtension);
                return 0;
            }

            FileReader reader = new FileReader(original);
            FileWriter writer = new FileWriter(destination);
            originalParser.convertTo(requiredFormatParser, reader, writer);
            reader.close();
            writer.close();
            sendSuccess(source, "File " + original.getName() + " converted to " + destination.getName());
            Files.delete(original.toPath());
            return 1;
        } catch (Exception e) {
            ErrorUtil.sendStackTrace(new ResourceParseException("Error while converting the file " + original.getName(), e), message -> sendError(source, message));
            return 0;
        }
    }

    public void handleError(S source, Exception error) {
        ErrorUtil.sendStackTrace(error, message -> sendError(source, message));
    }

    protected void sendSuccess(S source, String message) {
        this.feedbackSender.accept(source, Text.literal(message).formatted(Formatting.GREEN));
    }

    protected void sendError(S source, String error) {
        this.feedbackSender.accept(source, Text.literal(error).formatted(Formatting.RED));
    }
}
