package ro.editii.scriptorium

import editii.commons.xml.XpathTool
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ro.editii.scriptorium.xslt.XsltTool

import static org.junit.jupiter.api.Assertions.*

class XsltToolTest {

    @Test
    public void applyXslt() throws IOException, URISyntaxException {

        URL resource = this.getClass().getClassLoader().getResource("Alecsandri-Scrieri-excerpt1.xml");
        InputStream xmlStream = resource.openStream();

        URL xsltResource = this.getClass().getClassLoader().getResource("xslt-tei-official-html/html5/html5.xsl");
        InputStream xsltAsStream = xsltResource.openStream();
        String xsltSystemId = xsltResource.toURI().toString();

        LOG.info("xsltSystemId : {}", xsltSystemId);
        assertNotNull(xsltAsStream);

        XsltTool xslttool = new XsltTool(xmlStream);
        xslttool.applyXslt(xsltAsStream, xsltSystemId, System.out);
    }

    @Test
    public void test_teidiv2html_Xslt() throws IOException, URISyntaxException {
        URL xsltResource = this.getClass().getClassLoader().getResource("xslt/teidiv2html.xsl");
        URL resource = this.getClass().getClassLoader().getResource("Alecsandri-Scrieri-excerpt1.xml");
        XpathTool xpathTool = new XpathTool(resource.openStream(), resource.toURI().toASCIIString());
//        DomTool.serialize(xpathTool.)
        XsltTool xsltTool = new XsltTool(resource.openStream());
        String res = xsltTool.applyXsltForString(xsltResource.openStream(), xsltResource.toURI().toString());
        LOG.info(res);
    }

    @Test
    void testXml2Text() throws IOException, URISyntaxException {
        URL xsltResource = this.getClass().getClassLoader().getResource("xslt/xml2text.xsl")
        URL resource = this.getClass().getClassLoader().getResource("Alecsandri-Scrieri-excerpt1.xml")
//        XpathTool xpathTool = new XpathTool(resource.openStream(), resource.toURI().toASCIIString())
        final XsltTool xsltTool = new XsltTool(resource.openStream())
        final String res = xsltTool.applyXsltForString(xsltResource.openStream(), xsltResource.toURI().toString());
        LOG.info("{}", res.length())

        assert xsltTool.getRawText().length() > 100;
    }

    @Test
    void testTeiDivXslt() {
        String xml = """
             <publicationStmt>
                <p>Attribution-ShareAlike 3.0 Unported (CC BY-SA 3.0)<lb/>
                   <ptr target="https://creativecommons.org/licenses/by-sa/3.0/"/>
                   <lb/>Corrections of occasional typos have been made.</p>
             </publicationStmt> """
        println xml

        def tr = XsltTool.getTransformer(this.class.classLoader.getResourceAsStream("xslt/teidiv2html.xsl"), "xslt/teidiv.xsl")
        println XsltTool.apply(tr, xml)
//        println(tr)
    }

    final private static Logger LOG = LoggerFactory.getLogger(XsltToolTest.class);
}