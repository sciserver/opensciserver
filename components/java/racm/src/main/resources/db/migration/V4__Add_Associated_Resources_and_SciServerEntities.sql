CREATE TABLE t_AssociatedResource (
  ID BIGINT IDENTITY NOT NULL
, DTYPE  VARCHAR(32)
, OPTLOCK INTEGER
, publisherDID VARCHAR(256)
, containerId BIGINT NOT NULL -- Resource
, [usage] VARCHAR(255)
, [ownership] VARCHAR(255)
, resourceId BIGINT NOT NULL-- Resource
);

ALTER TABLE t_AssociatedResource ADD CONSTRAINT pk_t_AssociatedResource_ID PRIMARY KEY(ID);
CREATE INDEX ix_t_AssociatedResource___CONTAINER ON t_AssociatedResource(containerId);
CREATE INDEX ix_t_AssociatedResource_resource ON t_AssociatedResource(resourceId);
GO

CREATE TABLE t_AssociatedSciEntity (
  ID BIGINT IDENTITY NOT NULL
, DTYPE  VARCHAR(32)
, OPTLOCK INTEGER
, publisherDID VARCHAR(256)
, containerId BIGINT NOT NULL -- Resource
, [usage] VARCHAR(255)
, [ownership] VARCHAR(255)
, sciEntityId BIGINT NOT NULL-- SciserverEntity
);

ALTER TABLE t_AssociatedSciEntity ADD CONSTRAINT pk_t_AssociatedSciEntity_ID PRIMARY KEY(ID);
CREATE INDEX ix_t_AssociatedSciEntity___CONTAINER ON t_AssociatedSciEntity(containerId);
CREATE INDEX ix_t_AssociatedSciEntity_sciEntity ON t_AssociatedSciEntity(sciEntityId);
GO

CREATE TABLE t_CustomField (
  ID BIGINT IDENTITY NOT NULL
, DTYPE  VARCHAR(32)
, OPTLOCK INTEGER
, publisherDID VARCHAR(256)
, containerId BIGINT NOT NULL -- Resource
, [name] VARCHAR(255) NOT NULL
, [value] VARCHAR(MAX)
);

ALTER TABLE t_CustomField ADD CONSTRAINT pk_t_CustomField_ID PRIMARY KEY(ID);
CREATE INDEX ix_t_CustomField___CONTAINER ON t_CustomField(containerId);
GO

ALTER TABLE t_AssociatedResource ADD CONSTRAINT fk_t_AssociatedResource_container
	FOREIGN KEY (containerId) REFERENCES t_Resource(ID);

ALTER TABLE t_AssociatedResource ADD CONSTRAINT fk_t_AssociatedResource_resource
	FOREIGN KEY (resourceId)
	REFERENCES t_Resource(ID)
	ON DELETE CASCADE;

ALTER TABLE t_AssociatedSciEntity ADD CONSTRAINT fk_t_AssociatedSciEntity_container
	FOREIGN KEY (containerId) REFERENCES t_Resource(ID);

ALTER TABLE t_AssociatedSciEntity ADD CONSTRAINT fk_t_AssociatedSciEntity_sciEntity
	FOREIGN KEY (sciEntityId)
	REFERENCES t_SciserverEntity(ID)
	ON DELETE CASCADE;

ALTER TABLE t_CustomField ADD CONSTRAINT fk_t_CustomField_container
	FOREIGN KEY (containerId) REFERENCES t_Resource(ID);
GO

CREATE VIEW [AssociatedResource] AS
	SELECT ID,
		publisherDID,
		DTYPE,
		containerId,
		[usage],
		[ownership],
		resourceId
	FROM t_AssociatedResource
GO

CREATE VIEW [AssociatedSciEntity] AS
	SELECT ID,
		publisherDID,
		DTYPE,
		containerId,
		[usage],
		[ownership],
		sciEntityId
	FROM t_AssociatedSciEntity
GO

CREATE VIEW [CustomField] AS
	SELECT ID,
		publisherDID,
		DTYPE,
		containerId,
		[name],
		[value]
	FROM t_CustomField
GO