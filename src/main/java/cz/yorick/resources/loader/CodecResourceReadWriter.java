package cz.yorick.resources.loader;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceReadWriter;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class CodecResourceReadWriter<T> implements ResourceReadWriter<T> {
    private final Codec<T> codec;
    private final Predicate<String> shouldStripFileExtension;
    public CodecResourceReadWriter(Codec<T> codec, Predicate<String> shouldStripFileExtension) {
        this.codec = codec;
        this.shouldStripFileExtension = shouldStripFileExtension;
    }

    @Override
    public T read(String fileExtension, Reader reader, @Nullable RegistryWrapper.WrapperLookup wrapperLookup) throws Exception {
        DynamicOpsParser<?> parser = dynamicOpsRegistry.get(fileExtension);
        if(parser == null) {
            throw new IllegalArgumentException("File cannot be parsed - no dynamic ops registered for file extension '." + fileExtension + "', if you wish to use custom extensions register them with SimpleResources#registerOps");
        }

        if(wrapperLookup != null) {
            parser = parser.withLookup(wrapperLookup);
        }

        return parser.parse(reader, this.codec);
    }

    @Override
    public void write(String fileExtension, Writer writer, T data) throws Exception {
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
    private static final HashMap<String, DynamicOpsParser<?>> extraOps = new HashMap<>();
    static {
        dynamicOpsRegistry.put("json", new DynamicOpsParser<>(JsonOps.INSTANCE, JsonParser::parseReader, CodecResourceReadWriter::writeJson));
    }
    public static <T> void registerOps(String fileExtension, DynamicOps<T> ops, OpsReader<T> readerParser, OpsWriter<T> writer) {
        if(dynamicOpsRegistry.containsKey(fileExtension)) {
            SimpleResourcesCommon.LOGGER.warn("Attempted to register duplicate DynamicOps for file extension '." + fileExtension + "' ignoring register call - keeping original");
            return;
        }

        dynamicOpsRegistry.put(fileExtension, new DynamicOpsParser<>(ops, readerParser, writer));
        extraOps.put(fileExtension, new DynamicOpsParser<>(ops, readerParser, writer));
    }

    public static Map<String, DynamicOpsParser<?>> getExtraOps() {
        return ImmutableMap.copyOf(extraOps);
    }

    public static Set<String> getRegisteredExtensions() {
        return new HashSet<>(dynamicOpsRegistry.keySet());
    }

    public static DynamicOpsParser<?> getParser(String extension) {
        return dynamicOpsRegistry.get(extension);
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

    public record DynamicOpsParser<T>(DynamicOps<T> ops, OpsReader<T> readerParser, OpsWriter<T> writer) {
        public <V> V parse(Reader reader, Codec<V> codec) throws Exception {
            return codec.parse(this.ops, this.readerParser.read(reader)).getOrThrow();
        }

        public <V> void write(Writer writer, V value, Codec<V> codec) throws Exception {
            DataResult<T> encodeResult = codec.encodeStart(this.ops, value);
            this.writer.write(writer, encodeResult.getOrThrow());
        }

        public DynamicOpsParser<T> registryOps(RegistryOps<?> registryOps) {
            return new DynamicOpsParser<>(registryOps.withDelegate(this.ops), this.readerParser, this.writer);
        }

        public DynamicOpsParser<T> withLookup(RegistryWrapper.WrapperLookup lookup) {
            return new DynamicOpsParser<>(lookup.getOps(this.ops), this.readerParser, this.writer);
        }

        public <T2> void convertTo(CodecResourceReadWriter.DynamicOpsParser<T2> other, Reader reader, Writer writer) throws Exception {
            T result = this.readerParser.read(reader);
            T2 converted = this.ops.convertTo(other.ops(), result);
            other.writer().write(writer, converted);
        }

        public <T2> T2 readAs(DynamicOps<T2> otherOps, Reader reader) throws Exception {
            return this.ops.convertTo(otherOps, this.readerParser.read(reader));
        }
    }

    public interface OpsReader<T> {
        T read(Reader reader) throws Exception;
    }

    public interface OpsWriter<T> {
        void write(Writer writer, T data) throws Exception;
    }
}
