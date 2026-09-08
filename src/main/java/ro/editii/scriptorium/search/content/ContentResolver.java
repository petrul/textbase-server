package ro.editii.scriptorium.search.content;

/**
 * there is recurring proble this interface solves: getting content
 * corresponding to a given id.
 */
public interface ContentResolver {

    /**
     * get the content identified by the given id
     */
    String resolve(String id);
}
