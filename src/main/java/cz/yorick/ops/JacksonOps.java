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
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JacksonOps implements DynamicOps<JsonNode> {
    public static JacksonOps INSTANCE = new JacksonOps();

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
        if(!input.isBoolean()) {
            return DataResult.error(() -> "No a boolean: " + input);
        }

        return DataResult.success(input.booleanValue());
    }

    @Override
    public JsonNode createBoolean(boolean value) {
        return JsonNodeFactory.instance.booleanNode(value);
    }

    @Override
    public DataResult<Number> getNumberValue(JsonNode input) {
        if(input.isNumber()) {
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
        if(input.isTextual()) {
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
        if(!list.isArray()) {
            return DataResult.error(() -> "Not a list: " + list);
        }

        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        result.addAll((ArrayNode)list);
        result.add(value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<JsonNode> mergeToMap(JsonNode map, JsonNode key, JsonNode value) {
        if (!key.isTextual()) {
            return DataResult.error(() -> "Map key is not a string: " + key);
        }

        if(map != null) {
            if (!map.isObject()) {
                return DataResult.error(() -> "Not a map: " + map);
            }

            ((ObjectNode) map).set(key.asText(), value);
            return DataResult.success(map);
        }

        ObjectNode newMap = JsonNodeFactory.instance.objectNode();
        newMap.set(key.textValue(), value);
        return DataResult.success(newMap);
    }

    @Override
    public DataResult<Stream<Pair<JsonNode, JsonNode>>> getMapValues(JsonNode input) {
        if(!input.isObject()) {
            return DataResult.error(() -> "Not a map: " + input);
        }

        return DataResult.success(asStream(input.fields()).map(entry -> Pair.of(JsonNodeFactory.instance.textNode(entry.getKey()), entry.getValue())));
    }

    @Override
    public JsonNode createMap(Stream<Pair<JsonNode, JsonNode>> map) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        map.forEach(pair -> node.set(pair.getFirst().asText(), pair.getSecond()));
        return node;
    }

    @Override
    public DataResult<Stream<JsonNode>> getStream(JsonNode input) {
        if(!input.isArray()) {
            return DataResult.error(() -> "Not a list: " + input);
        }

        return DataResult.success(asStream(input.iterator()));
    }

    @Override
    public JsonNode createList(Stream<JsonNode> input) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        input.forEach(array::add);
        return array;
    }

    @Override
    public JsonNode remove(JsonNode input, String key) {
        if(input instanceof ObjectNode node) {
            node.remove(key);
            return node;
        }

        throw new IllegalArgumentException("Not a map: " + input);
    }

    private static <T> Stream<T> asStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
