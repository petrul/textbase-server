package ro.editii.scriptorium.search;

import ro.editii.scriptorium.model.Author;

public class AuthorHit extends Hit {
    Author author;

    public AuthorHit(String url, Float score, Author author) {
        super(url, score);
        this.author = author;
    }
}
