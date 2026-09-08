package editii.commons.xml;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static editii.commons.xml.TeiDocument.XPATH_BODY;
import static org.junit.jupiter.api.Assertions.*;

public class XPathToolTest {

    /**
     * test for {@link XpathTool#_applyXpath(String, QName)}
     * @throws Exception
     */
    @Test
    public void testXpath() throws Exception {
        URL resource = this.getClass().getClassLoader().getResource("tei-excerpt-1.xml");
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("tei-excerpt-1.xml");
        XpathTool xpathTool = new XpathTool(is, resource.toURI().toASCIIString());
        NodeList teiHeaderList = xpathTool.applyXpathForNodeSet("/tei:TEI/tei:teiHeader");
        assertNotNull(teiHeaderList);
        Assertions.assertTrue(teiHeaderList.getLength() > 0, "there should be something inside the header");

        for (int i = 0; i < teiHeaderList.getLength(); i++) {
            Node item = teiHeaderList.item(i);
            println (item);
        }

        final String title = xpathTool.applyXpathForString("/tei:TEI/tei:teiHeader//tei:title");
        assert ("Scrieri".equals(title));
        
        final String author = xpathTool.applyXpathForString("/tei:TEI/tei:teiHeader//tei:author");
        // println(">> " + author);
        assert ("Alecsandri,Vasile".equals(author));

        final NodeList divs = xpathTool.applyXpathForNodeSet(TeiDocument.XPATH_BODY + "//tei:div");
        assert (divs.getLength() == 2);

        for (int i = 0 ; i < divs.getLength(); i++) {
            final Node div = divs.item(i);
            final XpathTool divTool = new XpathTool(div);
            final NodeList headnode = divTool.applyXpathForNodeSet(".//tei:head");
            assert headnode.getLength() > 0;

            final String head = divTool.xpath(".//tei:head");

            assert (head != null);
            assert (head.length() > 0);

        }

        {
            assertEquals("Alecsandri,Vasile", xpathTool.xpath("/tei:TEI/tei:teiHeader//tei:author"));
            assertEquals("Scrieri", xpathTool.xpath("/tei:TEI/tei:teiHeader//tei:title"));
            assertEquals("Manifeste și amintiri politice", xpathTool.xpath("/tei:TEI/tei:text/tei:body/tei:div/tei:head"));

            final String protestatie = "Protestație în numele Moldovei, al omenirii și al lui Dumnezeu(1848)";
            assertEquals(protestatie, xpathTool.xpath(XPATH_BODY + "/tei:div/tei:div/tei:head"));
            assertEquals(protestatie, xpathTool.xpath(XPATH_BODY + "/tei:div[1]/tei:div/tei:head"));
            assertEquals(protestatie, xpathTool.xpath(XPATH_BODY + "/tei:div/tei:div[1]/tei:head"));
            assertEquals(protestatie, xpathTool.xpath(XPATH_BODY + "/tei:div[1]/tei:div[1]/tei:head"));

            assertEquals("Protestație în numele Moldovei, al omenirii și al lui Dumnezeu(1848)", 
                xpathTool.xpath(XPATH_BODY + "/tei:div/tei:div/tei:head"));
        }
    }

    /**
     * test for {@link XpathTool#getXPath(Node)}
     */
    @Test
    public void testComputeXpathOfDiv() throws Exception {
        final URL resource = this.getClass().getClassLoader().getResource("testrepo/ro/Alecsandri-Scrieri.xml");
        final InputStream is = resource.openStream();
        final XpathTool xpathTool = new XpathTool(is, resource.toURI().toASCIIString());
        final NodeList divs = xpathTool.applyXpathForNodeSet("//tei:div");
        final Collection<Node> arr = DomTool.nodeList2Collection(divs);
        assertEquals(222, arr.size());
        for (Node node : arr) {
            final String head = new XpathTool(node).xpath("head");

            final String div_xpath = XpathTool.getXPath(node);

            final NodeList re_node_list = xpathTool.applyXpathForNodeSet(div_xpath);
            assertEquals(1, re_node_list.getLength());
            final Node re_node = re_node_list.item(0);
            final String re_head = new XpathTool(re_node).xpath("head");

            assert  head != null;
            assert ! div_xpath.isBlank();

            assertEquals(head, re_head);

        }
    }

