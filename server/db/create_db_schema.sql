-- console chat DB schema for Postgres

CREATE SCHEMA console_chat
    AUTHORIZATION pg_database_owner;

CREATE TABLE console_chat.users
(
    id serial NOT NULL,
    login VARCHAR (100),
    password VARCHAR (100),
    username VARCHAR (100),
    role VARCHAR (100),
    PRIMARY KEY (id),
    UNIQUE (login), 
	UNIQUE (username)
);

INSERT INTO console_chat.users(login, password, username, role)
	VALUES ('admin', 'admin', 'admin1', 'ADMIN');
INSERT INTO console_chat.users(login, password, username, role)
	VALUES ('qwe', 'qwe', 'qwe1', 'USER');
INSERT INTO console_chat.users(login, password, username, role)
	VALUES ('asd', 'asd', 'asd1', 'USER');
INSERT INTO console_chat.users(login, password, username, role)
	VALUES ('zxc', 'zxc', 'zxc1', 'USER');

--select * from console_chat.users;