package ro.editii.scriptorium.search;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = true)
public class LuceneHit extends Hit {

    @Getter
    String content;

    @Getter
    String head;

    public LuceneHit(String url, Float score, String content, String head) {
        super(url, score);
        this.content = content;
        this.head = head;
    }

    public String toString() {
        return String.format("LuceneHit[%s - %f - %s]", this.getUrl(), this.getScore(), this.getHead());
    }
}
