package ro.editii.scriptorium.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RestUtil {
    public static void throw404() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    public static void throw404(String message) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    public static void throw400(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public static void throw500(Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }

    public static void throw500(String message) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
