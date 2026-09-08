package ro.editii.scriptorium.search;

import lombok.EqualsAndHashCode;
import ro.editii.scriptorium.Couple;

@EqualsAndHashCode(callSuper = true)
public class Hit extends Couple<String, Float> {

    public Hit(String url, Float score) {
        super(url, score);
    }

    public String getUrl() { return super.getFirst(); }
    public Float getScore() { return super.getSecond(); }
}
