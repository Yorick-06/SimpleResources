package cz.yorick.api.resources;

import java.util.function.Consumer;

/**
 * Adds the {@link ReloadableResourceKey#reload(Consumer)} method
 * @param <T> The class of your resource
 * */
public interface ReloadableResourceKey<T> extends ResourceKey<T> {
    /**
     * Reloads the resource (reads and parses the file)
     * @param errorHandler Each time an error gets thrown while parsing the resource, this handler gets fired
     * */
    void reload(Consumer<Exception> errorHandler);
}
