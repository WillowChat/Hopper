create table Logins
(
  id INTEGER
    primary key
  autoincrement,
  userid VARCHAR(32) not null,
  username VARCHAR(32) not null,
  password TEXT not null
)
;

create unique index Logins_userid_unique
  on Logins (userid)
;

create unique index Logins_username_unique
  on Logins (username)
;

create table Sessions
(
  id INTEGER
    primary key
  autoincrement,
  userid VARCHAR(32) not null,
  token VARCHAR(32) not null
)
;

