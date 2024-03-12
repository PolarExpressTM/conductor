package org.eu.polarexpress.conductor.repository;

import org.eu.polarexpress.conductor.model.Conductor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConductorRepository extends CrudRepository<Conductor, UUID> {
    List<Conductor> findAll();
    Optional<Conductor> findBySnowflakeId(String snowflakeId);
    Optional<Conductor> findByUsername(String username);
}
