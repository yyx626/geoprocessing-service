# Geoprocessing-service

## Step1

<img src="C:\Users\dell\AppData\Roaming\Typora\typora-user-images\image-20220817112620275.png" alt="image-20220817112620275" style="zoom: 50%;" />

+ run quartz.sql & ry_20210924.sql in MySQL
+ update url, username, and password in master druid in the file(application-durid.yml)

## Step2

+ Update the file: 

  + ruoyi-admin/src/main/java/com/ruoyi/web/controller/geotools/service/TerrainAnalysis.java 

  ```java
  public static String demPath = "C:\\Users\\dell\\Desktop\\geotools-data\\安徽省_高程_Level_13.tif";
  public static String clippedTiff = "C:\\Users\\dell\\Desktop\\geotools-data\\clippedTiff.tif";
  ```

+ Choose to run or debug 'RuoYiApplication' to start the server.

+ http://localhost:8080/   admin  123456

## Step3

<img src="C:\Users\dell\AppData\Roaming\Typora\typora-user-images\image-20220817114728072.png" alt="image-20220817114728072" style="zoom: 50%;" />

+ you can test.



