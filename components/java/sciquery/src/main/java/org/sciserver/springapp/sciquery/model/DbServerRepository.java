package org.sciserver.springapp.sciquery.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository of Database Servers.
 */
@Repository
@Schema(description = "Repository of Database Servers")
public interface DbServerRepository extends CrudRepository<DbServer, Long> {
    DbServer findById(long id);

    List<DbServer> findByServerName(String serverName);

    DbServer findByHost(String host);
}
