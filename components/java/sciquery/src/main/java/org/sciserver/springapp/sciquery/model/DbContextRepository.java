package org.sciserver.springapp.sciquery.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository of Database Contexts.
 */
@Repository
@Schema(description = "Repository of Database Contexts")
public interface DbContextRepository extends CrudRepository<DbContext, Long> {
    DbContext findById(long id);

    DbContext findByRacmId(long racmId);

    DbContext findByRacmUuid(String racmUuid);

    List<DbContext> findByContextName(String contextName);

    List<DbContext> findByDbDomain(DbDomain dbDomain);

    List<DbContext> findByDbDomainAndContextName(DbDomain dbDomain, String contextname);
    
    List<DbContext> findByDbDomainAndContextNameIgnoreCase(DbDomain dbDomain, String contextname);
}
