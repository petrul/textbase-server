package ro.editii.scriptorium.search;

import ro.editii.scriptorium.model.TeiDiv;


public class DivHit extends Hit {
    TeiDiv div;

    public DivHit(String url, Float score, TeiDiv div) {
        super(url, score);
        this.div = div;
    }
}
