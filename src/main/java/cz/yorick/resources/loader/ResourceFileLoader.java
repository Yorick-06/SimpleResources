package cz.yorick.resources.loader;

import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceReadWriter;
import cz.yorick.resources.ResourceParseException;
import cz.yorick.resources.Util;
import cz.yorick.resources.type.SimpleResource;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ResourceFileLoader<T> implements SimpleResource.Loader<T> {
    private final Supplier<T> defaultFactory;
    private final ResourceReadWriter<T> readWriter;
    public ResourceFileLoader(Supplier<T> defaultFactory, ResourceReadWriter<T> readWriter) {
        this.defaultFactory = defaultFactory;
        this.readWriter = readWriter;
    }

    @Override
    public T load(Path path, Consumer<ResourceParseException> errorHandler) {
        try {
            File file = getFile(path);
            //a new file got created
            if(file == null) {
                T defaultValue = this.defaultFactory.get();
                //use getFile, this time it won't return null
                File newFile = getFile(path);
                //noinspection DataFlowIssue
                this.readWriter.write(Util.getFileExtensionOrThrow(newFile.getName()), new FileWriter(newFile), defaultValue);
                SimpleResourcesCommon.LOGGER.info("Wrote default data to file " + newFile);
                return defaultValue;
            }

            return this.readWriter.read(Util.getFileExtensionOrThrow(Util.pathToString(path)), new FileReader(file));
        } catch (Exception e) {
            errorHandler.accept(new ResourceParseException("Error while loading the file " + SimpleResource.Loader.getRelativePath(path), e));
            return this.defaultFactory.get();
        }
    }

    private static File getFile(Path path) throws IOException {
        File file = path.toFile();
        if(file.exists()) {
            if(!file.isFile()) {
                throw new FileAlreadyExistsException("File found in the specified location " + SimpleResource.Loader.getRelativePath(path) + " but it is not a normal file!");
            }

            return file;
        }

        if(!file.getParentFile().exists()) {
            SimpleResourcesCommon.LOGGER.info("File " + SimpleResource.Loader.getRelativePath(path) + " is missing parents, creating...");
            if(!file.getParentFile().mkdirs()) {
                throw new IOException("Could not crate the parent directories for file " + SimpleResource.Loader.getRelativePath(path));
            }
        }

        if(!file.createNewFile()) {
            throw new IOException("Could not create the file " + SimpleResource.Loader.getRelativePath(path));
        }

        SimpleResourcesCommon.LOGGER.info("File missing, created new file " + file);
        return null;
    }

    @Override
    public Identifier getValidatedId(Identifier id) {
        //gets the extension or null
        String fileExtension = Util.getFileExtension(id.getPath());
        //if the file extension is null, the file format is not specified so add .json
        if(fileExtension == null) {
            return id.withSuffixedPath(".json");
        }

        //if a format is specified, return the id unchanged
        return id;
    }
}
