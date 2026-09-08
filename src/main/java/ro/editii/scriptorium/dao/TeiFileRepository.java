package ro.editii.scriptorium.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import ro.editii.scriptorium.model.TeiDiv;
import ro.editii.scriptorium.model.TeiFile;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
@Repository
public interface TeiFileRepository extends JpaRepository<TeiFile, Long> {
    List<TeiFile> findByFilename(String filename);

    Optional<TeiFile> getByFilename(String filename);

    List<TeiFile> findByRepoName(String repoName);

    @Query("select distinct tf.repoName from TeiFile tf where tf.repoName is not null")
    List<String> findDistinctRepoNames();

    @Query("""
            select tf from TeiFile tf 
            join tf.authors a 
            where a.strId = ?1
            """)
    List<TeiFile> getTeiFilesForAuthorStrId(String strid);

    @RestResource(exported = false)
    @Override
    <S extends TeiFile> S save(S entity);

    @RestResource(exported = false)
    @Override
    void delete(TeiFile entity);
}
