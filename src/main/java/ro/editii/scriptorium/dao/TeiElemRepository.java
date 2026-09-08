package ro.editii.scriptorium.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;
import ro.editii.scriptorium.model.TeiElem;

@Repository
public interface TeiElemRepository extends JpaRepository<TeiElem, Long> {

    @RestResource(exported = false)
    @Override
    <S extends TeiElem> S save(S entity);

    @RestResource(exported = false)
    @Override
    void delete(TeiElem entity);
}
