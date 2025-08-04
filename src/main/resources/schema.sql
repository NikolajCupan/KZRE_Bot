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
    UNIQUE INDEX snowflake_guild_tag_UNIQUE (snowflake_guild ASC, tag ASC) VISIBLE,
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
    quote_hash BINARY(32) GENERATED ALWAYS AS (UNHEX(SHA2(quote, 256))) VIRTUAL,
    quote_simhash BINARY(128) NOT NULL,
    date_created DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_quote),
    UNIQUE INDEX id_quote_UNIQUE (id_quote ASC) VISIBLE,
    INDEX fk_quote_user1_idx (snowflake_author ASC) VISIBLE,
    INDEX fk_quote_guild1_idx (snowflake_guild ASC) VISIBLE,
    UNIQUE INDEX snowflake_guild_quote_hash_UNIQUE (snowflake_guild ASC, quote_hash ASC) VISIBLE,
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

CREATE TABLE IF NOT EXISTS message (
	snowflake_message BIGINT NOT NULL,
	snowflake_channel BIGINT NOT NULL,
	message TEXT NOT NULL,
	PRIMARY KEY (snowflake_message),
	UNIQUE INDEX snowflake_messages_UNIQUE (snowflake_message ASC) VISIBLE
);

CREATE TABLE IF NOT EXISTS emoji (
	id_emoji BIGINT NOT NULL AUTO_INCREMENT,
	emoji VARCHAR(100) NOT NULL,
	PRIMARY KEY (id_emoji),
	UNIQUE INDEX id_emoji_UNIQUE (id_emoji ASC) VISIBLE
);

CREATE TABLE IF NOT EXISTS reaction (
	snowflake_message BIGINT NOT NULL,
	snowflake_author BIGINT NOT NULL,
	id_emoji BIGINT NOT NULL,
	INDEX fk_reaction_message1_idx (snowflake_message ASC) VISIBLE,
	INDEX fk_reaction_user1_idx (snowflake_author ASC) VISIBLE,
	PRIMARY KEY (id_emoji, snowflake_message, snowflake_author),
	CONSTRAINT fk_reaction_message1 FOREIGN KEY (snowflake_message) REFERENCES message (snowflake_message),
	CONSTRAINT fk_reaction_user1 FOREIGN KEY (snowflake_author) REFERENCES user (snowflake_user),
	CONSTRAINT fk_reaction_emoji1 FOREIGN KEY (id_emoji) REFERENCES emoji (id_emoji)
);

CREATE TABLE IF NOT EXISTS guild_emoji (
	id_emoji BIGINT NOT NULL,
	snowflake_guild BIGINT NOT NULL,
	snowflake_emoji BIGINT NOT NULL,
	animated TINYINT(1) NOT NULL DEFAULT 0,
	UNIQUE INDEX snowflake_emoji_UNIQUE (snowflake_emoji ASC) VISIBLE,
	INDEX fk_guild_emoji_emoji1_idx (id_emoji ASC) VISIBLE,
	INDEX fk_guild_emoji_guild1_idx (snowflake_guild ASC) VISIBLE,
	PRIMARY KEY (id_emoji, snowflake_guild),
	CONSTRAINT fk_guild_emoji_emoji1 FOREIGN KEY (id_emoji) REFERENCES emoji (id_emoji),
	CONSTRAINT fk_guild_emoji_guild1 FOREIGN KEY (snowflake_guild) REFERENCES guild (snowflake_guild)
);
