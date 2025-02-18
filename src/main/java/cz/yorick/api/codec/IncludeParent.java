package cz.yorick.api.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark a class which should also include its parents fields while being serialized by {@link ClassFieldsCodec}, parent is also checked for this annotation
 * */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IncludeParent {
}
