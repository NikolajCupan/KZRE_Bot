CREATE TABLE IF NOT EXISTS user (
    id_user BIGINT NOT NULL AUTO_INCREMENT,
    snowflake_user BIGINT NOT NULL,
    PRIMARY KEY (id_user),
    UNIQUE INDEX snowflake_UNIQUE (snowflake_user ASC) VISIBLE,
    UNIQUE INDEX id_user_UNIQUE (id_user ASC) VISIBLE
);

CREATE TABLE IF NOT EXISTS tag (
    id_tag BIGINT NOT NULL AUTO_INCREMENT,
    author BIGINT NOT NULL,
    snowflake_guild BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_tag),
    UNIQUE INDEX id_tag_UNIQUE (id_tag ASC) VISIBLE,
    UNIQUE INDEX snowflake_guild_tag_UNIQUE (snowflake_guild, tag) VISIBLE,
    INDEX fk_tag_user1_idx (author ASC) VISIBLE,
    CONSTRAINT fk_tag_user1 FOREIGN KEY (author) REFERENCES user (id_user)
);

CREATE TABLE IF NOT EXISTS quote (
    id_quote BIGINT NOT NULL AUTO_INCREMENT,
    author BIGINT NOT NULL,
    snowflake_guild BIGINT NOT NULL,
    quote TEXT NOT NULL,
    date_added DATETIME NOT NULL,
    PRIMARY KEY (id_quote),
    UNIQUE INDEX id_quote_UNIQUE (id_quote ASC) VISIBLE,
    INDEX fk_quote_user1_idx (author ASC) VISIBLE,
    CONSTRAINT fk_quote_user1 FOREIGN KEY (author) REFERENCES user (id_user)
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
