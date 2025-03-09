package cz.yorick.resources.type;

import com.google.common.collect.ImmutableMap;
import cz.yorick.SimpleResourcesCommon;
import cz.yorick.api.resources.ResourceKey;
import cz.yorick.api.resources.ResourceReadWriter;
import cz.yorick.api.resources.ResourceUtil;
import cz.yorick.resources.ErrorUtil;
import cz.yorick.resources.Util;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;

public class MinecraftResource<T> implements ResourceKey<Map<Identifier, T>> {
    private final Consumer<Map<Identifier, T>> reloadListener;
    private Map<Identifier, T> loadedValue = ImmutableMap.of();
    public MinecraftResource(Identifier id, ResourceReadWriter<T> readWriter, ResourceType resourceType, Consumer<Map<Identifier, T>> reloadListener, Identifier... dependencies) {
        this.reloadListener = reloadListener;
        //client resources cannot have the wrapper lookup
        if(resourceType == ResourceType.CLIENT_RESOURCES) {
            ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(getListener(id, readWriter, null, dependencies));
            return;
        }

        ResourceManagerHelper.get(resourceType).registerReloadListener(id, wrapperLookup -> getListener(id, readWriter, wrapperLookup, dependencies));
    }

    @Override
    public Map<Identifier, T> getValue() {
        return this.loadedValue;
    }

    private void parse(String resourceName, ResourceManager resourceManager, ResourceReadWriter<T> readWriter, RegistryWrapper.WrapperLookup wrapperLookup) {
        HashMap<Identifier, T> results = new HashMap<>();
        for(Map.Entry<Identifier, Resource> entry : resourceManager.findResources(resourceName, identifier -> true).entrySet()) {
            try {
                Identifier originalKey = entry.getKey();
                String fileExtension = Util.getFileExtensionOrThrow(originalKey.getPath());
                T parsed = readWriter.read(fileExtension, entry.getValue().getReader(), wrapperLookup);
                //converts
                //namespace:resource_name/file_name.extension -> namespace:file_name.extension
                //namespace:resource_name/directory/file_name.extension -> namespace:directory/file_name.extension
                Identifier loadedKey = originalKey.withPath(originalKey.getPath().substring(resourceName.length() + 1));
                if(readWriter.shouldStripFileExtension(fileExtension)) {
                    loadedKey = ResourceUtil.removeFileExtension(loadedKey);
                }

                if (results.containsKey(loadedKey) && !fileExtension.equals(SimpleResourcesCommon.getPreferredFormat())) {
                    ErrorUtil.reloadWarning("Duplicate data file ignored with ID " + loadedKey + " (path " + originalKey + ")");
                    continue;
                }

                results.put(loadedKey, parsed);
            } catch (Throwable e) {
                ErrorUtil.reloadError("Error occurred while loading resource: " + entry.getKey().toString(), e);
            }
        }

        this.loadedValue = ImmutableMap.copyOf(results);
        this.reloadListener.accept(this.loadedValue);
    }

    private SimpleSynchronousResourceReloadListener getListener(Identifier id, ResourceReadWriter<T> readWriter, RegistryWrapper.WrapperLookup lookup, Identifier... dependencies) {
        List<Identifier> fabricDependencies = Arrays.stream(dependencies).toList();
        return new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id;
            }

            @Override
            public void reload(ResourceManager manager) {
                parse(id.getPath(), manager, readWriter, lookup);
            }

            @Override
            public Collection<Identifier> getFabricDependencies() {
                return fabricDependencies;
            }
        };
    }
}
