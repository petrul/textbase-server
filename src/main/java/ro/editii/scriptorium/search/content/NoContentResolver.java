package ro.editii.scriptorium.search.content;

public class NoContentResolver implements ContentResolver{

    public String resolve(String id) {
        return id;
    }
}
