package cz.yorick.codec.reflection;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.codec.*;
import cz.yorick.api.codec.annotations.Ignore;
import cz.yorick.api.codec.annotations.IncludeParent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ClassFieldsReflectionCodec<C, T extends C> extends FieldsReflectionCodec<C, T> {
    ClassFieldsReflectionCodec(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        super(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames);
    }

    @Override
    protected LinkedHashMap<String, SerializableField> getClassFields(Class<C> clazz, boolean convertNames) {
        if(clazz.isRecord()) {
            throw new IllegalArgumentException("ClassFieldsReflectionCodec does not accept records since they are immutable, use RecordFieldsReflectionCodec instead (should be handled automatically if you are an api user)");
        }
        return getSerializableFields(clazz, convertNames);
    }

    private LinkedHashMap<String, SerializableField> getSerializableFields(Class<?> clazz, boolean convertNames) {
        LinkedHashMap<String, SerializableField> allFields = getDeclaredSerializableFields(clazz, convertNames);
        if(clazz.getAnnotation(IncludeParent.class) != null) {
            getSerializableFields(clazz.getSuperclass(), convertNames).forEach((id, field) -> {
                if (allFields.containsKey(id)) {
                    Field newField = field.field();
                    Field prevField = allFields.get(id).field();
                    throw new IllegalArgumentException("Duplicate field id '" + id + "' found!" +
                            " Field '" + newField.getName() + "' declared by class '" + newField.getDeclaringClass().getName() + "' has the same id as the previously specified" +
                            " field '" + prevField.getName() + "' declared by class '" + prevField.getDeclaringClass().getName() + "', either change one of the fields names or use the @FieldId or @Ignore annotation");
                }

                allFields.put(id, field);
            });
        }

        return allFields;
    }

    private LinkedHashMap<String, SerializableField> getDeclaredSerializableFields(Class<?> clazz, boolean convertNames) {
       return Arrays.stream(clazz.getDeclaredFields())
                .filter(this::shouldSerialize)
                .peek(field -> field.setAccessible(true))
                .map(field -> getFieldEntry(field, convertNames))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (field1, field2) -> {
                    throw new RuntimeException("Fields with matching ids found in class '" + clazz.getName() + "', " +
                            "field '" + field1.field().getName() + "' and '" + field2.field().getName() + "' have the same id!");
                }, LinkedHashMap::new));
    }

    private boolean shouldSerialize(Field field) {
        boolean shouldSerialize = !Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Ignore.class) == null;
        //final fields which are primitives or strings get replaced with their values at compile time
        if(shouldSerialize && Modifier.isFinal(field.getModifiers()) && (field.getType().isPrimitive() || field.getType().equals(String.class))) {
            SimpleResourcesCommon.LOGGER.warn("Field '" + field.getName() + "' declared in class '" + field.getDeclaringClass().getName() + "' is a compile-time constant, it got inlined by the compiler and will remain at its default value!");
        }
        return shouldSerialize;
    }

    @Override
    protected DataResult<T> createWithValues(Map<String, Object> values) {
        //get the missing keys by removing the received keys
        //from the field keys.
        //key set reflects its changes into the map so get a copy
        Set<String> missingKeys = new HashSet<>(this.classFields.keySet());
        missingKeys.removeAll(values.keySet());
        for (String missingKey : missingKeys) {
            //if the key is required, throw an exception
            if(this.classFields.get(missingKey).required()) {
                return DataResult.error(() -> "Missing a required key: '" + missingKey + "'");
            }
        }

        T instance = this.defaultFactory.get();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            SerializableField serializableField = this.classFields.get(entry.getKey());
            if(serializableField == null) {
                return DataResult.error(() -> "Key '" + entry.getKey() + "' does not represent a valid field!");
            }

            serializableField.set(instance, entry.getValue());
        }

        return this.postProcessor.apply(instance);
    }
}
