# initial

# --- !Ups

CREATE TABLE block (
    id serial NOT NULL,
    height int NOT NULL,
    hash char(64) NOT NULL, -- 256 bit hash
    parent_hash char(64) NOT NULL, -- 256 bit hash
    create_date timestamp NOT NULL,
    min_fee bigint NOT NULL,
    tx_count int NOT NULL,
    known_count int NOT NULL,
    size int NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX block_height_index ON block (height);
CREATE UNIQUE INDEX block_hash_idx ON block (hash);

CREATE TABLE tx (
    id serial NOT NULL,
    hash char(64) NOT NULL,
    fee bigint NOT NULL,
    abs_fee bigint NOT NULL,
    size int NOT NULL,
    create_date timestamp NOT NULL,
    mine_date timestamp NULL,
    PRIMARY KEY (id),
    UNIQUE (hash)
);

CREATE INDEX tx_mine_date_index ON tx (mine_date);

CREATE TABLE block_tx (
    block_id int NOT NULL,
    tx_id int NOT NULL,
    PRIMARY KEY (block_id, tx_id)
);

CREATE INDEX block_tx_tx_id_fkey_index ON block_tx (tx_id);
ALTER TABLE block_tx ADD CONSTRAINT block_tx_tx_id_fkey FOREIGN KEY (tx_id) REFERENCES tx (id);
ALTER TABLE block_tx ADD CONSTRAINT block_tx_block_id_fkey FOREIGN KEY (block_id) REFERENCES block (id);

CREATE TABLE prediction (
    hash char(64) NOT NULL,
    height int NOT NULL,
    delay_min int NOT NULL,
    delay_max int NOT NULL,
    minutes_min int NOT NULL,
    minutes_max int NOT NULL,
    create_date timestamp NOT NULL,
    PRIMARY KEY (hash)
);


# --- !Downs

DROP TABLE prediction;
DROP TABLE block_tx;
DROP TABLE tx;
DROP TABLE block;
