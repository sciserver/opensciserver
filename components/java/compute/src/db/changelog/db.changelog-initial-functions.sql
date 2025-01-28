--liquibase formatted sql

--changeset JosephBooker:initial-least_busy_node dbms:mysql,mariadb endDelimiter:DELIMITER
CREATE FUNCTION `least_busy_node`(domain_id BIGINT) RETURNS bigint(20)
BEGIN
        DECLARE result BIGINT;
        SELECT subquery.node_id FROM (
          SELECT node_id, COUNT(*) AS num_ports
          FROM
            `slot`
          JOIN
            `node`
          ON
            `slot`.`node_id` = `node`.`id`
          LEFT OUTER JOIN
            (SELECT * FROM `executable_container` WHERE `status`<>'DELETED') AS table2
          ON
            `slot`.`id` = table2.`slot_id`
          WHERE
            table2.`slot_id` IS NULL
            AND `node`.`domain_id` = domain_id
            AND `node`.`enabled` = TRUE
          GROUP BY `slot`.`node_id`
          ORDER BY `num_ports` DESC LIMIT 1) subquery INTO result;
          RETURN result;
    END ;;
DELIMITER

--changeset JosephBooker:initial-next-avaliable-slot dbms:mysql,mariadb endDelimiter:DELIMITER
CREATE FUNCTION `next_available_slot`(node_id BIGINT) RETURNS bigint(20)
BEGIN
      DECLARE result BIGINT;
      SELECT id FROM (
        SELECT table1.id, `port_number` FROM (
          SELECT `id`, `port_number` FROM `slot` WHERE `slot`.`node_id` = node_id) AS table1
          LEFT OUTER JOIN (SELECT `slot_id` FROM `executable_container` WHERE `status` <> 'DELETED') AS table2
          ON table1.id = table2.`slot_id`
          WHERE table2.`slot_id` IS NULL
          ORDER BY `port_number` ASC LIMIT 1) subquery INTO result;
      RETURN result;
    END ;;
DELIMITER

--changeset DmitryMedvedev:initial-add-port-range dbms:mysql,mariadb endDelimiter:DELIMITER
CREATE PROCEDURE `add_port_range`(node_id INT, port_from INT, port_to INT)
BEGIN
    DECLARE i INT;
    SET i = port_from;
    WHILE i <= port_to DO
        INSERT `slot` (`node_id`, `port_number`) VALUES (node_id, i);
        SET i = i + 1;
    END WHILE;
END ;;
DELIMITER
