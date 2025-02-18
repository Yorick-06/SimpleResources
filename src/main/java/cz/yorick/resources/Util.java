package cz.yorick.resources;

import cz.yorick.resources.type.SimpleReloadableResource;
import cz.yorick.resources.type.SimpleResource;
import net.minecraft.util.Identifier;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class Util {
    public static<T> Supplier<T> factoryFor(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return () -> {
                try {
                    return constructor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke the constructor of class " + clazz.getName(), e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " does not have a default (no-argument) constructor!");
        }
    }

    public static String getFileExtensionOrThrow(String path) throws IllegalArgumentException {
        String fileExtension = getFileExtension(path);
        if(fileExtension == null) {
            throw new IllegalArgumentException("File " + path + " does not have a file extension!");
        }

        return fileExtension;
    }

    public static String getFileExtension(String path) {
        String[] paths = path.split("/");
        String fileName = paths[paths.length - 1];
        int lastDotIndex = fileName.lastIndexOf(".");
        if(lastDotIndex == -1 || fileName.endsWith(".")) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1);
    }

    public static String pathToString(Path path) {
        File file = path.toFile();
        List<String> files = new ArrayList<>();
        while (file != null) {
            files.add(file.getName());
            file = file.getParentFile();
        }

        return String.join("/", files.reversed());
    }

    public static String removeFileExtension(String path) {
        String fileExtension = getFileExtension(path);
        if(fileExtension == null) {
            return path;
        }

        return path.substring(0, path.length() - fileExtension.length() - 1);
    }

    private static final Map<Identifier, SimpleResource<?>> resources = new HashMap<>();
    private static final Map<Identifier, SimpleReloadableResource<?>> reloadableResources = new LinkedHashMap<>();
    public static void registerConfig(Identifier id, SimpleResource<?> config) {
        if(resources.containsKey(id)) {
           throw new IllegalArgumentException("Attempted to register a resource with a duplicate id '" + id + "'");
        }

        resources.put(id, config);
        if(config instanceof SimpleReloadableResource<?> reloadableResource) {
            reloadableResources.put(id, reloadableResource);
        }
    }

    public static Collection<Identifier> getReloadableResourceKeys() {
        return reloadableResources.keySet();
    }

    public static Collection<SimpleReloadableResource<?>> getReloadableResources() {
        return reloadableResources.values();
    }

    public static SimpleReloadableResource<?> getReloadableResource(Identifier id) {
        return reloadableResources.get(id);
    }
}
