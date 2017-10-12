create table Connections
(
  id INTEGER
    primary key
  autoincrement,
  userid VARCHAR(32) not null
    constraint Connections_Logins_userid_fk
    references Logins (userid),
  connectionid VARCHAR(32) not null,
  host VARCHAR(255) not null,
  port INTEGER not null,
  tls BOOLEAN not null,
  nick VARCHAR(32) not null
)
;

create unique index Connections_id_uindex
  on Connections (id)
;

