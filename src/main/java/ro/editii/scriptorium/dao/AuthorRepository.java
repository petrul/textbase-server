package ro.editii.scriptorium.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import ro.editii.scriptorium.model.Author;
import ro.editii.scriptorium.model.TeiFile;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findByStrId(String strId);
    Page<Author> findByStrIdContainingIgnoreCase(String strId, Pageable page);
    List<Author> findByOriginalNameInTeiFile(String originalName);

    Optional<Author> getByStrId(String strId);
    Optional<Author> getByOriginalNameInTeiFile(String originalName);

    @Query("select tf from TeiFile tf join tf.authors a where a.id = ?1")
    List<TeiFile> getTeiFiles(long authorId);

    Page<Author> findByLastNameContainingIgnoreCase(String excerpt, Pageable page);
    Page<Author> findByLastNameIgnoreCase(String excerpt, Pageable page);
    Page<Author> findByFirstNameContainingIgnoreCase(String excerpt, Pageable page);

    @RestResource(exported = false)
    @Override
    <S extends Author> S save(S entity);

    @RestResource(exported = false)
    @Override
    void delete(Author entity);

}
