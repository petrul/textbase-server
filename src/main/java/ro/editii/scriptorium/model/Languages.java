package ro.editii.scriptorium.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * lang codes: https://www.loc.gov/standards/iso639-2/php/code_list.php
 */
public enum Languages {

    BG("bulgarian"),
    BR("britton"),
    CA("catalan"),
    DA("danish"),
    DE("german"),
    EN("english"),
    ES("spanish"),
    FI("finnish"),
    FR("french"),
    GR("greek"),
    HU("hungarian"),
    IT("italian"),
    LA("latin"),
    NL("dutch"),
    NO("norwegian"),
    PT("portuguese"),
    RO("romanian"),
    RU("russian"),
    ZH("chinese")
    ;

    String enName; // english name

    Languages(String enName) {
        this.enName = enName;
    }

    public String getEnName() {
        return this.enName;
    }

    public static Languages from(String repr) {
        final Optional<Languages> any = Arrays.stream(Languages.values())
                .filter(it -> it.name().toLowerCase().equals(repr.toLowerCase()))
                .findFirst();
        if (any.isEmpty()) return null;
        return any.get();
    }

    public String getTwoLetteredCode() {
        return this.getISO639_1Code();
    }
    public String getISO639_1Code(){
        return this.name().toLowerCase();
    }
}
