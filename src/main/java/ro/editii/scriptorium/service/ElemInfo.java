package ro.editii.scriptorium.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.w3c.dom.Node;
import ro.editii.scriptorium.model.Author;
import ro.editii.scriptorium.model.TeiElem;

import java.util.List;


/**
 * this is an 'extension' of TeiElem which regroups also connex information
 * like author, a copy of the DOM node, the db children too.
 * Main use of this class is to pass a destructible version of the TeiElem to code
 * that want to remove, delete, modify the underlying DOM (without altering the original XML).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ElemInfo {
    Author author;
    TeiElem teiElem;
    Node nodeCopy;          // a deep copy of teiElem.getNode() so that you can potentially modify it.
    List<TeiElem> children; // div children
}