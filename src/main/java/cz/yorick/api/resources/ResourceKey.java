package cz.yorick.api.resources;

/**
 * Since the actual resource class instance changes after every reload,
 * this class can be used to always retrieve the current instance
 * @param <T> The class of your resource
 * */
public interface ResourceKey<T> {
    /**
     * Gets the current resource class instance
     * @return The current resource class instance
     * */
    T getValue();
}
