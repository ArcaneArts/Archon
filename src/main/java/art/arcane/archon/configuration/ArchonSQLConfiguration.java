package art.arcane.archon.configuration;

import lombok.Data;

@Data
public class ArchonSQLConfiguration
{
    private String database = "adatabase";
    private String address = "localhost";
    private String username = "ausername";
    private String password = "apassword";
    private String driver = "com.mysql.cj.jdbc.Driver";
    private int port = 3306;
    private boolean readOnly = false;
    private boolean usesPassword = true;
}
