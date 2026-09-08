package ro.editii.scriptorium.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import ro.editii.scriptorium.model.Relocation;

@RepositoryRestResource(exported = false)
@Repository
public interface RelocationRepository extends JpaRepository<Relocation, String> {}
