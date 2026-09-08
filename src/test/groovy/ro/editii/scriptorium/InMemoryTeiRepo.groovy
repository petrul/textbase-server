package ro.editii.scriptorium

import ro.editii.scriptorium.model.Languages
import ro.editii.scriptorium.tei.TeiRepo

/**
 * test repo that keeps its
 */
class InMemoryTeiRepo implements TeiRepo {

    final Map map = [:]

    @Override
    String getName() {
        return 'in-memory'
    }

    @Override
    InputStream getStreamForName(String resName) {
        String obj = map[resName]
        return new ByteArrayInputStream(obj.bytes)
    }

    @Override
    boolean has(String resName) {
        return this.map.containsKey(resName)
    }

    @Override
    File getFile(String resName) {
        return new File("/blahblash")
    }

    @Override
    List<String> list() {
        return this.map.keySet().toList()
    }

    @Override
    Languages getLanguageHint(String resName) {
        return Languages.RO
    }

    void putAt(String key, Object value) {
        this.map[key] = value
    }
}