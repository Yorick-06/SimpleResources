package cz.yorick.resources.type;

import cz.yorick.SimpleResourcesCommon;
import cz.yorick.resources.ResourceParseException;
import cz.yorick.resources.Util;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.function.Consumer;

public class SimpleResource<T> {
    private final Path path;
    private final Loader<T> loader;
    private T loadedValue;
    public SimpleResource(Identifier configId, Loader<T> loader) {
        Identifier validatedId = loader.getValidatedId(configId);
        this.path = FabricLoader.getInstance().getConfigDir().resolve(toPath(validatedId));
        this.loader = loader;
        Util.registerConfig(validatedId, this);
        this.load(error -> SimpleResourcesCommon.LOGGER.error("Error while loading the resource " + validatedId, error));
    }

    public T getLoadedValue() {
        return this.loadedValue;
    }

    protected void load(Consumer<ResourceParseException> errorHandler) {
        this.loadedValue = this.loader.load(this.path, errorHandler);
    }

    private static Path toPath(Identifier id) {
        Path filePath = Path.of(id.getNamespace());
        String[] path = id.getPath().split("/");
        for (String pathPart : path) {
            filePath = filePath.resolve(pathPart);
        }

        return filePath;
    }

    public interface Loader<T> {
        T load(Path path, Consumer<ResourceParseException> errorHandler);
        Identifier getValidatedId(Identifier id);
        static Path getRelativePath(Path fullPath) {
            return FabricLoader.getInstance().getConfigDir().relativize(fullPath);
        }
    }
}
