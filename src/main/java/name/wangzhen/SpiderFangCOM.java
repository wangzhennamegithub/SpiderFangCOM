package name.wangzhen;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpiderFangCOM {

    Document doc=null;
    String savePath="save";
    String classPath="";
    Map<String, String[]> letterMap = new HashMap<>();
    Map<String, String[]> provinceMap = new HashMap<>();
    Map<String, String[]> mergeMap = new HashMap<>();
    List<String[]> citiesSortList=new ArrayList<>();

    SpiderFangCOM(){
        URL resUrl = ClassLoader.getSystemResource("");
        if(resUrl==null){
            savePath="save";
        }else{
            classPath=resUrl.getPath();
            savePath=classPath+"save";
        }

        new File(savePath).mkdir();
    }

    public void init() throws Exception {

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

    public void crawlCityPageCount() throws Exception {
        Map<String, String[]> tmpMap = new HashMap<>();
        tmpMap.putAll(mergeMap);
        mergeMap.clear();

        File f = new File(savePath+"/CityPageCount.csv");
        FileOutputStream fop = new FileOutputStream(f);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");

        int i=1;
        for (Map.Entry<String, String[]> Entry:tmpMap.entrySet()){
            String key = Entry.getKey();
            String[] value = Entry.getValue();
            System.out.print("按城市抓取,进度:"+i+"/"+tmpMap.size()+" "+key+" ");
            writer.append(key+",");
            writer.flush();

            i++;

            String url="";
            if(!value[1].equals("")){
                url= value[1];
            }else if(!value[3].equals("")){
                url= value[3];
            }

            doc=null;
            int j=1;
            while (doc==null&&j<5){
                j++;
                try {
                    doc = Jsoup.connect("https:"+url).get();
                    Element pageCountEle = doc.selectFirst("span.txt");
                    String pageCountStr = pageCountEle.text();
                    String pageCount = Pattern.compile("[^0-9]").matcher(pageCountStr).replaceAll("");
                    ArrayList<String> cityValue = new ArrayList();
                    Collections.addAll(cityValue, value);
                    //ArrayList<String> cityValue = new ArrayList(Collections.singleton(value));
                    cityValue.add(pageCount);
                    String[] cityArr = new String[cityValue.size()];
                    cityValue.toArray(cityArr);
                    mergeMap.put(key, cityArr );
                    writer.append(pageCount);
                    System.out.print("共"+pageCount+"页");
                } catch (Exception e){
                    doc=null;
                }
            }
            System.out.println();
            writer.append("\n");
            writer.flush();

        }

        writer.close();
        fop.close();
    }

    public void sortCityPage() throws Exception {
        Reader in = new InputStreamReader(new FileInputStream(savePath+"/CityPageCount.csv"),"UTF-8");
        byte[] bytes = new byte[1024];
        int len = -1;
        StringBuffer sb = new StringBuffer();
        while ((len = in.read()) != -1) {
            sb.append((char)len);
        }
        in.close();

        String[] citiesPageCount = sb.toString().split("\n");
        ArrayList<String[]> citiesList = new ArrayList<>();
        for (String cityPageCount:citiesPageCount){
            String[] cityItem = cityPageCount.split(",");
            citiesList.add(cityItem);
        }
        List<String[]> citiesSortTmpList = citiesList.stream().sorted((x, y) -> {
            int a = 0;
            if (x.length >= 2) {
                a = Integer.valueOf(String.valueOf(x[1]));
            }

            int b = 0;
            if (y.length >= 2) {
                b = Integer.valueOf(String.valueOf(y[1]));
            }

            return b - a;
        }).collect(Collectors.toList());
        citiesSortList.addAll(citiesSortTmpList);
    }

    public void crawlCityPageNum(String cityName, int pageNum) throws Exception {
        String[] cityValue = mergeMap.get(cityName);

        String url="";
        if(!cityValue[1].equals("")){
            url= cityValue[1];
        }else if(!cityValue[3].equals("")){
            url= cityValue[3];
        }

        File f = new File(savePath+"/CityPageNum.csv");
        FileOutputStream fop = new FileOutputStream(f,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");

        try {
            doc = Jsoup.connect("https:"+url+"house/i3"+pageNum+"/?rfss=1-28051d9280b13a8759-3b").get();
            Element houseTableEle = doc.body().getElementsByClass("houseList").first();
            Elements houseListEle = houseTableEle.getElementsByTag("dl");
            for (Element houseEle:houseListEle){
                Element houseInfoEle = houseEle.getElementsByTag("dd").first();
                String detailsUrl = houseInfoEle.child(0).getElementsByTag("a").first().attr("href");
                String title = houseInfoEle.child(0).text();
                String[] houseInfo = houseInfoEle.child(1).text().split("\\|");
                String mode = "";if(houseInfo.length>=1){mode=houseInfo[0];}
                String houseType = "";if(houseInfo.length>=2){houseType=houseInfo[1];}
                String areaSize = "";if(houseInfo.length>=3){areaSize=houseInfo[2];}
                String orientation = "";if(houseInfo.length>=4){orientation=houseInfo[3];}
                Elements busInfoEle = houseInfoEle.child(2).getElementsByTag("a");

                String startBusStation = "";if(busInfoEle.size()>=1){startBusStation=busInfoEle.get(0).text();}
                String startBusStationUrl = "";if(busInfoEle.size()>=1){startBusStationUrl=busInfoEle.get(0).attr("href");}
                String endBusStation = "";if(busInfoEle.size()>=2){endBusStation=busInfoEle.get(1).text();}
                String endBusStationUrl = "";if(busInfoEle.size()>=2){endBusStationUrl=busInfoEle.get(1).attr("href");}
                String busStation = "";if(busInfoEle.size()>=3){busStation=busInfoEle.get(2).text();}
                String busStationUrl = "";if(busInfoEle.size()>=3){busStationUrl=busInfoEle.get(2).attr("href");}
                Element subwayInfoEle = houseInfoEle.child(3);
                String subwayInfo = "";
                String subwayName = "";
                String subwayUrl = "";
                if (subwayInfoEle.hasText()){
                    subwayInfo = houseInfoEle.child(3)
                            .getElementsByTag("span")
                            .first()
                            .text();
                    Element subwayEle = houseInfoEle.child(3).getElementsByTag("span").first().getElementsByTag("a").first();
                    subwayName = subwayEle.text();
                    subwayUrl = subwayEle.attr("href");
                }


                String price = houseInfoEle.child(6).text();
                String writerStr = cityName + "\t" + title + "\t" + detailsUrl + "\t" + mode + "\t" + houseType + "\t" + areaSize + "\t" + orientation
                        + "\t" + startBusStation + "\t" + startBusStationUrl + "\t" + endBusStation + "\t" + endBusStationUrl + "\t" + busStation + "\t" + busStationUrl
                        + "\t" + subwayName + "\t" + subwayUrl + "\t" + subwayInfo
                        + "\t" + price;
                //System.out.println(writerStr);
                writer.append(writerStr+"\n");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        writer.close();
        fop.close();
    }

    public void crawlCityPage() throws Exception {
        long cityCount = citiesSortList.size();
        int j=1;
        for (String[] city:citiesSortList){
            if(city.length<2){
                continue;
            }
            String cityName = city[0];

            Integer cityPageCount = Integer.valueOf(city[1]);
            for (int i=1;i<=cityPageCount;i++){
                this.crawlCityPageNum(cityName,i);
                System.out.println("按页数抓取,进度:"+j+"/"+cityCount+" "+i+"/"+cityPageCount);
                try { Thread.sleep(10*1000); } catch (InterruptedException e) { e.printStackTrace(); }
            }

            j++;
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

        spiderFangCOM.crawlCityPageCount();

        spiderFangCOM.sortCityPage();
        spiderFangCOM.crawlCityPage();
    }
}
