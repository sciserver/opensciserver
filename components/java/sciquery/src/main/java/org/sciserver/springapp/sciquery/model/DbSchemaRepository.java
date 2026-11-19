package org.sciserver.springapp.sciquery.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository of Database Schemas.
 */
@Repository
@Schema(description = "Repository of Database Schemas")
public interface DbSchemaRepository extends CrudRepository<DbSchema, Long> {
    DbSchema findById(long id);

    DbSchema findByRacmId(long racmId);

    DbSchema findByRacmUuid(String racmUuid);

    List<DbSchema> findBySchemaName(String schemaName);

    List<DbSchema> findByDbContext(DbContext dbContext);
}
