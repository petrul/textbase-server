package ro.editii.scriptorium.cache;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;

@Log4j2
public class DiskCache {

    final File basedir;

    public DiskCache(File basedir) {
        this.basedir = basedir;
        ensureBasedirExists(basedir);
    }

    /**
     * @param basedir the generic directory cache directory containing potentially several caches
     * @param name this cache's name which will be a subdir in basedir
     */
    public DiskCache(File basedir, String name) {
        this(new File(basedir, name));
    }

    public void putAt(Object key, Object value) {
        ensureBasedirExists(basedir);
        final File file = new File(this.basedir, key.toString());
        try (
            final FileOutputStream  fos = new FileOutputStream(file);
            final BufferedOutputStream bos = new BufferedOutputStream(fos);
            final ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {
            oos.writeObject(value);
        } catch (IOException  e) {
            throw new RuntimeException(e);
        }
    }

    public boolean has(Object key) {
        final File file = new File(this.basedir, key.toString());
        return file.exists();
    }

    public Object getAt(Object key) {
        final File file = new File(this.basedir, key.toString());
        if (! file.exists())
            return null;

        try (
                final FileInputStream fis = new FileInputStream(file);
                final BufferedInputStream bos = new BufferedInputStream(fis);
                final ObjectInputStream oos = new ObjectInputStream(bos)
        ) {
            return oos.readObject();
        } catch (Exception e) {
            file.delete();
            return null;
        }
    }

    public void delete() {
        try {
            FileUtils.deleteDirectory(this.basedir);
            log.debug("deleted " + this.basedir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void ensureBasedirExists(File dir) {
        if (!dir.exists()) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
