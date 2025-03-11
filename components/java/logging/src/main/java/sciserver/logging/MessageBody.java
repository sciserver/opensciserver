package sciserver.logging;
import java.util.Hashtable;

public abstract class MessageBody {

    public String $type = "";
    public Hashtable<String, String> Properties = new Hashtable<String,String>(){{
            put("$type", "System.Collections.Generic.Dictionary`2[[System.String, mscorlib],[System.String, mscorlib]], mscorlib");
        }};

}
