package cz.yorick.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.codec.Optional;
import cz.yorick.api.codec.*;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.dynamic.Codecs;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ClassFieldsReflectionCodec<C, T extends C> {
    private static final ImmutableMap<Class<?>, Codec<?>> DEFAULT_CODECS = ImmutableMap.<Class<?>, Codec<?>>builder()
            //basic java classes
            .put(boolean.class, Codec.BOOL)
            .put(Boolean.class, Codec.BOOL)
            .put(byte.class, Codec.BYTE)
            .put(Byte.class, Codec.BYTE)
            .put(int.class, Codec.INT)
            .put(Integer.class, Codec.INT)
            .put(float.class, Codec.FLOAT)
            .put(Float.class, Codec.FLOAT)
            .put(double.class, Codec.DOUBLE)
            .put(Double.class, Codec.DOUBLE)
            .put(long.class, Codec.LONG)
            .put(Long.class, Codec.LONG)
            .put(String.class, Codec.STRING)
            //minecraft's registries
            .put(Item.class, Registries.ITEM.getCodec())
            .put(EntityType.class, Registries.ENTITY_TYPE.getCodec())
            .put(Block.class, Registries.BLOCK.getCodec())
            //extra minecraft classes
            .put(Identifier.class, Identifier.CODEC)
            .put(ItemStack.class, ItemStack.CODEC)
            .build();
    private final Supplier<T> defaultFactory;
    private final Map<Class<?>, Codec<?>> extraCodecs;
    private final Map<String, Codec<?>> codecOverwrites;
    private final LinkedHashMap<String, SerializableField> classFields;
    private final Function<T, DataResult<T>> postProcessor;
    ClassFieldsReflectionCodec(Class<?> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor) {
        if(clazz.isRecord()) {
            throw new IllegalArgumentException("ClassFieldsCodec does not accept records since they are immutable, if the class has to be a record you need to write your own codec");
        }

        this.defaultFactory = defaultFactory;
        this.extraCodecs = ImmutableMap.copyOf(extraCodecs);
        this.codecOverwrites = ImmutableMap.copyOf(codecOverwrites);
        this.classFields = getSerializableFields(clazz);
        this.postProcessor = postProcessor;
    }

    private LinkedHashMap<String, SerializableField> getSerializableFields(Class<?> clazz) {
        LinkedHashMap<String, SerializableField> allFields = getDeclaredSerializableFields(clazz);
        if(clazz.getAnnotation(IncludeParent.class) != null) {
            getSerializableFields(clazz.getSuperclass()).forEach((id, field) -> {
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

    private LinkedHashMap<String, SerializableField> getDeclaredSerializableFields(Class<?> clazz) {
       return Arrays.stream(clazz.getDeclaredFields())
                .filter(this::shouldSerialize)
                .peek(field -> field.setAccessible(true))
                .map(this::getFieldEntry)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (field1, field2) -> {
                    throw new RuntimeException("Fields with matching ids found in class '" + clazz.getName() + "', " +
                            "field '" + field1.field().getName() + "' and '" + field2.field().getName() + "' have the same id!");
                }, LinkedHashMap::new));
    }

    private boolean shouldSerialize(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && field.getAnnotation(Ignore.class) == null;
    }

    private Pair<String, SerializableField> getFieldEntry(Field field) {
        String fieldId = getFieldId(field);
        boolean required = field.getAnnotation(Optional.class) == null;

        Codec<?> overwriteCodec = this.codecOverwrites.get(fieldId);
        if(overwriteCodec != null) {
            return Pair.of(fieldId, new SerializableField(field, overwriteCodec, required));
        }

        Class<?> fieldClass = field.getType();
        Codec<?> codec = this.extraCodecs.get(fieldClass);
        if(codec != null) {
            return Pair.of(fieldId, new SerializableField(field, codec, required));
        }

        Codec<?> defaultCodec = DEFAULT_CODECS.get(fieldClass);
        if(defaultCodec != null) {
            return Pair.of(fieldId, new SerializableField(field, defaultCodec, required));
        }

        //try to create a generic enum codec
        if(fieldClass.isEnum()) {
            return Pair.of(fieldId, new SerializableField(field, enumCodec(fieldClass.asSubclass(Enum.class)), required));
        }

        throw new IllegalArgumentException("Could not get codec for field '" + field.getName() + "' no codec registered for class " + fieldClass.getName() + " or field id '" + fieldId + "'");
    }

    private<E extends Enum<E>> Codec<E> enumCodec(Class<E> enumClass) {
        String validValues = "[" + String.join(", ", Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList()) + "]";
        return Codecs.NON_EMPTY_STRING.comapFlatMap(
                string -> {
                    try {
                        return DataResult.success(Enum.valueOf(enumClass, string));
                    } catch (Exception e) {
                        return DataResult.error(() -> "The id '" + string + "' does not represent a valid value, valid values: " + validValues);
                    }
                },
                Enum::name
        );
    }

    private String getFieldId(Field field) {
        FieldId fieldId = field.getAnnotation(FieldId.class);
        if(fieldId == null) {
            return field.getName();
        }

        if(fieldId.id().equals("")) {
            throw new IllegalArgumentException("Field '" + field.getName() +"' is marked with @FieldId(id = \"\"), the name of the field cannot be empty!");
        }

        return fieldId.id();
    }

    private Codec<?> getFieldCodec(String fieldName) {
        SerializableField field = this.classFields.get(fieldName);
        if(field == null) {
            //just return the unit codec, error will get thrown later by createWithValues()
            return Unit.CODEC;
        }

        return field.codec();
    }

    private DataResult<T> createWithValues(Map<String, Object> values) {
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

    private DataResult<Map<String, Object>> getValues(T instance) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, SerializableField> entry : this.classFields.entrySet()) {
            Object value = entry.getValue().get(instance);
            if(value == null) {
                return DataResult.error(() -> "Cannot serialize the field '" + entry.getKey() + "' because its value is null");
            }

            values.put(entry.getKey(), value);
        }

        return DataResult.success(values);
    }

    public static<C, T extends C> Codec<T> of(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor) {
        ClassFieldsReflectionCodec<C, T> fieldsCodec = new ClassFieldsReflectionCodec<>(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor);
        Codec<Map<String, Object>> mapCodec = Codec.dispatchedMap(Codecs.NON_EMPTY_STRING, fieldsCodec::getFieldCodec);
        return mapCodec.flatXmap(fieldsCodec::createWithValues, fieldsCodec::getValues);
    }

    private record SerializableField(Field field, Codec<?> codec, boolean required) {
        private Object get(Object instance) {
            try {
                return this.field.get(instance);
            } catch (IllegalAccessException e) {
                SimpleResourcesCommon.LOGGER.error("Could not retrieve the value of config field '" + this.field.getName() + "'", e);
                return null;
            }
        }

        private void set(Object instance, Object value) {
            try {
                this.field.set(instance, value);
            } catch (Exception e) {
                SimpleResourcesCommon.LOGGER.error("Could not assign value to the config field '" + this.field.getName() + "'", e);
            }
        }
    }

    public static class Builder<C, T extends C> implements ClassFieldsCodec.Builder<C, T> {
        private final Class<C> clazz;
        private final  Supplier<T> defaultFactory;
        private final Map<Class<?>, Codec<?>> extraCodecs = new HashMap<>();
        private final Map<String, Codec<?>> codecOverwrites = new HashMap<>();
        private Function<T, DataResult<T>> postProcessor = null;
        public Builder(Class<C> clazz, Supplier<T> defaultFactory) {
            this.clazz = clazz;
            this.defaultFactory = defaultFactory;
        }

        @Override
        public Builder<C, T> withCodec(Codec<?> codec, Class<?> clazz) throws IllegalArgumentException {
            if(this.extraCodecs.containsKey(clazz)) {
                throw new IllegalArgumentException("Attempted to register multiple codecs for the class '" + clazz.getName() +"'");
            }
            this.extraCodecs.put(clazz, codec);
            return this;
        }

        @Override
        public Builder<C, T> withCodec(Codec<?> codec, String... fieldIds) throws IllegalArgumentException {
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
        public Builder<C, T> postProcessor(Function<T, DataResult<T>> postProcessor) {
            if(this.postProcessor != null) {
                throw new IllegalStateException("Attempted to register a post processor while a post processor is already registered");
            }

            this.postProcessor = postProcessor;
            return this;
        }

        @Override
        public Codec<T> build() {
            return ClassFieldsReflectionCodec.of(this.clazz, this.defaultFactory, this.extraCodecs, this.codecOverwrites, this.postProcessor != null ? this.postProcessor : DataResult::success);
        }
    }
}
