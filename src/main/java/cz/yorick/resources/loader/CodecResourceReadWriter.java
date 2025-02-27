package cz.yorick.resources.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceReadWriter;
import net.minecraft.util.JsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CodecResourceReadWriter<T> implements ResourceReadWriter<T> {
    private final Codec<T> codec;
    private final Predicate<String> shouldStripFileExtension;
    public CodecResourceReadWriter(Codec<T> codec, Predicate<String> shouldStripFileExtension) {
        this.codec = codec;
        this.shouldStripFileExtension = shouldStripFileExtension;
    }

    @Override
    public T read(String fileExtension, Reader reader) {
        DynamicOpsParser<?> parser = dynamicOpsRegistry.get(fileExtension);
        if(parser == null) {
            throw new IllegalArgumentException("File cannot be parsed - no dynamic ops registered for file extension '." + fileExtension + "', if you wish to use custom extensions register them with SimpleResources#registerOps");
        }

        return parser.parse(reader, codec);
    }

    @Override
    public void write(String fileExtension, Writer writer, T data) {
        DynamicOpsParser<?> parser = dynamicOpsRegistry.get(fileExtension);
        if(parser == null) {
            throw new IllegalArgumentException("Cannot write to file - no dynamic ops registered for file extension '." + fileExtension + "', if you wish to use custom extensions register them with SimpleResources#registerOps");
        }

        parser.write(writer, data, this.codec);
    }

    @Override
    public boolean shouldStripFileExtension(String fileExtension) {
        return this.shouldStripFileExtension.test(fileExtension);
    }

    private static final HashMap<String, DynamicOpsParser<?>> dynamicOpsRegistry = new HashMap<>();
    static {
        registerOps("json", JsonOps.INSTANCE, JsonParser::parseReader, CodecResourceReadWriter::writeJson);
    }
    public static<T> void registerOps(String fileExtension, DynamicOps<T> ops, Function<Reader, T> readerParser, BiConsumer<Writer, T> writer) {
        if(dynamicOpsRegistry.containsKey(fileExtension)) {
            SimpleResourcesCommon.LOGGER.warn("Attempted to register duplicate DynamicOps for file extension '." + fileExtension + "' ignoring register call - keeping original");
            return;
        }

        dynamicOpsRegistry.put(fileExtension, new DynamicOpsParser<>(ops, readerParser, writer));
    }

    private static void writeJson(Writer writer, JsonElement data) {
        try {
            JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.setIndent("  ");
            jsonWriter.setSerializeNulls(false);
            JsonHelper.writeSorted(jsonWriter, data, (string1, string2) -> 0);
            jsonWriter.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while writing json to file", e);
        }
    }

    private record DynamicOpsParser<T>(DynamicOps<T> ops, Function<Reader, T> readerParser, BiConsumer<Writer, T> writer) {
        private<V> V parse(Reader reader, Codec<V> codec) {
            return codec.parse(this.ops, this.readerParser.apply(reader)).getOrThrow();
        }

        private<V> void write(Writer writer, V value, Codec<V> codec) {
            DataResult<T> encodeResult = codec.encodeStart(this.ops, value);
            this.writer.accept(writer, encodeResult.getOrThrow());
        }
    }
}
