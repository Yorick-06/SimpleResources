package cz.yorick.resources.loader;

import com.google.common.collect.ImmutableMap;
import cz.yorick.api.resources.ResourceReadWriter;
import cz.yorick.resources.ResourceParseException;
import cz.yorick.resources.Util;
import cz.yorick.resources.type.SimpleResource;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ResourceTreeLoader<T> implements SimpleResource.Loader<Map<String, T>> {
    private final ResourceFileLoader<T> fileLoader;
    private final Predicate<String> shouldStripExtension;
    public ResourceTreeLoader(ResourceReadWriter<T> readWriter) {
        this.fileLoader = new ResourceFileLoader<>(() -> null, readWriter);
        this.shouldStripExtension = readWriter::shouldStripFileExtension;
    }

    @Override
    public Map<String, T> load(Path path, Consumer<ResourceParseException> errorHandler) {
        //if the directory is missing, try to create it including the parents
        File file = path.toFile();
        if(!file.exists() && !path.toFile().mkdirs()) {
            errorHandler.accept(new ResourceParseException("Failed to create the directory and parent directories at " + SimpleResource.Loader.getRelativePath(path)));
            return ImmutableMap.of();
        }

        HashMap<String, T> results = new HashMap<>();
        try {
            Files.walkFileTree(path, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    T loadedValue = ResourceTreeLoader.this.fileLoader.load(file, errorHandler);
                    //default factory returns null, which means parsing failed and error notification
                    //was handled by the file loader
                    if(loadedValue == null) {
                        return FileVisitResult.CONTINUE;
                    }

                    //converts D:/server/config/namespace/resource_name/file.extension -> file.extension
                    //converts D:/server/config/namespace/resource_name/directory/file.extension -> directory/file.extension
                    Path relativePath = path.relativize(file);
                    String path = Util.pathToString(relativePath);
                    if(ResourceTreeLoader.this.shouldStripExtension.test(Util.getFileExtension(path))) {
                        path = Util.removeFileExtension(path);
                    }

                    results.put(Util.removeFileExtension(path), loadedValue);
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
        } catch (IOException e) {
            errorHandler.accept(new ResourceParseException("Fatal error occurred while loading directory " + SimpleResource.Loader.getRelativePath(path) + " returning only partial result", e));
        }

        return ImmutableMap.copyOf(results);
    }

    @Override
    public Identifier getValidatedId(Identifier id) {
        return id;
    }
}
