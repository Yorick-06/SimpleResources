package cz.yorick.api.resources.ops;

import java.io.Reader;

public interface OpsReader<T> {
    T read(Reader reader) throws Exception;
}
