package ro.editii.scriptorium.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import ro.editii.scriptorium.model.AppUser;
import ro.editii.scriptorium.model.DivCollection;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(exported = false)
@Repository
public interface DivCollectionRepository extends JpaRepository<DivCollection, Long> {
    List<DivCollection> findByOwner(AppUser owner);

    Optional<DivCollection> findByOwnerAndName(AppUser owner, String name);

    boolean existsByOwnerAndName(AppUser owner, String name);
}
