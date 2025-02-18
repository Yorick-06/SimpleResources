package cz.yorick.api.resources;

import java.io.Reader;
import java.io.Writer;

public interface ResourceReadWriter<T> {
    T read(String fileExtension, Reader reader) throws Exception;
    void write(String fileExtension, Writer writer, T data) throws Exception;
}
