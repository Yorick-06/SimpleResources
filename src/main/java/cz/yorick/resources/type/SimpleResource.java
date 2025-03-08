package cz.yorick.resources.type;

import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceReadWriter;
import cz.yorick.resources.ResourceParseException;
import cz.yorick.resources.Util;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class SimpleResource<T> {
    private final Path path;
    private final String name;
    private final Loader<T> loader;
    private T loadedValue;
    public SimpleResource(Identifier configId, Loader<T> loader) {
        Path filePath = Path.of(configId.getNamespace());
        String[] path = configId.getPath().split("/");
        for (int i = 0; i < path.length - 1; i++) {
            filePath = filePath.resolve(path[i]);
        }

        this.path = FabricLoader.getInstance().getConfigDir().resolve(filePath);
        this.loader = loader;
        this.name = path[path.length -1];
        Util.registerConfig(configId, this);
        this.load(error -> SimpleResourcesCommon.LOGGER.error("Error while loading the resource " + configId, error));
    }

    public T getLoadedValue() {
        return this.loadedValue;
    }

    protected void load(Consumer<ResourceParseException> errorHandler) {
        this.loadedValue = this.loader.load(this.loader.getFilePath(this.path, this.name), errorHandler);
    }

    public ResourceReadWriter<?> getReadWriter() {
        return this.loader.getReadWriter();
    }

    public File getFile() {
        return this.loader.getFilePath(this.path, this.name).toFile();
    }

    public interface Loader<T> {
        T load(Path path, Consumer<ResourceParseException> errorHandler);
        Path getFilePath(Path path, String name);
        ResourceReadWriter<?> getReadWriter();
        static Path getRelativePath(Path fullPath) {
            return FabricLoader.getInstance().getConfigDir().relativize(fullPath);
        }
    }
}
