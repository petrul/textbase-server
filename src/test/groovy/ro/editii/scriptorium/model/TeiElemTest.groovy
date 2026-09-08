package ro.editii.scriptorium.model

import org.junit.jupiter.api.Test
import ro.editii.scriptorium.model.TeiDiv
import ro.editii.scriptorium.model.TeiElem;

import static org.junit.jupiter.api.Assertions.*;
import static ro.editii.scriptorium.GTestUtil.p
class TeiElemTest {

    @Test
    void teiElem_toString() {
        final estr = new TeiElem().toString()
        assert ! estr.contains('_node')
        final dstr = new TeiDiv().toString()
        assert ! dstr.contains('_node')
    }
}