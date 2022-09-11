package utils;

import java.io.*;
import java.util.Properties;
public class PropertiesReader {
    public static Properties readProperties(String url){
        Properties prop=new Properties();
        try{
            InputStream in=new BufferedInputStream(new FileInputStream(url));
            prop.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return prop;
    }
}
