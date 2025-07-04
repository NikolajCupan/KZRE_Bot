CREATE TABLE IF NOT EXISTS user (
    snowflake_user BIGINT NOT NULL,
    PRIMARY KEY (snowflake_user),
    UNIQUE INDEX id_user_UNIQUE (snowflake_user ASC) VISIBLE
);

CREATE TABLE IF NOT EXISTS guild (
    snowflake_guild BIGINT NOT NULL,
    PRIMARY KEY (snowflake_guild),
    UNIQUE INDEX snowflake_guild_UNIQUE (snowflake_guild ASC) VISIBLE
);

CREATE TABLE IF NOT EXISTS tag (
    id_tag BIGINT NOT NULL AUTO_INCREMENT,
    snowflake_author BIGINT NOT NULL,
    snowflake_guild BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_tag),
    UNIQUE INDEX id_tag_UNIQUE (id_tag ASC) VISIBLE,
    UNIQUE INDEX `snowflake_guild_tag_UNIQUE` (`snowflake_guild` ASC, `tag` ASC) VISIBLE,
    INDEX fk_tag_user1_idx (snowflake_author ASC) VISIBLE,
    INDEX fk_tag_guild1_idx (snowflake_guild ASC) VISIBLE,
    CONSTRAINT fk_tag_user1 FOREIGN KEY (snowflake_author) REFERENCES user (snowflake_user),
    CONSTRAINT fk_tag_guild1 FOREIGN KEY (snowflake_guild) REFERENCES guild (snowflake_guild)
);

CREATE TABLE IF NOT EXISTS quote (
    id_quote BIGINT NOT NULL AUTO_INCREMENT,
    snowflake_author BIGINT NOT NULL,
    snowflake_guild BIGINT NOT NULL,
    quote TEXT NOT NULL,
    date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_quote),
    UNIQUE INDEX id_quote_UNIQUE (id_quote ASC) VISIBLE,
    INDEX fk_quote_user1_idx (snowflake_author ASC) VISIBLE,
    INDEX fk_quote_guild1_idx (snowflake_guild ASC) VISIBLE,
    CONSTRAINT fk_quote_user1 FOREIGN KEY (snowflake_author) REFERENCES user (snowflake_user),
    CONSTRAINT fk_quote_guild1 FOREIGN KEY (snowflake_guild) REFERENCES guild (snowflake_guild)
);

CREATE TABLE IF NOT EXISTS quote_tag (
    id_tag BIGINT NOT NULL,
    id_quote BIGINT NOT NULL,
    INDEX fk_quote_tag_tag_idx (id_tag ASC) VISIBLE,
    INDEX fk_quote_tag_quote1_idx (id_quote ASC) VISIBLE,
    PRIMARY KEY (id_tag, id_quote),
    CONSTRAINT fk_quote_tag_tag FOREIGN KEY (id_tag) REFERENCES tag (id_tag),
    CONSTRAINT fk_quote_tag_quote1 FOREIGN KEY (id_quote) REFERENCES quote (id_quote)
);
