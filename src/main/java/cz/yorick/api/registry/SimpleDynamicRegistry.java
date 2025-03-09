package cz.yorick.api.registry;

import com.mojang.serialization.Codec;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.SimpleResources;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Disables registering values manually, but allows
 * reloading the registry with a map of new values
 * */
public class SimpleDynamicRegistry<K, V> extends SimpleRegistry<K, V> {
    public SimpleDynamicRegistry(Identifier registryId, Codec<K> keyCodec) {
        super(registryId, keyCodec);
    }

    public void reload(Map<K, V> newValues) {
        this.registry.clear();
        this.keyRegistry.clear();
        newValues.forEach(super::register);
        SimpleResourcesCommon.LOGGER.info("Reloaded the registry " + getRegistryId());
    }

    @Override
    public void register(K key, V value) {
        throw new UnsupportedOperationException("Cannot call .register() on a dynamic registry! Values can only be modified when reloading with the .reload() method!");
    }

    /**
     * Creates a data pack resource and wraps it in a dynamic registry.
     * Any time the resource gets reloaded, the registries values get updated
     * */
    public static <T> SimpleDynamicRegistry<Identifier, T> ofDatapackResource(Identifier id, Codec<T> codec, Identifier... dependencies) {
        SimpleDynamicRegistry<Identifier, T> dynamicRegistry = new SimpleDynamicRegistry<>(id, Identifier.CODEC);
        SimpleResources.datapackResource(id, codec, dynamicRegistry::reload, dependencies);
        return dynamicRegistry;
    }

    /**
     * Creates a resource pack resource and wraps it in a dynamic registry.
     * Any time the resource gets reloaded, the registries values get updated
     * */
    public static <T> SimpleDynamicRegistry<Identifier, T> ofResourcepackResource(Identifier id, Codec<T> codec, Identifier... dependencies) {
        SimpleDynamicRegistry<Identifier, T> dynamicRegistry = new SimpleDynamicRegistry<>(id, Identifier.CODEC);
        SimpleResources.resourcepackResource(id, codec, dynamicRegistry::reload, dependencies);
        return dynamicRegistry;
    }
}
