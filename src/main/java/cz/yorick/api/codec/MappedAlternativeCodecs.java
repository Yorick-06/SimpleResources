package cz.yorick.api.codec;

import com.mojang.serialization.Codec;
import cz.yorick.codec.MappedAlternativeCodec;

import java.util.List;
import java.util.function.Function;

/**
 * Methods for constructing the MappedAlternativeCodec
 * This codec takes in one main codec which defines
 * its type and is used for serializing, and any amount
 * of codecs for deserialization with a converter to the
 * firstly specified value
 * */
public interface MappedAlternativeCodecs {
    static <O, A> MappedAlternativeCodec<O> of(Codec<O> codec, Codec<A> alternative, Function<A, O> converter) {
        return new MappedAlternativeCodec<>(codec, List.of(
                new MappedAlternativeCodec.Entry<>(codec, value -> value),
                new MappedAlternativeCodec.Entry<>(alternative, converter)
        ));
    }

    static <O, A1, A2> MappedAlternativeCodec<O> of(Codec<O> codec, Codec<A1> alternative1, Function<A1, O> converter1, Codec<A2> alternative2, Function<A2, O> converter2) {
        return new MappedAlternativeCodec<>(codec, List.of(
                new MappedAlternativeCodec.Entry<>(codec, value -> value),
                new MappedAlternativeCodec.Entry<>(alternative1, converter1),
                new MappedAlternativeCodec.Entry<>(alternative2, converter2)
        ));
    }

    static <O, A1, A2, A3> MappedAlternativeCodec<O> of(Codec<O> codec, Codec<A1> alternative1, Function<A1, O> converter1, Codec<A2> alternative2, Function<A2, O> converter2, Codec<A3> alternative3, Function<A3, O> converter3) {
        return new MappedAlternativeCodec<>(codec, List.of(
                new MappedAlternativeCodec.Entry<>(codec, value -> value),
                new MappedAlternativeCodec.Entry<>(alternative1, converter1),
                new MappedAlternativeCodec.Entry<>(alternative2, converter2),
                new MappedAlternativeCodec.Entry<>(alternative3, converter3)
        ));
    }

    static <O, A1, A2, A3, A4> MappedAlternativeCodec<O> of(Codec<O> codec, Codec<A1> alternative1, Function<A1, O> converter1, Codec<A2> alternative2, Function<A2, O> converter2, Codec<A3> alternative3, Function<A3, O> converter3, Codec<A4> alternative4, Function<A4, O> converter4) {
        return new MappedAlternativeCodec<>(codec, List.of(
                new MappedAlternativeCodec.Entry<>(codec, value -> value),
                new MappedAlternativeCodec.Entry<>(alternative1, converter1),
                new MappedAlternativeCodec.Entry<>(alternative2, converter2),
                new MappedAlternativeCodec.Entry<>(alternative3, converter3),
                new MappedAlternativeCodec.Entry<>(alternative4, converter4)
        ));
    }

    static <O, A1, A2, A3, A4, A5> MappedAlternativeCodec<O> of(Codec<O> codec, Codec<A1> alternative1, Function<A1, O> converter1, Codec<A2> alternative2, Function<A2, O> converter2, Codec<A3> alternative3, Function<A3, O> converter3, Codec<A4> alternative4, Function<A4, O> converter4, Codec<A5> alternative5, Function<A5, O> converter5) {
        return new MappedAlternativeCodec<>(codec, List.of(
                new MappedAlternativeCodec.Entry<>(codec, value -> value),
                new MappedAlternativeCodec.Entry<>(alternative1, converter1),
                new MappedAlternativeCodec.Entry<>(alternative2, converter2),
                new MappedAlternativeCodec.Entry<>(alternative3, converter3),
                new MappedAlternativeCodec.Entry<>(alternative4, converter4),
                new MappedAlternativeCodec.Entry<>(alternative5, converter5)
        ));
    }

    static <O, A1, A2, A3, A4, A5, A6> MappedAlternativeCodec<O> of(Codec<O> codec, Codec<A1> alternative1, Function<A1, O> converter1, Codec<A2> alternative2, Function<A2, O> converter2, Codec<A3> alternative3, Function<A3, O> converter3, Codec<A4> alternative4, Function<A4, O> converter4, Codec<A5> alternative5, Function<A5, O> converter5, Codec<A6> alternative6, Function<A6, O> converter6) {
        return new MappedAlternativeCodec<>(codec, List.of(
                new MappedAlternativeCodec.Entry<>(codec, value -> value),
                new MappedAlternativeCodec.Entry<>(alternative1, converter1),
                new MappedAlternativeCodec.Entry<>(alternative2, converter2),
                new MappedAlternativeCodec.Entry<>(alternative3, converter3),
                new MappedAlternativeCodec.Entry<>(alternative4, converter4),
                new MappedAlternativeCodec.Entry<>(alternative5, converter5),
                new MappedAlternativeCodec.Entry<>(alternative6, converter6)
        ));
    }
}
