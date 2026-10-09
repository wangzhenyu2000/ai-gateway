-- Repair comments that were double-encoded (UTF-8 bytes interpreted as latin1,
-- then stored as utf8mb4) during container init. Non-destructive: only rewrites
-- table/column COMMENT metadata, never touches row data.
--
-- Usage:
--   Get-Content -Raw .\db\repair-comments.sql | docker exec -i ai-gateway-mysql \
--     mysql -uroot -proot123456 --default-character-set=utf8mb4

SET NAMES utf8mb4;
USE ai_gateway;

DROP PROCEDURE IF EXISTS repair_comments;
DELIMITER $$

CREATE PROCEDURE repair_comments()
BEGIN
    DECLARE v_done    INT DEFAULT 0;
    DECLARE v_table   VARCHAR(64);
    DECLARE v_col     VARCHAR(64);
    DECLARE v_type    VARCHAR(512);
    DECLARE v_null    VARCHAR(3);
    DECLARE v_default TEXT;
    DECLARE v_extra   VARCHAR(64);
    DECLARE v_comment TEXT;
    DECLARE v_stmt    TEXT;

    -- Table comments
    DECLARE tcur CURSOR FOR
        SELECT TABLE_NAME,
               CONVERT(BINARY CONVERT(TABLE_COMMENT USING latin1) USING utf8mb4)
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = 'ai_gateway' AND TABLE_TYPE = 'BASE TABLE';

    -- Column comments (definition is restated from information_schema)
    DECLARE ccur CURSOR FOR
        SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA,
               CONVERT(BINARY CONVERT(COLUMN_COMMENT USING latin1) USING utf8mb4)
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = 'ai_gateway';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN tcur;
    tloop: LOOP
        FETCH tcur INTO v_table, v_comment;
        IF v_done THEN LEAVE tloop; END IF;
        SET v_stmt = CONCAT('ALTER TABLE `', v_table, '` COMMENT ', QUOTE(v_comment));
        SET @s = v_stmt;
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE tcur;

    SET v_done = 0;

    OPEN ccur;
    cloop: LOOP
        FETCH ccur INTO v_table, v_col, v_type, v_null, v_default, v_extra, v_comment;
        IF v_done THEN LEAVE cloop; END IF;
        SET v_stmt = CONCAT(
            'ALTER TABLE `', v_table, '` MODIFY COLUMN `', v_col, '` ', v_type,
            IF(v_null = 'NO', ' NOT NULL', ' NULL'),
            IF(v_extra LIKE '%auto_increment%', ' AUTO_INCREMENT',
                IF(v_default IS NOT NULL,
                    IF(v_extra LIKE '%DEFAULT_GENERATED%',
                        CONCAT(' DEFAULT ', v_default),
                        CONCAT(' DEFAULT ', QUOTE(v_default))),
                    '')),
            IF(v_extra LIKE '%on update%', ' ON UPDATE CURRENT_TIMESTAMP', ''),
            ' COMMENT ', QUOTE(v_comment)
        );
        SET @s = v_stmt;
        PREPARE stmt FROM @s;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE ccur;
END$$

DELIMITER ;

CALL repair_comments();
DROP PROCEDURE repair_comments;
