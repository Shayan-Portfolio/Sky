package engine.asset;

import engine.logging.SkyRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Asset<T> {
    private T object;
    private AssetPackage assetPackage;
    private String path;
    private transient Map<String, AssetListener> listeners = new HashMap<>();

    public Asset() {}

    public Asset(AssetPackage assetPackage, String path, T object) {
        this.assetPackage = assetPackage;
        this.path = path;
        this.object = object;
    }

    public void addListener(String id, AssetListener listener) {
        listeners.put(id, listener);
    }

    public void removeListener(String id) {
        listeners.remove(id);
    }

    public T getObject() {
        return object;
    }

    public AssetPackage getAssetPackage() {
        return assetPackage;
    }

    public String getPath() {
        return path;
    }

    public String getFQN() {
        return assetPackage.getNamespace() + ":" + path;
    }

    public void setObjectUnsafe(Object object) {
        try {
            this.object = (T) object;
        } catch (ClassCastException e) {
            throw new SkyRuntimeException("Invalid asset type");
        }
    }

    public void fireAssetListeners() {
        for(AssetListener listener : listeners.values()) {
            listener.onAssetChanged();
        }
    }
}
