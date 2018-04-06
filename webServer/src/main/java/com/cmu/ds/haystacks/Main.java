import org.rapidoid.config.Conf;
import org.rapidoid.setup.On;
import org.rapidoid.setup.App;

public class Main {

    public static void main(String[] args) throws Exception {
        App.run(args);
        Conf.HTTP.set("maxPipeline", 128);
        Conf.HTTP.set("timeout", 0);

        On.get("/").managed(false).cacheTTL(6000).plain((req) ->
            "Hello haystack"
        );
    }
}

