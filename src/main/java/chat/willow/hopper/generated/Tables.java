/*
 * This file is generated by jOOQ.
*/
package chat.willow.hopper.generated;


import chat.willow.hopper.generated.tables.Connections;
import chat.willow.hopper.generated.tables.Logins;
import chat.willow.hopper.generated.tables.SchemaVersion;
import chat.willow.hopper.generated.tables.Sessions;
import chat.willow.hopper.generated.tables.SqliteSequence;

import javax.annotation.Generated;


/**
 * Convenience access to all tables in 
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>Connections</code>.
     */
    public static final Connections CONNECTIONS = chat.willow.hopper.generated.tables.Connections.CONNECTIONS;

    /**
     * The table <code>Logins</code>.
     */
    public static final Logins LOGINS = chat.willow.hopper.generated.tables.Logins.LOGINS;

    /**
     * The table <code>Sessions</code>.
     */
    public static final Sessions SESSIONS = chat.willow.hopper.generated.tables.Sessions.SESSIONS;

    /**
     * The table <code>schema_version</code>.
     */
    public static final SchemaVersion SCHEMA_VERSION = chat.willow.hopper.generated.tables.SchemaVersion.SCHEMA_VERSION;

    /**
     * The table <code>sqlite_sequence</code>.
     */
    public static final SqliteSequence SQLITE_SEQUENCE = chat.willow.hopper.generated.tables.SqliteSequence.SQLITE_SEQUENCE;
}
