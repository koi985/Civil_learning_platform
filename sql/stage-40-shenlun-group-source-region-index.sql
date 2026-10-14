ALTER TABLE shenlun_group
    DROP INDEX uk_shenlun_group_source_region,
    ADD INDEX idx_shenlun_group_source_region (source_name, region);
