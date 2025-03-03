package cz.yorick.api.resources;

import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.io.Writer;

/**
 * Allows for reading/writing custom resource types
 * */
public interface ResourceReadWriter<T> {
    /**
     * Reads the file
     * @param fileExtension The extension of the file
     * @param reader The reader of the file
     * @return The value parsed from this file
     * @throws Exception If an exception occurred while parsing the file
     * */
    default T read(String fileExtension, Reader reader) throws Exception {
        return read(fileExtension, reader, null);
    }

    T read(String fileExtension, Reader reader, @Nullable RegistryWrapper.WrapperLookup lookup) throws Exception;
    /**
     * Writes to the file
     * @param fileExtension The extension of the file
     * @param writer The writer of the file
     * @throws Exception If an exception occurred while writing the file
     * */
    void write(String fileExtension, Writer writer, T data) throws Exception;
    /**
     * If this file extension should be stripped when inserting into
     * the loaded resources map. Does nothing for single-file configs
     * */
    boolean shouldStripFileExtension(String fileExtension);
}
