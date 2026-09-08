package ro.editii.scriptorium;

import java.util.concurrent.atomic.AtomicReference;

public class Globals {

    // mutex for not attempting multimple importing at once as it is quite a lengthy operation
    final public static AtomicReference<Boolean> IMPORT_TEIS_WORKING = new AtomicReference<>(false);

}
