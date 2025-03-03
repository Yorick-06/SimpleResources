package cz.yorick.api.resources.ops;

import java.io.Writer;

public interface OpsWriter<T> {
    void write(Writer writer, T data) throws Exception;
}
