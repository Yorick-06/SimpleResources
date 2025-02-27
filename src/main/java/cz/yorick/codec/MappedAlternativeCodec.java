package cz.yorick.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MappedAlternativeCodec<O> implements Codec<O> {
    private final Codec<O> decoderCodec;
    private final List<Entry<O, ?>> options;

    /**
     * If you are using this directly, keep in mind that the decoder does not
     * automatically get added to the list of options
     * */
    public MappedAlternativeCodec(Codec<O> decoderCodec, List<Entry<O, ?>> options) {
        this.decoderCodec = decoderCodec;
        this.options = options;
    }

    @Override
    public <T> DataResult<Pair<O, T>> decode(DynamicOps<T> ops, T input) {
        List<String> errors = new ArrayList<>();
        for (Entry<O, ?> entry : this.options) {
            DataResult<Pair<O, T>> result = entry.parse(ops, input);
            if(result.ifError(error -> errors.add(error.message())).isSuccess()) {
                return result;
            }
        }

        return DataResult.error(() -> "All possible options failed: " + String.join(" | ", errors));
    }

    @Override
    public <T> DataResult<T> encode(O input, DynamicOps<T> ops, T prefix) {
        return this.decoderCodec.encode(input, ops, prefix);
    }

    public record Entry<O, A>(Codec<A> codec, Function<A, O> converter) {
        public <T> DataResult<Pair<O, T>> parse(DynamicOps<T> ops, T input) {
            return this.codec.decode(ops, input).map(pair -> pair.mapFirst(this.converter));
        }
    }
}
