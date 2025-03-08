package cz.yorick.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JacksonOps implements DynamicOps<JsonNode> {
    public static final JacksonOps INSTANCE = new JacksonOps();

    @Override
    public JsonNode empty() {
        return JsonNodeFactory.instance.nullNode();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, JsonNode input) {
        if (input.isObject()) {
            return convertMap(outOps, input);
        }
        if (input.isArray()) {
            return convertList(outOps, input);
        }
        if (input.isNull()) {
            return outOps.empty();
        }

        if (input.isTextual()) {
            return outOps.createString(input.textValue());
        }
        if (input.isBoolean()) {
            return outOps.createBoolean(input.booleanValue());
        }

        if(input.isNumber()) {
            return outOps.createNumeric(input.numberValue());
        }

        throw new IllegalArgumentException("Unknown type: " + input);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(JsonNode input) {
        if(input != null && input.isBoolean()) {
            return DataResult.success(input.booleanValue());
        }

        return DataResult.error(() -> "No a boolean: " + input);
    }

    @Override
    public JsonNode createBoolean(boolean value) {
        return JsonNodeFactory.instance.booleanNode(value);
    }

    @Override
    public DataResult<Number> getNumberValue(JsonNode input) {
        if(input != null && input.isNumber()) {
            return DataResult.success(input.numberValue());
        }
        return DataResult.error(() -> "Not a number " + input);
    }

    @Override
    public JsonNode createNumeric(Number number) {
        if(number instanceof Byte b) {
            return JsonNodeFactory.instance.numberNode(b);
        } else if(number instanceof Short s) {
            return JsonNodeFactory.instance.numberNode(s);
        } else if(number instanceof Integer i) {
            return JsonNodeFactory.instance.numberNode(i);
        } else if(number instanceof Long l) {
            return JsonNodeFactory.instance.numberNode(l);
        } else if(number instanceof BigInteger b) {
            return JsonNodeFactory.instance.numberNode(b);
        } else if(number instanceof Float f) {
            return JsonNodeFactory.instance.numberNode(f);
        } else if(number instanceof Double d) {
            return JsonNodeFactory.instance.numberNode(d);
        } else if(number instanceof BigDecimal b) {
            return JsonNodeFactory.instance.numberNode(b);
        }

        throw new IllegalArgumentException("Unknown number class: " + (number != null ? number.getClass() : null));
    }

    @Override
    public DataResult<String> getStringValue(JsonNode input) {
        if(input != null && input.isTextual()) {
            return DataResult.success(input.textValue());
        }

        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public JsonNode createString(String value) {
        return JsonNodeFactory.instance.textNode(value);
    }

    @Override
    public DataResult<JsonNode> mergeToList(JsonNode list, JsonNode value) {
        if(list != null && list.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            result.addAll((ArrayNode)list);
            result.add(value);
            return DataResult.success(result);
        }

        return DataResult.error(() -> "Not a list: " + list);
    }

    @Override
    public DataResult<JsonNode> mergeToMap(JsonNode map, JsonNode key, JsonNode value) {
        if((map == null || !map.isObject()) && map != empty()) {
            return DataResult.error(() -> "Not a map: " + map);
        }

        if(key == null || !key.isTextual()) {
            return DataResult.error(() -> "Map key is not a string: " + key);
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        if(map != empty()) {
            asStream(map.fields()).forEach(entry -> result.set(entry.getKey(), entry.getValue()));
        }
        result.set(key.asText(), value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<JsonNode, JsonNode>>> getMapValues(JsonNode input) {
        if(input != null && input.isObject()) {
            return DataResult.success(asStream(input.fields()).map(entry -> Pair.of(JsonNodeFactory.instance.textNode(entry.getKey()), entry.getValue())));

        }

        return DataResult.error(() -> "Not a map: " + input);
    }

    @Override
    public JsonNode createMap(Stream<Pair<JsonNode, JsonNode>> map) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        map.forEach(pair -> node.set(pair.getFirst().asText(), pair.getSecond()));
        return node;
    }

    @Override
    public DataResult<Stream<JsonNode>> getStream(JsonNode input) {
        if(input != null && input.isArray()) {
            return DataResult.success(asStream(input.iterator()));
        }

        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public JsonNode createList(Stream<JsonNode> input) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        input.forEach(array::add);
        return array;
    }

    @Override
    public JsonNode remove(JsonNode input, String key) {
        if(input != null && input.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            asStream(input.fields()).filter(entry -> !Objects.equals(entry.getKey(), key)).forEach(entry -> result.set(entry.getKey(), entry.getValue()));
            return result;
        }

        return input;
    }

    private static <T> Stream<T> asStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
