package editii.commons.xml

import org.junit.jupiter.api.Test
import org.w3c.dom.Node

import static ro.editii.scriptorium.GTestUtil.*

class GXPathToolTest {

    /**
     * https://yt.scriptorium.ro/youtrack/issue/TB-219
     */
    @Test
    void testRemovingLabelStillKeepsTheHeadParseable() {
        final str =
            """<head>
                 <label>Cap. 1</label>
                 Manifeste și amintiri politice
             </head>
             """

        def xt = new XpathTool(str)
        final headList = xt.applyXpathForNodeSet('/head')
        assert headList.getLength() == 1
        def head = headList.item(0)
        assert head != null

        head = DomTool.deepCopy(head)

        xt = new XpathTool(head)
        final textNodes = xt.applyXpathForNodeSet(".//text()")
        assert textNodes.getLength() == 3
        for (int i = 0; i < textNodes.getLength(); i++) {
            final txt = textNodes.item(i)
            p txt
        }
        assert head.getChildNodes().length == 3
        Node label = null
        for (int i = 0; i < head.getChildNodes().getLength(); i++) {
            final Node c = head.getChildNodes().item(i)
            p "$i - ${c.nodeName} - [${c.nodeValue}]"

            if (c.nodeName == 'label')
                label = c
        }
        // with label
        assert head.getTextContent().trim()
                .replaceAll('\n', ' ')
                .replaceAll('\\s+', ' ') == 'Cap. 1 Manifeste și amintiri politice'

        head.removeChild(label)
        assert head.getChildNodes().length == 2

        // label was removed
        assert head.getTextContent().trim() == 'Manifeste și amintiri politice'
    }
}