    @Test
    public void testBug() throws Exception {
        final URL resource = this.getClass().getClassLoader().getResource("testrepo/ro/Alecsandri-Scrieri.xml");
        final InputStream is = resource.openStream();
        final XpathTool xpathTool = new XpathTool(is, resource.toURI().toASCIIString());

        final NodeList nodeList = xpathTool.applyXpathForNodeSet("/tei:TEI/tei:text/tei:body/tei:div[4]/tei:div[2]/tei:head");
        assertNotNull(nodeList);
        assertEquals(1, nodeList.getLength());
        assertEquals("Melodiile românești", nodeList.item(0).getTextContent());
    }

    /**
     *  4 june 2021 bug: 0 or several (0) divs correspond to xpath /tei:TEI/tei:text/tei:body/tei:div[4]/tei:div/tei:div[4]
     *  http://localhost:8080/alecsandri/suvenire/maiorului_iancu_bran
     */
    static LRUMap<String, XpathTool> cache = new LRUMap<>(10, 0);
    @Test
    public void testBug2() throws IOException, URISyntaxException, InterruptedException {
        final String resname = "testrepo/ro/Alecsandri-Scrieri.xml";
        final URL resource = this.getClass().getClassLoader().getResource(resname);
        final InputStream is = resource.openStream();
        final String orig_xpath = XPATH_BODY + "/tei:div[4]/tei:div/tei:div[4]";

        final XpathTool xpathTool = new XpathTool(is, resource.toURI().toASCIIString());
        final String origRes = xpathTool.xpath(orig_xpath);
        
        assertTrue(Strings.isNotEmpty(origRes));
        final int resLen = origRes.length();
        assert resLen > 0;
        
        cache.put(resname, xpathTool);

        final String s1 = cache.get(resname).xpath(orig_xpath);
        final String s2 = cache.get(resname).xpath(XPATH_BODY + "/tei:div[4]/tei:div");
        final String s3 = cache.get(resname).xpath(XPATH_BODY + "/tei:div[4]");
        final String s4 = cache.get(resname).xpath(XPATH_BODY );
        final String s5 = cache.get(resname).xpath("/tei:TEI/tei:text");

        assertTrue(s1.length() < s2.length());
        assertTrue(s2.length() < s3.length());
        assertTrue(s3.length() < s4.length());
        assertTrue(s4.length() < s5.length());

        final int n = 100;
        final ExecutorService executorService = Executors.newFixedThreadPool(n);

        for (int i = 0; i < n; i++) {
            executorService.submit(() -> {
                LOG.info("starting from thread " + Thread.currentThread().getName());

                final String res = cache.get(resname).xpath(orig_xpath);

                assertEquals(res, origRes);
                assertTrue(Strings.isNotEmpty(res));

                LOG.info("ended thread " + Thread.currentThread().getName());
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);

    }


    static void println(Object obj) {
        System.out.println(obj);
    }

//    void println_nodeset(NodeList nodeList) {
//        for (int i = 0; i < nodeList.getLength(); i++) {
//            println(nodeList.item(i));
//        }
//    }

    @Test
    public void testXpathForString() {
        XpathTool xpathTool = XpathTool.from("<doc><head> <label>Cap. 1</label>  Aici se intampla ceva</head></doc>");
        Node doc = xpathTool.applyXpathForNodeSet("/doc").item(0);
        assertNotNull(doc);
        assertEquals("doc", doc.getNodeName());

        XpathTool xp2 = new XpathTool(doc);

        assertEquals(" Cap. 1  Aici se intampla ceva", xp2.xpath("head"));

        NodeList nodeList = xp2.applyXpathForNodeSet("head/text()");
        assertEquals(2, nodeList.getLength());
        assertEquals(" ", nodeList.item(0).getTextContent());
        assertEquals("  Aici se intampla ceva", nodeList.item(1).getTextContent());
        assertEquals("   Aici se intampla ceva", xp2.xpath("head/text()"));
    }

    final private static Logger LOG = Logger.getLogger(XPathToolTest.class.getName());
}
