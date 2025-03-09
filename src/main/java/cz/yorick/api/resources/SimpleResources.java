package cz.yorick.api.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import cz.yorick.resources.loader.CodecResourceReadWriter;
import cz.yorick.resources.loader.ResourceFileLoader;
import cz.yorick.resources.loader.ResourceTreeLoader;
import cz.yorick.resources.type.MinecraftResource;
import cz.yorick.resources.type.SimpleReloadableResource;
import cz.yorick.resources.type.SimpleResource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Main class used for creating resources, can be called from the client initializer
 * to create client-only resources which will not get created on a dedicated server
 * and can use client-only classes.
 * */
public interface SimpleResources {
    /**
     * Overload of {@link SimpleResources#config(Identifier, Supplier, ResourceReadWriter)}, which takes in
     * a codec for serializing/deserializing the class.
     * */
    static <T> T config(Identifier configId, Supplier<T> defaultFactory, Codec<T> codec) {
        return config(configId, defaultFactory, new CodecResourceReadWriter<>(codec, extension -> true));
    }

    /**
     * The simplest resource which serializes a class with the provided codec into a file
     * @param <T> Your config class
     * @param configId The identifier of the config, the namespace should be the mod id, the path should be the name, for subdirectories use "/" in the path as the separator.
     *                 If no file extension is present the default json format is used, if you specify a custom file extension, make sure it's registered by {@link SimpleResources#registerOps(String, DynamicOps, Function, BiConsumer)}
     * @param defaultFactory The factory for creating a new instance of your class
     * @param readWriter The ResourceReadWriter used for reading/writing the resource into the file
     * @return The config value
     * */
    static <T> T config(Identifier configId, Supplier<T> defaultFactory, ResourceReadWriter<T> readWriter) {
        return new SimpleResource<>(configId, new ResourceFileLoader<>(defaultFactory, readWriter)).getLoadedValue();
    }

    /**
     * Overload of {@link SimpleResources#reloadableConfig(Identifier, Supplier, Codec, Consumer)}, but without a reload listener
     * */
    static <T> ReloadableResourceKey<T> reloadableConfig(Identifier configId, Supplier<T> defaultFactory, Codec<T> codec) {
        return reloadableConfig(configId, defaultFactory, codec, newValue -> {});
    }

    /**
     * Overload of {@link SimpleResources#reloadableConfig(Identifier, Supplier, ResourceReadWriter, Consumer)}, which takes in
     * a codec for serializing/deserializing the class
     * */
    static <T> ReloadableResourceKey<T> reloadableConfig(Identifier configId, Supplier<T> defaultFactory, Codec<T> codec, Consumer<T> reloadListener) {
        return reloadableConfig(configId, defaultFactory, new CodecResourceReadWriter<>(codec, extension -> true), reloadListener);
    }

    /**
     * Same as {@link SimpleResources#config(Identifier, Supplier, ResourceReadWriter)}, but allows reloading at runtime.
     * Because of this it also takes in a reload listener and returns a key since the config class instance can change
     * @param reloadListener Invoked after the config gets reloaded
     * @return The key used to access the currently loaded value
     * */
    static <T> ReloadableResourceKey<T> reloadableConfig(Identifier configId, Supplier<T> defaultFactory, ResourceReadWriter<T> readWriter, Consumer<T> reloadListener) {
        return new SimpleReloadableResource<>(configId, new ResourceFileLoader<>(defaultFactory, readWriter), reloadListener);
    }

    /**
     * Overload of {@link SimpleResources#resourceTree(Identifier, ResourceReadWriter)}, which takes in
     * a codec for serializing/deserializing the class
     * */
    static <T> Map<String, T> resourceTree(Identifier resourceId, Codec<T> codec) {
        return resourceTree(resourceId, new CodecResourceReadWriter<>(codec, extension -> true));
    }

    /**
     * The location specified is not going to
     * be a file, but a directory
     * <p>
     * While parsing the config goes through the
     * directory and all subdirectories parsing
     * every file
     * <p>
     * Returns the result as a map of:
     *  <p>
     *  KEY: the relative path as a string using "/" as the file separator
     *  <p>
     *  VALUE: the parsed value from the file
     * <p>
     * If a file fails to parse it will be skipped,
     * but parsing of other files will continue
     * unless a fatal error is encountered
     * @param <T> Your resource class
     * @param resourceId The identifier of the resource, the namespace should be your mod id, the path should be the name, for subdirectories use "/" in the path as the separator
     * @param readWriter The ResourceReadWriter used for reading/writing the resource into the file
     * @return The resource values
     * */
    static <T> Map<String, T> resourceTree(Identifier resourceId, ResourceReadWriter<T> readWriter) {
        return new SimpleResource<>(resourceId, new ResourceTreeLoader<>(readWriter)).getLoadedValue();
    }

    /**
     * Overload of {@link SimpleResources#reloadableResourceTree(Identifier, Codec, Consumer)}, but without a reload listener
     * */
    static <T> ReloadableResourceKey<Map<String, T>> reloadableResourceTree(Identifier resourceId, Codec<T> codec) {
        return reloadableResourceTree(resourceId, codec, newValues -> {});
    }

