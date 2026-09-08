package ro.editii.scriptorium.search;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
public class MilvusHit extends Hit {

    public MilvusHit(String url, Float score, String content) {
        super(url, score);
        this.content = content;
    }

    @Getter
    String content;

    public String toString() {
        return String.format("MilvusHit[%s - %f - %s]", this.getUrl(), this.getScore(), this.getContent());
    }

    public static MilvusHit from(String url, float score, String content) {
        return new MilvusHit(url, score, content);
    }
}
