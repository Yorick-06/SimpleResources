package cz.yorick.api.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import cz.yorick.codec.ClassFieldsReflectionCodec;
import cz.yorick.resources.Util;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Creates a codec from fields specified in the class using reflection
 *  <ul>
 *   <li>The class cannot be a record</li>
 *   <li>Ignores static fields and fields marked with {@link Ignore}</li>
 *   <li>Codecs for some classes are provided by default, but you can add your own and override the defaults with {@link ClassFieldsCodec.Builder#withCodec(Codec, Class)}</li>
 *   <li>If different codecs are needed for fields of the same type, {@link ClassFieldsCodec.Builder#withCodec(Codec, String...)}
 *   can be used. The string is the fields name, but you can mark a field with {@link FieldId} to change its id</li>
 *   <li>If a field does not need to be specified in the loaded data, you can use {@link Optional}</li>
 *   <li>If the fields of the classes parent class should also get serialized, mark the class with {@link IncludeParent}</li>
 * </ul>
 * */
public interface ClassFieldsCodec {
    /**
     * Creates the codec with no extra options, the class needs to have a default, no-argument constructor,
     * if your class needs to have a constructor with parameters, use {@link ClassFieldsCodec#of(Class, Supplier)}
     * @param clazz Your class
     * @return The codec created from the fields in the class
     * */
    static <T> Codec<T> of(Class<T> clazz) {
        return of(clazz, Util.factoryFor(clazz));
    }

    /**
     * Creates the codec with no extra options
     * @param clazz Your class
     * @param defaultFactory The default factory for your class
     * @return The codec created from the fields in the class
     * */
    static<T> Codec<T> of(Class<T> clazz, Supplier<T> defaultFactory) {
        return ClassFieldsReflectionCodec.of(clazz, defaultFactory, Map.of(), Map.of(), DataResult::success);
    }

    /**
     * Creates a builder for the codec which allows you to customize the codec, the class needs to have a default, no-argument constructor,
     * if your class needs to have a constructor with parameters, use {@link ClassFieldsCodec#builder(Class, Supplier)}
     * @param clazz Your class
     * @return The codec created from the fields in the class
     * */
    static<T> Builder<T, T> builder(Class<T> clazz) {
        return builder(clazz, Util.factoryFor(clazz));
    }

    /**
     * Creates a builder for the codec which allows you to customize the codec
     * @param clazz Your class
     * @param defaultFactory The default factory for your class
     * @return The codec created from the fields in the class
     * */
    static<C, T extends C> Builder<C, T> builder(Class<C> clazz, Supplier<T> defaultFactory) {
        return new ClassFieldsReflectionCodec.Builder<>(clazz, defaultFactory);
    }

    interface Builder<C, T extends C> {
        /**
         * Adds a codec for a class - if a codec for the class is specified by default, the one specified by this method takes priority
         * @param codec The codec to add
         * @param clazz The class which you want to add the codec for
         * @return The builder
         * @throws IllegalArgumentException If a codec is already specified for the class
         * */
        Builder<C, T> withCodec(Codec<?> codec, Class<?> clazz) throws IllegalArgumentException;

        /**
         * @param codec The codec of the class
         * @param fieldIds The field ids for which to register the codec
         * @return The builder
         * @throws IllegalArgumentException If a codec is already specified for the codecId
         * */
        Builder<C, T> withCodec(Codec<?> codec, String... fieldIds) throws IllegalArgumentException;

        /**
         * Adds a post processor to modify the class, check for conflicting values or do something else
         * @param postProcessor Invoked after all values are set for the class
         * */
        Builder<C, T> postProcessor(Function<T, DataResult<T>> postProcessor);
        /**
         * Builds the codec
         * */
        Codec<T> build();
    }
}
