package cz.yorick.resources;

import cz.yorick.SimpleResourcesCommon;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;
import java.util.function.Consumer;

public class ErrorUtil {
    private static ServerCommandSource reloadingSource;
    public static void startReload(ServerCommandSource source) {
        reloadingSource = source;
    }

    public static void reloadError(String message, Throwable error) {
        SimpleResourcesCommon.LOGGER.error(message, error);
        broadcastReloadError(message, error);
    }

    public static void broadcastReloadError(String message, Throwable error) {
        if(reloadingSource != null && SimpleResourcesCommon.shouldBroadcastErrors()) {
            sendStackTrace(new ResourceParseException(message, error), errorLine -> reloadingSource.sendError(Text.literal(errorLine)));
        }
    }

    public static void sendStackTrace(Throwable error, Consumer<String> feedbackConsumer) {
        feedbackConsumer.accept(error.getMessage());
        Throwable throwable = error.getCause();
        while (throwable != null) {
            feedbackConsumer.accept("Caused by " + throwable.getClass().getName() + ":");
            feedbackConsumer.accept(throwable.getMessage());
            throwable = throwable.getCause();
        }
    }

    public static void reloadWarning(String message) {
        SimpleResourcesCommon.LOGGER.warn(message);
        if(reloadingSource != null && SimpleResourcesCommon.shouldBroadcastErrors()) {
            reloadingSource.sendMessage(Text.literal(message).formatted(Formatting.GOLD));
        }
    }

    public static void loggerError(String string, Object[] args) {
        if(args[args.length - 1] instanceof Throwable throwable) {
            FormattingTuple result = MessageFormatter.arrayFormat(string, Arrays.copyOf(args, args.length - 1));
            broadcastReloadError(result.getMessage(), throwable);
            return;
        }

        FormattingTuple result = MessageFormatter.arrayFormat(string, args);
        broadcastReloadError(result.getMessage(), null);
    }
}
