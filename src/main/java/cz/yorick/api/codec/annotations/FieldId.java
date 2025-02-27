package cz.yorick.api.codec.annotations;

import cz.yorick.api.codec.ClassFieldsCodec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark fields which should be serialized under a different id than their declared name by {@link ClassFieldsCodec}
 * */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldId {
    /**
     * @return The id under which this field should be serialized
     * */
    String id();
}
