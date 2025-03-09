package cz.yorick.api;

import com.mojang.serialization.DynamicOps;

import java.io.Reader;
import java.io.Writer;

public interface FileTypeInitializer<T> {
    T read(Reader reader) throws Exception;
    void write(Writer writer, T data) throws Exception;
    String getExtension();
    DynamicOps<T> getOps();
}
