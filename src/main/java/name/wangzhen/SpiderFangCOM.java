package name.wangzhen;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

public class SpiderFangCOM {

    Document doc=null;
    String savePath="save";
    String classPath="";
    Map<String, String[]> letterMap = new HashMap<>();
    Map<String, String[]> provinceMap = new HashMap<>();
    Map<String, String[]> mergeMap = new HashMap<>();

    public void init() throws Exception {
        classPath = ClassLoader.getSystemResource("").getPath();
        savePath=classPath+"save";
        new File(savePath).mkdir();

        doc = Jsoup.connect("https://zu.fang.com/cities.aspx").get();
        String bodyHtml = doc.body().html();
        File citiesf = new File(savePath+"/cities.html");
        FileOutputStream citiesfop = new FileOutputStream(citiesf);
        OutputStreamWriter citiesWriter = new OutputStreamWriter(citiesfop, "UTF-8");
        citiesWriter.append(bodyHtml);
        citiesWriter.close();
        citiesfop.close();
        System.out.println("保存html文件");
    }

    public void crawlLetter() throws Exception {
        Element letterTableEle = doc.body().getElementById("c01");
        Elements letterEleList = letterTableEle.getElementsByTag("li");

        StringBuffer letterCsv = new StringBuffer();

        for(Element citiesTableEle:letterEleList){
            if(citiesTableEle.hasText()){
                Elements letterEle = citiesTableEle.getElementsByTag("strong");
                String letter = letterEle.first().text();

                Elements citiesListEle = citiesTableEle.getElementsByTag("a");
                for (Element cityEle:citiesListEle){
                    String cityName = cityEle.text();
                    String url = cityEle.attr("href");
                    letterMap.put(cityName,new String[]{letter,url});
                    letterCsv.append(cityName+","+letter+","+url+"\n");
                }
            }
        }
        File f = new File(savePath+"/letter.csv");
        FileOutputStream fop = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
        writer.append(letterCsv);
        writer.close();
        fop.close();
        System.out.println("保存字母城市文件");
    }

    public void crawlProvince() throws Exception {
        Element provinceTableEle = doc.body().getElementById("c02");
        Elements provinceEleList = provinceTableEle.getElementsByTag("li");

        StringBuffer provinceCsv = new StringBuffer();
        for(Element citiesTableEle:provinceEleList){
            if(citiesTableEle.hasText()){
                Elements municipalitiesEle = citiesTableEle.getElementsByTag("b");
                Elements provinceEle = citiesTableEle.getElementsByTag("strong");
                String province = "";
                if(municipalitiesEle.size()!=0){
                    province = municipalitiesEle.first().text();
                }else if(provinceEle.size()!=0){
                    province = provinceEle.first().text();
                }


                Elements citiesListEle = citiesTableEle.getElementsByTag("a");
                for (Element cityEle:citiesListEle){
                    String cityName = cityEle.text();
                    String url = cityEle.attr("href");
                    provinceMap.put(cityName,new String[]{province,url});
                    provinceCsv.append(cityName+","+province+","+url+"\n");
                }
            }
        }
        File f = new File(savePath+"/province.csv");
        FileOutputStream fop = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
        writer.append(provinceCsv);
        writer.close();
        fop.close();
        System.out.println("保存省份城市文件");
    }

    public void mergeCitiesMap() throws Exception {
        Set<String> letterSet = letterMap.keySet();
        Set<String> provinceSet = provinceMap.keySet();
        HashSet<String> mergeSet = new HashSet<String>();
        mergeSet.addAll(letterSet);
        mergeSet.addAll(provinceSet);

        mergeSet.remove("台湾");

        StringBuffer mergeCsv = new StringBuffer();
        for(String cityName:mergeSet){
            String[] letterValue = letterMap.get(cityName);
            String[] provinceValue = provinceMap.get(cityName);
            String letter="";
            String letterUrl="";
            if (letterValue!=null){
                letter=letterValue[0];
                letterUrl=letterValue[1];
            }

            String province="";
            String provinceUrl="";
            if (provinceValue!=null){
                province=provinceValue[0];
                provinceUrl=provinceValue[1];
            }

            mergeMap.put(cityName,new String[]{letter,letterUrl,province,provinceUrl});
            mergeCsv.append(cityName+","+letter+","+letterUrl+","+province+","+provinceUrl+"\n");
        }
        File f = new File(savePath+"/merge.csv");
        FileOutputStream fop = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");
        writer.append(mergeCsv);
        writer.close();
        fop.close();
        System.out.println("保存合并字母和省份分城市列表文件");
    }

    public void crawlCity() throws Exception {
        Map<String, String[]> tmpMap = new HashMap<>();
        tmpMap.putAll(mergeMap);
        mergeMap.clear();
        int i=1;
        for (Map.Entry<String, String[]> Entry:tmpMap.entrySet()){
            String key = Entry.getKey();
            String[] value = Entry.getValue();
            System.out.print("进度:"+i+"/"+tmpMap.size()+" "+key+" ");
            i++;

            String url="";
            if(!value[1].equals("")){
                url= value[1];
            }else if(!value[3].equals("")){
                url= value[3];
            }

            doc=null;
            try {
                doc = Jsoup.connect("https:"+url).get();
                Element pageCountEle = doc.selectFirst("span.txt");
                String pageCountStr = pageCountEle.text();
                String pageCount = Pattern.compile("[^0-9]").matcher(pageCountStr).replaceAll("");
                //ArrayList<String> cityValue = new ArrayList(Collections.singleton(value));
                //cityValue.add(pageCount);
                //mergeMap.put(key, (String[]) cityValue.toArray());
                System.out.println("共"+pageCount+"页");
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("程序启动");

        SpiderFangCOM spiderFangCOM=new SpiderFangCOM();
        //初始化
        spiderFangCOM.init();
        //保存按字母分城市列表
        spiderFangCOM.crawlLetter();
        //保存按省份分城市列表
        spiderFangCOM.crawlProvince();

        //合并字母和省份分城市列表
        spiderFangCOM.mergeCitiesMap();

        spiderFangCOM.crawlCity();
    }
}
