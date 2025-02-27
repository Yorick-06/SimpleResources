package cz.yorick.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.dynamic.Codecs;

import java.util.Arrays;

public class EnumCodec {
    public static <E extends Enum<E>> Codec<E> of(Class<E> enumClass) {
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

    public static <E extends Enum<E>> Codec<E> caseConverting(Class<E> enumClass) {
        String validValues = "[" + String.join(", ", Arrays.stream(enumClass.getEnumConstants()).map(value -> value.name().toLowerCase()).toList()) + "]";
        return Codecs.NON_EMPTY_STRING.comapFlatMap(
                string -> {
                    try {
                        return DataResult.success(Enum.valueOf(enumClass, string.toUpperCase()));
                    } catch (Exception e) {
                        return DataResult.error(() -> "The id '" + string.toUpperCase() + "' does not represent a valid value, valid values: " + validValues);
                    }
                },
                value -> value.name().toLowerCase()
        );
    }
}
