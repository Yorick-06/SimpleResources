package cz.yorick.codec.reflection;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.codec.annotations.Ignore;
import cz.yorick.api.codec.annotations.OptionalField;
import cz.yorick.codec.DelegatedDispatchedMapCodec;
import net.minecraft.util.dynamic.Codecs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class RecordFieldsReflectionCodec<C, T extends C> extends FieldsReflectionCodec<C, T> {
    private final Constructor<T> canonicalConstructor;
    RecordFieldsReflectionCodec(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        super(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames);
        this.canonicalConstructor = getCanonicalConstructor(clazz);
    }

    private Constructor<T> getCanonicalConstructor(Class<C> recordClass) {
        if(this.defaultFactory != null && recordClass != this.defaultFactory.get().getClass()) {
            throw new IllegalArgumentException("For record reflection codecs the specified class and the class returned by the default constructor must be the same!");
        }

        try {
            //cast verified by the code above
            return (Constructor<T>)recordClass.getConstructor(
                    Arrays.stream(recordClass.getRecordComponents())
                            .map(RecordComponent::getType)
                            .toArray(Class<?>[]::new)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Record class is missing a canonical constructor (how?)", e);
        }
    }

    @Override
    protected LinkedHashMap<String, SerializableField> getClassFields(Class<C> clazz, boolean convertNames) {
        if(!clazz.isRecord()) {
            throw new IllegalArgumentException("RecordFieldsReflectionCodec only accepts records, use RecordFieldsReflectionCodec instead (should be handled automatically if you are an api user)");
        }

        LinkedHashMap<String, SerializableField> classFields = new LinkedHashMap<>();
        RecordComponent[] recordComponents = clazz.getRecordComponents();
        for (RecordComponent recordComponent : recordComponents) {
            Field field = getRecordField(clazz, recordComponent);

            Ignore ignore = field.getAnnotation(Ignore.class);
            if(ignore != null) {
                SimpleResourcesCommon.LOGGER.warn("@Ignore annotation present in a RecordFieldsReflectionCodec for class '" + clazz.getName() + "' on field '" + field.getName() + "', the annotation will have no effect!");
            }

            OptionalField optionalField = field.getAnnotation(OptionalField.class);
            if(optionalField != null && this.defaultFactory == null) {
                throw new IllegalStateException("@OptionalField annotation found in a RecordFieldsReflectionCodec on field '" + recordComponent.getName() + "', but this codec is missing a default constructor!");
            }

            Pair<String, SerializableField> recordEntry = getFieldEntry(field, convertNames);
            classFields.put(recordEntry.getFirst(), recordEntry.getSecond());
        }

        return classFields;
    }

    private Field getRecordField(Class<C> recordClass, RecordComponent component) {
        try {
            Field field = recordClass.getDeclaredField(component.getName());
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new IllegalStateException("Record class is missing a field but has a component with name: " + component.getName() + " (how?)");
        }
    }

    @Override
    public DataResult<T> createWithValues(Map<String, Object> values) {
        Object[] constructorParams = new Object[this.canonicalConstructor.getParameterCount()];
        T defaultInstance = null;
        List<String> errors = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, SerializableField> entry : this.classFields.entrySet()) {
            Object value = values.get(entry.getKey());
            if(value == null) {
                if(entry.getValue().required()) {
                    //try to create a default instance if missing
                    if(defaultInstance == null && this.defaultFactory != null) {
                        defaultInstance = this.defaultFactory.get();
                    }

                    //if creation failed, return an error
                    if(defaultInstance == null) {
                        return DataResult.error(() -> "Missing a required key: '" + entry.getKey() + "'");
                    }

                    //otherwise add an error and let the default get assigned
                    errors.add("Missing a required key: '" + entry.getKey() + "'");
                }

                //if the value is optional, copy it from a default instance - factory cannot be null when an
                //optional field is present, don't always create since defaultFactory can be null for records
                //without optional fields
                if(defaultInstance == null) {
                    defaultInstance = this.defaultFactory.get();
                }

                constructorParams[i] = entry.getValue().get(defaultInstance);
            }

            constructorParams[i] = value;
            i++;
        }

        try {
            T result = this.canonicalConstructor.newInstance(constructorParams);
            this.postProcessor.apply(result);
            //return as partial if errors were present
            if(!errors.isEmpty()) {
                return DataResult.error(() -> String.join(" | ", errors), result);
            }
            return this.postProcessor.apply(result);
        } catch (Exception e) {
            SimpleResourcesCommon.LOGGER.error("Could not run the constructor: ", e);
            return DataResult.error(() -> "Could not run the constructor - check the log for details");
        }
    }

    public static<C, T extends C> Codec<T> of(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        return ofMap(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames).codec();
    }

    public static<C, T extends C> MapCodec<T> ofMap(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        ClassFieldsReflectionCodec<C, T> fieldsCodec = new ClassFieldsReflectionCodec<>(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames);
        MapCodec<Map<String, Object>> objects = new DelegatedDispatchedMapCodec<>(fieldsCodec.classFields.keySet(), Codecs.NON_EMPTY_STRING, fieldsCodec::getFieldCodec);
        return objects.flatXmap(fieldsCodec::createWithValues, fieldsCodec::getValues);
    }
}
