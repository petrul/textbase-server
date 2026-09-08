package ro.editii.scriptorium;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

public class TestUtils {
    public final static String TEI_ELEM = Util.TEI_ELEM;

    public static String randomString() {
        return randomString(20);
    }

    public static String randomString(int length) {
        return RandomStringUtils.randomAlphabetic(length);
    }

    public static String getTmpDir() {
        return System.getProperty("java.io.tmpdir");
    }

    public static void truncateAllTables(JdbcTemplate jt) {
        jt.update("SET FOREIGN_KEY_CHECKS = 0");
        jt.update("truncate table tei_file_authors");
        jt.update("truncate table author");
        jt.update("truncate table " + TEI_ELEM);
        jt.update("truncate table tei_file");
        jt.update("truncate table relocation");
        jt.update("SET FOREIGN_KEY_CHECKS = 1");

        assert countTableRows(jt, "author") == 0;
        assert countTableRows(jt, "tei_file_authors") == 0;
        assert countTableRows(jt, TEI_ELEM) == 0;
    }

    public static int countTableRows(JdbcTemplate jt, String tableName) {
        return jt.queryForObject(
                "select count(*) from " + tableName,
                Integer.class);
    }

    public static ObjectWriter jsonPp() {
        return new JsonMapper().writerWithDefaultPrettyPrinter();
    }

}
