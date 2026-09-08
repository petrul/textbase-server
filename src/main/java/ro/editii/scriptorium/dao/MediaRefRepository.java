package ro.editii.scriptorium.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import ro.editii.scriptorium.media.MediaRef;

@RepositoryRestResource(exported = false)
@Repository
public interface MediaRefRepository extends JpaRepository<MediaRef, String> {
    @RestResource(exported = false)
    @Override
    <S extends MediaRef> S save(S entity);

    @RestResource(exported = false)
    @Override
    void delete(MediaRef entity);
}
