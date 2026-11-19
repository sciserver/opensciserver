package org.sciserver.sciquery.metadata;

import java.util.List;
import java.util.Properties;

public class MetadataManagerTest {
    
    public static void main(String[] args) throws Exception {
        long t0=System.currentTimeMillis();
        String token=args[0];
        String stoken=args[1];
        String racmEndpoint = args[2];
        String cajobsUUID = args[3];

        MetadataManager mm = new MetadataManager(racmEndpoint,stoken, cajobsUUID, null);
        try {
            List<SciQueryPrivileges>privs = mm.sciQueryPrivilegesForUser(token);
            System.out.println(privs);
            
            CasJobsPrivileges cj = mm.casjobsPrivilegesForUser(token);
            System.out.println(cj);

            long t1=System.currentTimeMillis();
            System.out.printf("Duration %d\n",(t1-t0));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
