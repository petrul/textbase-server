package ro.editii.scriptorium.model

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class AuthorTest {

    // ES-19
    @Test
    public void newFromOriginalNameInTeiFile() {

        final eminescu1 = "Eminescu,Mihai"
        final eminescu2 = "Eminescu, Mihai" // note the space

        final author1 = Author.newFromOriginalNameInTeiFile(eminescu1)
        final author2 = Author.newFromOriginalNameInTeiFile(eminescu2)

        assert author1.lastName == author2.lastName
        assert author1.firstName == author2.firstName
        assert author1.originalNameInTeiFile == author2.originalNameInTeiFile

        final toparceanul = Author.newFromOriginalNameInTeiFile("George Topârceanu")
        assert (toparceanul.firstName == 'George')
        assert (toparceanul.lastName == 'Topârceanu')
    }

    @Test
    public void testStaticPropertiesAreRead() {
        assert Author.FORBIDDEN_AUTHOR_NAMES != null
        assert Author.RECOMMENDED_AUTHOR_MAPPINGS != null

        assert Author.FORBIDDEN_AUTHOR_NAMES.size() > 0
        assert Author.RECOMMENDED_AUTHOR_MAPPINGS.size() > 0

        println Author.FORBIDDEN_AUTHOR_NAMES
        println Author.RECOMMENDED_AUTHOR_MAPPINGS

        assert Author.FORBIDDEN_AUTHOR_NAMES.findAll { it.startsWith("#") }.size() == 0
    }

    @Test
    void testNewFromOriginalNameInTeiFile() {
        final alecsandri = Author.newFromOriginalNameInTeiFile("Vasile Alecsandri")
        assert alecsandri.firstName == 'Vasile'
        assert alecsandri.lastName == 'Alecsandri'

        final reginaMaria = Author.newFromOriginalNameInTeiFile("Regina Maria a României")
        assert reginaMaria.displayName == 'Regina Maria a României'
        assert reginaMaria.firstName == 'Maria'
    }
}
