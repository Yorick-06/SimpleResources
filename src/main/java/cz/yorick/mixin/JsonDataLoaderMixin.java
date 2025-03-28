package cz.yorick.mixin;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceUtil;
import cz.yorick.resources.ErrorUtil;
import cz.yorick.resources.loader.CodecResourceReadWriter;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(JsonDataLoader.class)
public class JsonDataLoaderMixin {
    @Inject(method = "load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/RegistryKey;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V", at = @At("TAIL"))
    private static <T> void load(ResourceManager manager, RegistryKey<? extends Registry<T>> registryRef, DynamicOps<JsonElement> ops, Codec<T> codec, Map<Identifier, T> results, CallbackInfo info) {
        if(ops instanceof RegistryOps<JsonElement> registryOps) {
            CodecResourceReadWriter.getExtraOps().forEach((extension, parser) -> loadCustom(manager, new ResourceFinder(RegistryKeys.getPath(registryRef), "." + extension), parser.registryOps(registryOps), codec, results));
        } else {
            throw new IllegalArgumentException("JsonDataLoaderMixin received a non-registry codec! should never happen!");
        }
    }

    private static <T> void loadCustom(ResourceManager resourceManager, ResourceFinder finder, CodecResourceReadWriter.DynamicOpsParser<?> parser, Codec<T> codec, Map<Identifier, T> results) {
        for(Map.Entry<Identifier, Resource> entry : finder.findResources(resourceManager).entrySet()) {
            try {
                T parsed = parser.parse(entry.getValue().getReader(), codec);
                Identifier loadedKey = finder.toResourceId(entry.getKey());
                if (results.containsKey(loadedKey) && !SimpleResourcesCommon.getPreferredFormat().equals(ResourceUtil.getFileExtension(entry.getKey()))) {
                    ErrorUtil.reloadWarning("Duplicate data file ignored with ID " + loadedKey + " (path " + entry.getKey() + ")");
                    continue;
                }

                results.put(loadedKey, parsed);
            } catch (Throwable e) {
                ErrorUtil.reloadError("Error occurred while loading resource: " + entry.getKey().toString(), e);
            }
        }
    }

    //inject to both possible LOGGER.error()
    @WrapOperation(
            method = {
                    "load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/resource/ResourceFinder;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
                    //the lambda which handles parse errors
                    "method_63567(Lnet/minecraft/util/Identifier;Lnet/minecraft/util/Identifier;Lcom/mojang/serialization/DataResult$Error;)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V",
                    remap = false
            )
    )
    private static void sendError(Logger instance, String message, Object[] params, Operation<Void> original) {
        //log the message
        original.call(instance, message, params);
        try {
            ErrorUtil.loggerError(message, params);
        } catch (Throwable e) {
            ErrorUtil.reloadError("Failed to send a reload error to chat - original error is logged by minecraft.", e);
        }
    }
}
