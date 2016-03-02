package gov.nist.toolkit.configDatatypes.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps between TransactionType name and list of PatientErrors
 */
public class PatientErrorMap implements Serializable, IsSerializable, Map<String, PatientErrorList> {
    Map<String, PatientErrorList> config = new HashMap<>();

    public PatientErrorMap() {}

    @Override
    public int size() {
        return config.size();
    }

    @Override
    public boolean isEmpty() {
        return config.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return config.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return config.containsValue(value);
    }

    @Override
    public PatientErrorList get(Object key) {
        return config.get(key);
    }

    @Override
    public PatientErrorList put(String key, PatientErrorList value) {
        return config.put(key, value);
    }

    @Override
    public PatientErrorList remove(Object key) {
        return config.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends PatientErrorList> m) {
        config.putAll(m);
    }

    @Override
    public void clear() {
        config.clear();
    }

    @Override
    public Set<String> keySet() {
        return config.keySet();
    }

    @Override
    public Collection<PatientErrorList> values() {
        return config.values();
    }

    @Override
    public Set<Entry<String, PatientErrorList>> entrySet() {
        return config.entrySet();
    }
}
