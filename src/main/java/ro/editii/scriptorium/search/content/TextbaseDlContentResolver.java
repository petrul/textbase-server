package ro.editii.scriptorium.search.content;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import ro.editii.scriptorium.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Deprecated
public class TextbaseDlContentResolver implements ContentResolver {

    final String rootDir;

    public TextbaseDlContentResolver(String rootDir) {
        this.rootDir = Util.replaceTilde(rootDir);
    }

    /**
     * get the content identified by the given id
     */
    public String resolve(String id) {

        String fragm = null;
        Integer nrPara = null;
        Integer nrSentence = null;

        if (id.contains("#")) {
            final var fragms = id.split("#");
            id = fragms[0];
            fragm = fragms[1];

            if (fragm.contains("-")) {
                final var s_p = fragm.split("-");
                nrPara = Integer.parseInt(s_p[0]);
                nrSentence = Integer.parseInt(s_p[1]);
            } else
                nrPara = Integer.parseInt(fragm);
        }
        final var f = new File(this.rootDir, id);
        try {
            final var text = IOUtils.readLines(
                    new BufferedInputStream(new FileInputStream(f)),
                    StandardCharsets.UTF_8);

            final var para = text.get(nrPara);
            if (nrSentence != null) {
                final var sentences = para.split("\\.");
                return sentences[nrSentence];
            } else
                return para;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        final var cr = new TextbaseDlContentResolver("~/data/textbase-dl");
        System.out.println(
                cr.resolve(
                        "cosbuc/nepublicate_in_volum/0073_41116_cosbuc__nepublicate_in_volum__petrea.txt#72-0")
        );
    }
}
