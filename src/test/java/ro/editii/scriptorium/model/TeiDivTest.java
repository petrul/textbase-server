package ro.editii.scriptorium.model;

import editii.commons.xml.TeiDocument;
import editii.commons.xml.XpathTool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *
 */
public class TeiDivTest {

    @Test
    public void cacheBug() throws IOException, URISyntaxException {
        final URL resource = this.getClass().getClassLoader().getResource("testrepo/ro/Alecsandri-Scrieri.xml");
        final InputStream is = resource.openStream();
        final String xpath = TeiDocument.XPATH_BODY + "/tei:div[4]/tei:div/tei:div[4]";
        final XpathTool xpathTool = new XpathTool(is, resource.toURI().toASCIIString());
        final String res = xpathTool.xpath(xpath);

        assert res != null;
        assert ! res.isEmpty();
        assert ! res.isBlank();

    }
}