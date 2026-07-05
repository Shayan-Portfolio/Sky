package engine.util;

public class Pair<K, V> {
    public K key;
    public V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Pair<?, ?> pair) {
                return pair.key.equals(key) && pair.value.equals(value) ||
                    pair.key.equals(value) && pair.value.equals(key);
        }
        return false;
    }
}
