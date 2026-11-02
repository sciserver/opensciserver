package org.sciserver.springapp.sciquery.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository of Database Domains.
 */
@Repository
@Schema(description = "Repository of Database Domains")
public interface DbDomainRepository extends CrudRepository<DbDomain, Long> {
    DbDomain findById(long id);

    DbDomain findByRacmId(long racmId);

    DbDomain findByRacmUuid(String racmUuid);
    
    List<DbDomain> findByDisplayName(String displayName);
}
