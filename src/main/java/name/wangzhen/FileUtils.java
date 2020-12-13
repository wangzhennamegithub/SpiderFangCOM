package name.wangzhen;

import java.io.*;
import java.net.URL;

public class FileUtils {



    public static String getSavePath(){
        String savePath="save";
        String classPath="";
        URL resUrl = ClassLoader.getSystemResource("");
        if(resUrl==null){
            savePath="save";
        }else{
            classPath=resUrl.getPath();
            savePath=classPath+"save";
        }
        return savePath;
    }

    public static void saveFile(String fileName, String fileContext)  {

        String savePath = getSavePath();
        if(!new File(savePath).exists()){
            new File(savePath).mkdir();
        }

        File f = new File(savePath+"/"+fileName);
        FileOutputStream fop = null;
        try {
            fop = new FileOutputStream(f);
            OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
            writer.append(fileContext);
            writer.close();
            fop.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static String getFile(String fileName){
        String savePath = getSavePath();
        StringBuffer sb = new StringBuffer();
        try {
            Reader in = new InputStreamReader(new FileInputStream(savePath+"/"+fileName),"UTF-8");
            byte[] bytes = new byte[1024];
            int len = -1;
            while ((len = in.read()) != -1) {
                sb.append((char)len);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("无法读取文件"+fileName);
        }
        return sb.toString();
    }
}
