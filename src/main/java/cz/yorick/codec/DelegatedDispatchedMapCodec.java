package cz.yorick.codec;

import com.mojang.serialization.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class DelegatedDispatchedMapCodec<K, V> extends MapCodec<Map<K, V>> {
    private final Set<String> keys;
    private final Codec<Map<K, V>> codec;
    public DelegatedDispatchedMapCodec(Set<String> keys, Codec<K> keyCodec, Function<K, Codec<? extends V>> keyToCodec) {
        this.keys = keys;
        this.codec = Codec.dispatchedMap(keyCodec, keyToCodec);
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return this.keys.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
        List<T> validKeys = keys(ops).toList();
        //get a map with only the valid keys
        T normalInput = ops.createMap(input.entries().filter(pair -> validKeys.contains(pair.getFirst())));
        return this.codec.parse(ops, normalInput);
    }

    @Override
    public <T> RecordBuilder<T> encode(Map<K, V> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        DataResult<T> result = this.codec.encodeStart(ops, input);
        if(result.isError()) {
            prefix.withErrorsFrom(result);
            return prefix;
        }

        DataResult<MapLike<T>> mapLike = ops.getMap(result.getOrThrow());
        if(mapLike.isError()) {
            prefix.withErrorsFrom(mapLike);
            return prefix;
        }

        mapLike.getOrThrow().entries().forEach(pair -> prefix.add(pair.getFirst(), pair.getSecond()));
        return prefix;
    }
}
