package org.eu.polarexpress.conductor.repository;

import org.eu.polarexpress.conductor.model.Server;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServerRepository extends CrudRepository<Server, UUID> {
    List<Server> findAll();
}
