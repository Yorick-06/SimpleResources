package cz.yorick.api.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;

/**
 * This class is basically a BiMap which also provides a codec
 * which will serialize a value by getting its key and using
 * the key codec on it
 * */
public class SimpleRegistry<K, V> {
    protected final HashMap<K, V> registry = new HashMap<>();
    protected final HashMap<V, K> keyRegistry = new HashMap<>();
    private final Identifier registryId;
    private final Codec<V> codec;
    public SimpleRegistry(Identifier registryId, Codec<K> keyCodec) {
        this.registryId = registryId;
        this.codec = keyCodec.flatXmap(
                key -> {
                    V value = getOrNull(key);
                    if(value != null) {
                        return DataResult.success(value);
                    }
                    return DataResult.error(() -> "Invalid key for registry " + this.registryId + " '" + key + "'");
                },
                value -> {
                    K key = getIdOrNull(value);
                    if(key != null) {
                        return DataResult.success(key);
                    }
                    return DataResult.error(() -> "Invalid value for registry " + this.registryId + " '" + value + "'");
                }
        );
    }

    public List<K> getKeys() {
        return List.copyOf(this.registry.keySet());
    }

    public List<V> getValues() {
        return List.copyOf(this.keyRegistry.keySet());
    }

    public Identifier getRegistryId() {
        return this.registryId;
    }

    public V getOrNull(K key) {
        return registry.get(key);
    }

    public K getIdOrNull(V value) {
        return this.keyRegistry.get(value);
    }

    public void register(K key, V value) {
        if(this.registry.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate id in registry " + this.registryId + " '" + key + "'");
        }

        if(this.keyRegistry.containsKey(value)) {
            throw new IllegalArgumentException("Duplicate value in registry " + this.registryId + " '" + value + "'");
        }

        this.registry.put(key, value);
        this.keyRegistry.put(value, key);
    }

    public Codec<V> getCodec() {
        return this.codec;
    }
}
