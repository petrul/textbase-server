package ro.editii.scriptorium.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ro.editii.scriptorium.model.TeiElem;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class TeiElemDto {

    Long id;
    TeiElemDto parent;

    String name;  // i.e 'p', 'lg', 'table' (without the tei: prefix).

    String path;  //  the complete textbase url path (from '/author/opus/...') needed to reach this elem

    // @deprecated ? -- this does not look like very public information
    String xpath; // the tei xpath needed to reach this elem
    // @deprecated ? -- this is also included by path
    String urlFragment;

    String url;
    String text; // put here the actual content of the elem, in some preconvened format.
    String text_sha256; // this is the sha256 fingerprint of the text, uniquely identifying it

    // ISO 639-1 code (e.g. "en", "ro") of the parent TeiFile's detected
    // language (see LanguageDetectionService) - a whole TEI file is one
    // language, elements don't carry their own. Null if undetected.
    String language;

    Integer size; // size in chars or bytes.
    Integer wordSize; // size in words

}
