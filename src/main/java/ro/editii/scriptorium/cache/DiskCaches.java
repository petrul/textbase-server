package ro.editii.scriptorium.cache;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DiskCaches {

    @Autowired @Qualifier(CacheConf.CACHE_TOC)
    DiskCache cacheToc;

    @Autowired @Qualifier(CacheConf.CACHE_NODE)
    DiskCache cacheNode;

    @Autowired @Qualifier(CacheConf.CACHE_BINARY_OBJECT)
    DiskCache cacheBinaryObject;

    public void deleteAll() {
        this.cacheToc.delete();
        this.cacheNode.delete();
        this.cacheBinaryObject.delete();
    }

}
