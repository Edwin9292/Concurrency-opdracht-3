package nl.saxion.concurrency;


public class ArgumentUtil {

    public static String getPort(String[] args) {
        String port = ArgumentUtil.get("-port",args);
        try {
            Integer.parseInt(port);
        } catch (Exception e) {
            return "8080";
        }
        return port;
    }

    private static String get(String prefix,String[] args) {
            for(int i  = 0; i < args.length;i++) {
                if (args[i].toLowerCase().trim().equals(prefix)) {
                    if (i < args.length)  return args[i + 1];
                }
            }
            return "";
    }

    public static String getAkkaPort(String[] args) {
        String port = ArgumentUtil.get("-akkaport",args);
        try {
            Integer.parseInt(port);
        } catch (Exception e) {
            return "0";
        }
        return port;
    }

}
