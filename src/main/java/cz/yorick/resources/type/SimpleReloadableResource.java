package cz.yorick.resources.type;

import cz.yorick.api.resources.ReloadableResourceKey;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class SimpleReloadableResource<T> extends SimpleResource<T> implements ReloadableResourceKey<T> {
    private final Consumer<T> reloadListener;
    public SimpleReloadableResource(Identifier configId, Loader<T> loader, Consumer<T> reloadListener) {
        super(configId, loader);
        this.reloadListener = reloadListener;
        //supers constructor loads the value
        this.reloadListener.accept(this.getLoadedValue());
    }

    @Override
    public void reload(Consumer<Exception> errorHandler) {
        this.load(errorHandler::accept);
        this.reloadListener.accept(this.getLoadedValue());
    }

    @Override
    public T getValue() {
        return this.getLoadedValue();
    }
}
