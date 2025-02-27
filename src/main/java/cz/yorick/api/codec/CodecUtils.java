package cz.yorick.api.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import cz.yorick.codec.DelegatedDispatchedMapCodec;
import cz.yorick.codec.EnumCodec;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface CodecUtils {
    /**
     * Creates a simple codec which converts an enum to its name and back
     * */
     static <T extends Enum<T>> Codec<T> enumCodec(Class<T> enumClass) {
         return EnumCodec.of(enumClass);
     }

    /**
     * Converts the enum to lower case for serialization and back to
     * upper case before calling Enum.valueOf()
     * */
    static <T extends Enum<T>> Codec<T> caseConvertingEnum(Class<T> enumClass) {
        return EnumCodec.caseConverting(enumClass);
    }

    /**
     * Same functionality as {@link com.mojang.serialization.Codec#unboundedMap(Codec, Codec)}, but returns a {@link com.mojang.serialization.MapCodec} instead of a {@link com.mojang.serialization.Codec}
     * this means that it can be used by {@link com.mojang.serialization.Codec#dispatch} and as a part of other map codecs
     * @param keys Specifies which keys to parse, keys missing from this set will be ignored
     * @param keyCodec Codec for the keys of the map
     * @param valueCodec Codec for the values of the map
     * */
    static <K, V> MapCodec<Map<K, V>> unboundedMap(Set<String> keys, Codec<K> keyCodec, Codec<V> valueCodec) {
        return dispatchedMap(keys, keyCodec, key -> valueCodec);
    }

    /**
     * Same functionality as {@link com.mojang.serialization.Codec#dispatchedMap(Codec, Function)}, but returns a {@link com.mojang.serialization.MapCodec} instead of a {@link com.mojang.serialization.Codec}
     * this means that it can be used by {@link com.mojang.serialization.Codec#dispatch} and as a part of other map codecs
     * @param keys Specifies which keys to parse, keys missing from this set will be ignored
     * @param keyCodec Codec for the keys of the map
     * @param keyToCodec Function which receives a key and returns a valid codec
     * */
    static <K, V> MapCodec<Map<K, V>> dispatchedMap(Set<String> keys, Codec<K> keyCodec, Function<K, Codec<? extends V>> keyToCodec) {
        return new DelegatedDispatchedMapCodec<>(keys, keyCodec, keyToCodec);
    }
}
