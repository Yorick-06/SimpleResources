package cz.yorick.codec.reflection;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.codec.annotations.FieldId;
import cz.yorick.api.codec.annotations.OptionalField;
import cz.yorick.codec.DelegatedDispatchedMapCodec;
import cz.yorick.codec.EnumCodec;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class FieldsReflectionCodec<C, T extends C> {
    public static final ImmutableMap<Class<?>, Codec<?>> DEFAULT_CODECS = ImmutableMap.<Class<?>, Codec<?>>builder()
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

    protected final Supplier<T> defaultFactory;
    protected final Map<Class<?>, Codec<?>> extraCodecs;
    protected final Map<String, Codec<?>> codecOverwrites;
    protected final Function<T, DataResult<T>> postProcessor;
    protected final LinkedHashMap<String, ClassFieldsReflectionCodec.SerializableField> classFields;
    FieldsReflectionCodec(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        this.defaultFactory = defaultFactory;
        this.extraCodecs = ImmutableMap.copyOf(extraCodecs);
        this.codecOverwrites = ImmutableMap.copyOf(codecOverwrites);
        this.postProcessor = postProcessor;
        this.classFields = getClassFields(clazz, convertNames);
    }

    //converts camelCase to snake_case
    public static String convertName(String name) {
        StringBuilder builder = new StringBuilder();
        for (char ch : name.toCharArray()) {
            if(Character.isUpperCase(ch)) {
                builder.append("_");
                builder.append(Character.toLowerCase(ch));
                continue;
            }

            builder.append(ch);
        }

        return builder.toString();
    }

    protected DataResult<Map<String, Object>> getValues(T instance) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, SerializableField> entry : this.classFields.entrySet()) {
            DataResult<Object> value = entry.getValue().get(instance);
            if(value.isError()) {
                return DataResult.error(() -> "Cannot serialize the field '" + entry.getKey() + "' - check the log for details");
            }

            //if the value is null, don't add it
            if(value.getOrThrow() == null) {
                continue;
            }

            values.put(entry.getKey(), value.getOrThrow());
        }

        return DataResult.success(values);
    }

    protected Pair<String, SerializableField> getFieldEntry(Field field, boolean convertNames) {
        String fieldId = getFieldId(field, convertNames);
        boolean required = field.getAnnotation(OptionalField.class) == null;

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
            return Pair.of(fieldId, new SerializableField(field, EnumCodec.of(fieldClass.asSubclass(Enum.class)), required));
        }

        throw new IllegalArgumentException("Could not get codec for field '" + field.getName() + "' no codec registered for class " + fieldClass.getName() + " or field id '" + fieldId + "'");
    }

    private String getFieldId(Field field, boolean convertNames) {
        FieldId fieldId = field.getAnnotation(FieldId.class);
        if(fieldId == null) {
            return convertNames ? convertName(field.getName()) : field.getName();
        }

        if(fieldId.id().isEmpty()) {
            throw new IllegalArgumentException("Field '" + field.getName() +"' is marked with @FieldId(id = \"\"), the name of the field cannot be empty!");
        }

        return fieldId.id();
    }

    protected Codec<?> getFieldCodec(String fieldName) {
        SerializableField field = this.classFields.get(fieldName);
        if(field == null) {
            throw new IllegalArgumentException("Attempted to get a codec for an unknown field '" + fieldName + "' - this should be filtered out by DelegatedDispatchedMapCodec and never happen!");
        }

        return field.codec();
    }

    protected abstract LinkedHashMap<String, SerializableField> getClassFields(Class<C> clazz, boolean convertNames);
    protected abstract DataResult<T> createWithValues(Map<String, Object> values);

    protected record SerializableField(Field field, Codec<?> codec, boolean required) {
        protected DataResult<Object> get(Object instance) {
            try {
                return DataResult.success(this.field.get(instance));
            } catch (IllegalAccessException e) {
                SimpleResourcesCommon.LOGGER.error("Could not retrieve the value of config field '" + this.field.getName() + "'", e);
                return DataResult.error(() -> "Could not retrieve the value of config field '" + this.field.getName() + "' - Check the log for more details");
            }
        }

        protected void set(Object instance, Object value) {
            try {
                this.field.set(instance, value);
            } catch (Exception e) {
                SimpleResourcesCommon.LOGGER.error("Could not assign value to the config field '" + this.field.getName() + "'", e);
            }
        }
    }

    public static<C, T extends C> Codec<T> of(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        return ofMap(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames).codec();
    }

    public static<C, T extends C> MapCodec<T> ofMap(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        FieldsReflectionCodec<C, T> reflectionCodec = createFor(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames);
        MapCodec<Map<String, Object>> objects = new DelegatedDispatchedMapCodec<>(reflectionCodec.classFields.keySet(), Codecs.NON_EMPTY_STRING, reflectionCodec::getFieldCodec);
        return objects.flatXmap(reflectionCodec::createWithValues, reflectionCodec::getValues);
    }

    private static<C, T extends C> FieldsReflectionCodec<C, T> createFor(Class<C> clazz, Supplier<T> defaultFactory, Map<Class<?>, Codec<?>> extraCodecs, Map<String, Codec<?>> codecOverwrites, Function<T, DataResult<T>> postProcessor, boolean convertNames) {
        if(clazz.isRecord()) {
            return new RecordFieldsReflectionCodec<>(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames);
        } else {
            return new ClassFieldsReflectionCodec<>(clazz, defaultFactory, extraCodecs, codecOverwrites, postProcessor, convertNames);
        }
    }
}
