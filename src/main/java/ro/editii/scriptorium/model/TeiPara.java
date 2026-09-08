package ro.editii.scriptorium.model;

import lombok.*;

/**
 * a 'paragraph' in the extended sense, that means an immediate child of
 * a {@link TeiDiv} which is usually displayed as block.
 *
 * As such, may really be, in the TEI xml: a <p>, an <lg>, a <table>, etc.
 */
@Data @AllArgsConstructor @NoArgsConstructor @EqualsAndHashCode(callSuper = true)
public class TeiPara extends TeiElem {
    TeiDiv parent;

    @Override
    public TeiDiv getOpus() {
        return parent.getOpus();
    }

    @Override
    public TeiDiv getDiv() {
        return this.parent;
    }

//    @Override
//    public TeiElemDto toDto(UriComponentsBuilder uriComponentsBuilder) {
//        return null;
//    }
}
