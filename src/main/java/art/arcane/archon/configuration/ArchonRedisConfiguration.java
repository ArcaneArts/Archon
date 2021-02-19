package art.arcane.archon.configuration;

import lombok.Data;

@Data
public class ArchonRedisConfiguration
{
    private String address = "localhost";
    private int port = 8123;
    private int database = 0;
    private String password = "apassword";
    private boolean usePassword = true;
}
