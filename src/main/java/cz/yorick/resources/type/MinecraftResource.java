package cz.yorick.resources.type;

import com.google.common.collect.ImmutableMap;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceKey;
import cz.yorick.api.resources.ResourceReadWriter;
import cz.yorick.api.resources.ResourceUtil;
import cz.yorick.resources.Util;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MinecraftResource<T> implements ResourceKey<Map<Identifier, T>> {
    private final Consumer<Map<Identifier, T>> reloadListener;
    private Map<Identifier, T> loadedValue = ImmutableMap.of();
    public MinecraftResource(Identifier id, ResourceReadWriter<T> readWriter, ResourceType resourceType, Consumer<Map<Identifier, T>> reloadListener) {
        this.reloadListener = reloadListener;
        ResourceManagerHelper.get(resourceType).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id;
            }

            @Override
            public void reload(ResourceManager manager) {
                parse(id.getPath(), manager, readWriter);
            }
        });
    }

    @Override
    public Map<Identifier, T> getValue() {
        return this.loadedValue;
    }

    private void parse(String resourceName, ResourceManager resourceManager, ResourceReadWriter<T> readWriter) {
        HashMap<Identifier, T> results = new HashMap<>();
        for(Map.Entry<Identifier, Resource> entry : resourceManager.findResources(resourceName, identifier -> true).entrySet()) {
            try {
                Identifier originalKey = entry.getKey();
                String fileExtension = Util.getFileExtensionOrThrow(originalKey.getPath());
                T parsed = readWriter.read(fileExtension, entry.getValue().getReader());
                //converts
                //namespace:resource_name/file_name.extension -> namespace:file_name.extension
                //namespace:resource_name/directory/file_name.extension -> namespace:directory/file_name.extension
                Identifier loadedKey = originalKey.withPath(originalKey.getPath().substring(resourceName.length() + 1));
                if(readWriter.shouldStripFileExtension(fileExtension)) {
                    loadedKey = ResourceUtil.removeFileExtension(loadedKey);
                }

                if (results.containsKey(loadedKey)) {
                    throw new IllegalStateException("Duplicate data file ignored with ID " + loadedKey + " (path " + originalKey + ")");
                }

                results.put(loadedKey, parsed);
            } catch (Exception e) {
                SimpleResourcesCommon.LOGGER.error("Error occurred while loading resource: " + entry.getKey().toString(), e);
            }
        }

        this.loadedValue = ImmutableMap.copyOf(results);
        this.reloadListener.accept(this.loadedValue);
    }
}