    /**
     * Overload of {@link SimpleResources#reloadableResourceTree(Identifier, ResourceReadWriter, Consumer)}, which takes in
     * a codec for serializing/deserializing the class
     * */
    static <T> ReloadableResourceKey<Map<String, T>> reloadableResourceTree(Identifier resourceId, Codec<T> codec, Consumer<Map<String, T>> reloadListener) {
        return reloadableResourceTree(resourceId, new CodecResourceReadWriter<>(codec, extension -> true), reloadListener);
    }

    /**
     * Same as {@link SimpleResources#resourceTree(Identifier, ResourceReadWriter)}, but allows reloading at runtime.
     * Because of this it also takes in a reload listener and returns a key since the config class instance can change
     * @param reloadListener Invoked after the config gets reloaded
     * @return The key used to access the currently loaded value
     * */
    static <T> ReloadableResourceKey<Map<String, T>> reloadableResourceTree(Identifier resourceId, ResourceReadWriter<T> readWriter, Consumer<Map<String, T>> reloadListener) {
        return new SimpleReloadableResource<>(resourceId, new ResourceTreeLoader<>(readWriter), reloadListener);
    }

    /**
     * Overload of {@link SimpleResources#datapackResource(Identifier, Codec, Consumer, Identifier[])}, but without a reload listener
     * */
    static<T> ResourceKey<Map<Identifier, T>> datapackResource(Identifier resourceId, Codec<T> codec, Identifier... dependencies) {
        return datapackResource(resourceId, codec, newValues -> {}, dependencies);
    }

    /**
     * Overload of {@link SimpleResources#datapackResource(Identifier, ResourceReadWriter, Consumer, Identifier[])}, which takes in
     * a codec for serializing/deserializing the class
     * */
    static<T> ResourceKey<Map<Identifier, T>> datapackResource(Identifier resourceId, Codec<T> codec, Consumer<Map<Identifier, T>> reloadListener, Identifier... dependencies) {
        return datapackResource(resourceId, new CodecResourceReadWriter<>(codec, extension -> true), reloadListener, dependencies);
    }

    /**
     * Creates a new data pack resource using fabrics api {@link net.fabricmc.fabric.api.resource.ResourceManagerHelper}
     * <p>
     * Can be specified by any data pack
     * <p>
     * Gets reloaded when data packs are reloaded (server start, the /reload command, joining singleplayer worlds)
     * <p>
     * Values get returned as a map of:
     *  <p>
     *  KEY: an identifier with a namespace specified in the data pack and a path representing its location
     *  <p>
     *  namespace/resource_name/file_name.extension becomes namespace:file_name.extension
     *  namespace/resource_name/directory/file_name.extension becomes namespace:directory/file_name.extension
     *  <p>
     *  VALUE: the parsed value from the file
     * @param <T> Your resource class
     * @param resourceId The identifier of the resource, the namespace should be the mod id, the path should be the resource name (something like minecraft's "advancement", "recipe" or "tags")
     * @param readWriter The ResourceReadWriter used for reading/writing the resource into the file
     * @param reloadListener Invoked after this data pack resource has been reloaded
     * @param dependencies A list of fabric resources which need to be loaded before this resource can be loaded
     * @return The key used to access the currently loaded values
     * */
    static<T> ResourceKey<Map<Identifier, T>> datapackResource(Identifier resourceId, ResourceReadWriter<T> readWriter, Consumer<Map<Identifier, T>> reloadListener, Identifier... dependencies) {
        return new MinecraftResource<>(resourceId, readWriter, ResourceType.SERVER_DATA, reloadListener, dependencies);
    }

    /**
     * Overload of {@link SimpleResources#resourcepackResource(Identifier, Codec, Consumer, Identifier[])}, but without a reload listener
     * */
    static<T> ResourceKey<Map<Identifier, T>> resourcepackResource(Identifier resourceId, Codec<T> codec, Identifier... dependencies) {
        return resourcepackResource(resourceId, codec, newValues -> {}, dependencies);
    }

    /**
     * Overload of {@link SimpleResources#resourcepackResource(Identifier, ResourceReadWriter, Consumer, Identifier[])}, which takes in
     * a codec for serializing/deserializing the class
     * */
    static<T> ResourceKey<Map<Identifier, T>> resourcepackResource(Identifier resourceId, Codec<T> codec, Consumer<Map<Identifier, T>> reloadListener, Identifier... dependencies) {
        return resourcepackResource(resourceId, new CodecResourceReadWriter<>(codec, extension -> true), reloadListener, dependencies);
    }

    /**
     * Same as {@link SimpleResources#datapackResource(Identifier, ResourceReadWriter, Consumer, Identifier[])}, but instead of loading from a data pack it loads from a resource pack.
     * That means it also gets reloaded when resource packs are reloaded and not when data packs are reloaded
     * */
    static<T> ResourceKey<Map<Identifier, T>> resourcepackResource(Identifier resourceId, ResourceReadWriter<T> readWriter, Consumer<Map<Identifier, T>> reloadListener, Identifier... dependencies) {
        return new MinecraftResource<>(resourceId, readWriter, ResourceType.CLIENT_RESOURCES, reloadListener, dependencies);
    }
}
