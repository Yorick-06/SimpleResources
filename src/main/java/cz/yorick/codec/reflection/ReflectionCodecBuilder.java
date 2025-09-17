package cz.yorick.codec.reflection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import cz.yorick.api.codec.ClassFieldsCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReflectionCodecBuilder<C, T extends C> implements ClassFieldsCodec.Builder<C, T> {
    private final Class<C> clazz;
    private final Supplier<T> defaultFactory;
    private final Map<Class<?>, Codec<?>> extraCodecs = new HashMap<>();
    private final Map<String, Codec<?>> codecOverwrites = new HashMap<>();
    private Function<T, DataResult<T>> postProcessor = DataResult::success;
    private boolean convertNames = false;
    public ReflectionCodecBuilder(Class<C> clazz, Supplier<T> defaultFactory) {
        this.clazz = clazz;
        this.defaultFactory = defaultFactory;
    }

    @Override
    public ClassFieldsCodec.Builder<C, T> withCodec(Codec<?> codec, Class<?> clazz) throws IllegalArgumentException {
        if(this.extraCodecs.containsKey(clazz)) {
            throw new IllegalArgumentException("Attempted to register multiple codecs for the class '" + clazz.getName() +"'");
        }
        this.extraCodecs.put(clazz, codec);
        return this;
    }

    @Override
    public ClassFieldsCodec.Builder<C, T> withCodec(Codec<?> codec, String... fieldIds) throws IllegalArgumentException {
        if(fieldIds.length == 0) {
            throw new IllegalArgumentException("Tried to register a codec for field ids, but did not specify any!");
        }

        for (String fieldId : fieldIds) {
            if(this.codecOverwrites.containsKey(fieldId)) {
                throw new IllegalArgumentException("Attempted to register a duplicate codec for field id '" + fieldId +"'");
            }
            this.codecOverwrites.put(fieldId, codec);
        }
        return this;
    }

    @Override
    public ClassFieldsCodec.Builder<C, T> postProcessor(Function<T, DataResult<T>> postProcessor) {
        if(this.postProcessor != null) {
            throw new IllegalStateException("Attempted to register a post processor while a post processor is already registered");
        }

        this.postProcessor = postProcessor;
        return this;
    }

    @Override
    public ClassFieldsCodec.Builder<C, T> convertNames() {
        this.convertNames = true;
        return this;
    }

    @Override
    public Codec<T> build() {
        return FieldsReflectionCodec.of(this.clazz, this.defaultFactory, this.extraCodecs, this.codecOverwrites, this.postProcessor, this.convertNames);
    }

    @Override
    public MapCodec<T> buildMap() {
        return FieldsReflectionCodec.ofMap(this.clazz, this.defaultFactory, this.extraCodecs, this.codecOverwrites, this.postProcessor, this.convertNames);
    }
}
