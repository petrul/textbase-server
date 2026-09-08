package ro.editii.scriptorium;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component @Getter
public class TextbaseConfig {

    @Value("${textbase.advertised.url:https://textbase.scriptorium.ro}")
    protected String textbaseAdvertisedUrl;

}
