package ro.editii.scriptorium.web;


import editii.commons.xml.DomTool;
import editii.commons.xml.XpathTool;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ro.editii.scriptorium.tei.TeiRepo;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Controller
@RequestMapping("/api/files")
@Log4j2
@Hidden
public class FilesController {

    @Autowired
    TeiRepo teiRepo;

    @GetMapping("/")
    public @ResponseBody List<String> getFiles() {
        return teiRepo.list();
    }

    /**
     * serves the whole file
     * @param response
     */
    @GetMapping("/file")
    public void getFile(@RequestParam String name , HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setHeader("Content-type", "application/xml; charset=utf-8");
            String filename = name;
            filename = filename.replaceAll("\\/+","\\/");

            InputStream is = this.teiRepo.getStreamForName(filename);

            ServletOutputStream os = response.getOutputStream();
            IOUtils.copy(is, os);
            os.flush();
            os.close();
            is.close();
        } catch (FileNotFoundException e) {
            throw new ResourceNotFoundException(String.format("file [%s] not found", name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * serves a fragment of a file
     */
    public void getFragment(@PathVariable String file,
                            HttpServletRequest request,
                            HttpServletResponse response)
            throws
            IOException, SAXException, XPathExpressionException {

        String urlStart = "/api/files/" + file;


        final String _path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        if (! _path.startsWith(urlStart))
            throw new RuntimeException("url should start with " + urlStart);
        String xpath = _path.substring(urlStart.length());


        File sourceFile = this.teiRepo.getFile(file);

        XpathTool xt = new XpathTool(sourceFile);
        NodeList nodeList = xt.applyXpathForNodeSet(xpath);

        Node grouped = DomTool.rootForResults(nodeList);

        DomTool.serialize(grouped, response.getOutputStream());

    }
}
