package cz.yorick.api.resources;

import cz.yorick.resources.Util;
import net.minecraft.util.Identifier;

public interface ResourceUtil {
    /**
     * Gets the file extension from the path of the identifier
     * */
    static String getFileExtension(Identifier id) {
        return getFileExtension(id.getPath());
    }

    /**
     * Gets the file extension of a path, expected file separator is "/"
     * */
    static String getFileExtension(String path) {
        return Util.getFileExtension(path);
    }

    /**
     * Removes the file extension from the path of the identifier
     * */
    static Identifier removeFileExtension(Identifier id) {
        return id.withPath(removeFileExtension(id.getPath()));
    }

    /**
     * Removes file extension from a path, expected file separator is "/"
     * */
    static String removeFileExtension(String path) {
        return Util.removeFileExtension(path);
    }
}
