package name.wangzhen;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpiderFangCOM {

    Map<String, String[]> citiesMap = new HashMap<>();
    List<String[]> citiesSortList=new ArrayList<>();

    String cityPageCountFileName="CityPageCount.csv";

    SpiderFangCOM(){

    }

    public String getCityPage() throws Exception {


        Document cityDoc = Jsoup.connect("https://zu.fang.com/cities.aspx").get();
        String cityHtml = cityDoc.body().html();

        //FileUtils.saveFile("cities.html",bodyHtml);
        //System.out.println("保存html文件");
        //TaskMap.setTaskStatus("HomeTask",true);

        return cityHtml;
    }

    public Map<String, String[]> crawlLetterMap(String cityHtml) throws Exception {

        Document cityDoc=Jsoup.parse(cityHtml);
        Element letterTableEle = cityDoc.body().getElementById("c01");
        Elements letterEleList = letterTableEle.getElementsByTag("li");

        StringBuffer letterCsv = new StringBuffer();

        Map<String, String[]> letterMap = new HashMap<>();
        for(Element citiesTableEle:letterEleList){
            if(citiesTableEle.hasText()){
                Elements letterEle = citiesTableEle.getElementsByTag("strong");
                String letter = letterEle.first().text();

                Elements citiesListEle = citiesTableEle.getElementsByTag("a");
                for (Element cityEle:citiesListEle){
                    String cityName = cityEle.text();
                    String url = cityEle.attr("href");
                    letterMap.put(cityName,new String[]{letter,url});

                }
            }
        }
        return letterMap;
    }

    public Map<String, String[]> crawlProvinceMap(String cityHtml) throws Exception {
        Document cityDoc=Jsoup.parse(cityHtml);
        Element provinceTableEle = cityDoc.body().getElementById("c02");
        Elements provinceEleList = provinceTableEle.getElementsByTag("li");

        StringBuffer provinceCsv = new StringBuffer();
        Map<String, String[]> provinceMap = new HashMap<>();
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

                }
            }
        }
        return provinceMap;
    }

    public void crawlCitiesUrlMap() throws Exception {
        String cityHtml = getCityPage();
        Map<String, String[]> letterMap = crawlLetterMap(cityHtml);
        Set<String> letterSet = letterMap.keySet();
        Map<String, String[]> provinceMap = crawlProvinceMap(cityHtml);
        Set<String> provinceSet = provinceMap.keySet();
        HashSet<String> mergeSet = new HashSet<String>();
        mergeSet.addAll(letterSet);
        mergeSet.addAll(provinceSet);

        StringBuffer citiesCsv = new StringBuffer();
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
            citiesMap.put(cityName,new String[]{letter,letterUrl,province,provinceUrl});
            citiesCsv.append(cityName+","+letter+","+letterUrl+","+province+","+provinceUrl+"\n");
        }
        FileUtils.saveFile("Cities.csv",citiesCsv.toString());
        System.out.println("保存城市列表文件");
    }

    public void crawlCityPageCount() throws Exception {
        String cityPageCountStr = FileUtils.getFile(cityPageCountFileName);
        String[] citiesPageCount = cityPageCountStr.toString().split("\n");
        ArrayList<String[]> citiesList = new ArrayList<>();
        for (String cityPageCount:citiesPageCount){
            String[] cityItem = cityPageCount.split(",");
            citiesList.add(cityItem);
        }

        Map<String, String[]> citiesTmpMap = new HashMap<>();
        citiesTmpMap.putAll(citiesMap);

        for (String[] city:citiesList){
            citiesTmpMap.remove(city[0]);
        }
        int citiesMapCount = citiesTmpMap.size();

        String savePath=FileUtils.getSavePath();
        File f = new File(savePath+"/"+cityPageCountFileName);
        FileOutputStream fop = new FileOutputStream(f,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");

        int i=1;
        for (Map.Entry<String, String[]> city:citiesTmpMap.entrySet()){
            String cityName = city.getKey();
            String[] cityValue = city.getValue();

            String url="";
            if(!cityValue[1].equals("")){
                url= cityValue[1];
            }else if(!cityValue[3].equals("")){
                url= cityValue[3];
            }

            Document doc = null;
            int j=1;
            String pageCount="0";
            while (doc==null&&j<5){
                j++;
                try {
                    doc = Jsoup.connect("https:"+url).get();
                    Element pageCountEle = doc.selectFirst("span.txt");
                    String pageCountStr = pageCountEle.text();
                    pageCount = Pattern.compile("[^0-9]").matcher(pageCountStr).replaceAll("");
                } catch (Exception e){
                    doc=null;
                }
            }
            writer.append(cityName+","+pageCount+"\n");
            writer.flush();
            System.out.println("按城市抓取页数,进度:"+i+"/"+citiesMapCount+" "+cityName+" 共"+pageCount+"页");
            i++;
        }

        writer.close();
        fop.close();
    }

    public void sortCityPage() throws Exception {
        String savePath=FileUtils.getSavePath();
        String cityPageCountStr = FileUtils.getFile(cityPageCountFileName);

        String[] citiesPageCount = cityPageCountStr.toString().split("\n");
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
        String[] cityValue = citiesMap.get(cityName);

        String url="";
        if(!cityValue[1].equals("")){
            url= cityValue[1];
        }else if(!cityValue[3].equals("")){
            url= cityValue[3];
        }

        String savePath = FileUtils.getSavePath();
        File f = new File(savePath+"/CityPageNum.csv");
        FileOutputStream fop = new FileOutputStream(f,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop, "UTF-8");

        try {
            Document doc = Jsoup.connect("https:" + url + "house/i3" + pageNum + "/?rfss=1-28051d9280b13a8759-3b").get();
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


        int sumPageNum = 0;
        for (String[] city:citiesSortList){
            Integer cityPageCount = Integer.valueOf(city[1]);
            sumPageNum+=cityPageCount;
        }

        int j=1;
        for (String[] city:citiesSortList){

            String cityName = city[0];

            Integer cityPageCount = Integer.valueOf(city[1]);
            for (int i=1;i<=cityPageCount;i++){
                this.crawlCityPageNum(cityName,i);
                System.out.println("按页数抓取,进度:"+j+"/"+sumPageNum);
                j++;
                try { Thread.sleep(10*1000); } catch (InterruptedException e) { e.printStackTrace(); }
            }

            j++;
        }


    }

    public static void main(String[] args) throws Exception {
        System.out.println("程序启动");

        SpiderFangCOM spiderFangCOM=new SpiderFangCOM();

        //合并字母和省份分城市列表
        spiderFangCOM.crawlCitiesUrlMap();

        spiderFangCOM.crawlCityPageCount();

        spiderFangCOM.sortCityPage();
        spiderFangCOM.crawlCityPage();
    }
}
