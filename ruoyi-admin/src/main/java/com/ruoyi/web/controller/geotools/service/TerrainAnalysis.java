package com.ruoyi.web.controller.geotools.service;

import com.alibaba.fastjson.JSON;
import com.ruoyi.web.controller.geotools.CommonMethod;
import com.ruoyi.web.controller.geotools.TopographyAlgorithm;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.process.raster.ContourProcess;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.Coverage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yyx
 * @date 2022/8/16 - 上午 9:14
 */
public class TerrainAnalysis {
    public static String demPath = "C:\\Users\\dell\\Desktop\\geotools-data\\安徽省_高程_Level_13.tif";
    public static String clippedTiff = "C:\\Users\\dell\\Desktop\\geotools-data\\clippedTiff.tif";

    /**
     * 提取等高线
     *
     * @param geoJson
     * @param interval
     * @return
     * @throws IOException
     */
    public static String calContour(String geoJson, Double interval) throws IOException {
        long start = System.currentTimeMillis();
        System.out.println("开始提取等高线...");
        Coverage coverage = CommonMethod.clipByGeoJson(geoJson, TerrainAnalysis.demPath);
        GridCoverage2D gridCoverage2D = (GridCoverage2D) coverage;
        SimpleFeatureCollection result = ContourProcess.process(gridCoverage2D, null, null, interval, null, null, null, null);
        String geoJSON = GeoJSONWriter.toGeoJSON(result);
        long end = System.currentTimeMillis();
        System.out.println("提取成功，用时" + (end - start) + "毫秒...");
        return geoJSON;
    }

    /**
     * 坡度分析
     *
     * @param geoJson
     * @return
     * @throws IOException
     * @throws SchemaException
     */
    public static String calSlope(String geoJson) throws IOException, SchemaException {
        long start = System.currentTimeMillis();
        System.out.println("开始坡度分析...");
        String inputTiff = TerrainAnalysis.demPath;
        String clippedTiff = TerrainAnalysis.clippedTiff;

        CommonMethod.clipByGeoJson(geoJson, inputTiff, clippedTiff);
        Map tiffMap = CommonMethod.readTiffFile(clippedTiff);
        double cellSize = (double) tiffMap.get("cellSize");
        GridCoverage2D coverage = (GridCoverage2D) tiffMap.get("coverage");
        Raster sourceRaster = (Raster) tiffMap.get("raster");

        TopographyAlgorithm topographyAlgorithm = new TopographyAlgorithm(coverage, sourceRaster, (float) cellSize);
        String result = topographyAlgorithm.calculateSlope();

        long end = System.currentTimeMillis();
        System.out.println("提取成功，用时" + (end - start) + "毫秒...");
        return result;
    }

    /**
     * 坡向分析
     *
     * @param geoJson
     * @return
     * @throws IOException
     * @throws SchemaException
     */
    public static String calAspect(String geoJson) throws IOException, SchemaException {
        long start = System.currentTimeMillis();
        System.out.println("开始坡向分析...");
        String inputTiff = TerrainAnalysis.demPath;
        String clippedTiff = TerrainAnalysis.clippedTiff;

        CommonMethod.clipByGeoJson(geoJson, inputTiff, clippedTiff);
        Map tiffMap = CommonMethod.readTiffFile(clippedTiff);
        double cellSize = (double) tiffMap.get("cellSize");
        GridCoverage2D coverage = (GridCoverage2D) tiffMap.get("coverage");
        Raster sourceRaster = (Raster) tiffMap.get("raster");

        TopographyAlgorithm topographyAlgorithm = new TopographyAlgorithm(coverage, sourceRaster, (float) cellSize);
        String result = topographyAlgorithm.calculateAspect();

        long end = System.currentTimeMillis();
        System.out.println("提取成功，用时" + (end - start) + "毫秒...");
        return result;
    }

    /**
     * 提取地形因子
     *
     * @param lon
     * @param lat
     * @return
     * @throws IOException
     * @throws TransformException
     */
    public static String getTerrainFactors(double lon, double lat) throws IOException, TransformException {
        long start = System.currentTimeMillis();
        System.out.println("开始提取地形因子...");
        Map tiffMap = CommonMethod.readTiffFile(TerrainAnalysis.demPath);
        double cellSize = (double) tiffMap.get("cellSize");
        GridCoverage2D coverage = (GridCoverage2D) tiffMap.get("coverage");
        Raster sourceRaster = (Raster) tiffMap.get("raster");
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) tiffMap.get("crs");
        TopographyAlgorithm topographyAlgorithm = new TopographyAlgorithm(coverage, sourceRaster, (float) cellSize);
        DirectPosition2D position = new DirectPosition2D(crs, lon, lat);
        Point2D point2D = coverage.getGridGeometry().worldToGrid(position);
        int colNo = (int) point2D.getX();
        int rowNo = (int) point2D.getY();
        float slope = topographyAlgorithm.cellSlope(colNo, rowNo);
        float aspect = topographyAlgorithm.cellAspect(colNo, rowNo);
        Object evaluate = coverage.evaluate(position);
        float[] dem = (float[]) evaluate;
        Map map = new HashMap();
        map.put("dem", dem[0]);
        map.put("slope", slope);
        map.put("aspect", aspect);
        long end = System.currentTimeMillis();
        System.out.println("提取成功，用时" + (end - start) + "毫秒...");
        return JSON.toJSONString(map);
    }

    /**
     * 视域-通视分析
     *
     * @param point
     * @param viewAreaGeoJson
     * @return
     * @throws IOException
     * @throws SchemaException
     * @throws TransformException
     */
    public static String viewShed(double[] point, String viewAreaGeoJson) throws IOException, SchemaException, TransformException {
        long start = System.currentTimeMillis();
        System.out.println("开始视域通视分析...");
        String inputTiff = TerrainAnalysis.demPath;
        String clippedTiff = TerrainAnalysis.clippedTiff;

        CommonMethod.clipByGeoJson(viewAreaGeoJson, inputTiff, clippedTiff);
        Map tiffMap = CommonMethod.readTiffFile(clippedTiff);
        double cellSize = (double) tiffMap.get("cellSize");
        GridCoverage2D coverage = (GridCoverage2D) tiffMap.get("coverage");
        Raster sourceRaster = (Raster) tiffMap.get("raster");

        TopographyAlgorithm topographyAlgorithm = new TopographyAlgorithm(coverage, sourceRaster, (float) cellSize);
        String result = topographyAlgorithm.calculateViewShed(point);

        long end = System.currentTimeMillis();
        System.out.println("提取成功，用时" + (end - start) + "毫秒...");
        return result;
    }

    /**
     * 视线-通视分析
     * @param coordinates
     * @return
     * @throws IOException
     * @throws SchemaException
     */
    public static String lineOfSign(String coordinates) throws IOException, SchemaException {
        long start = System.currentTimeMillis();
        System.out.println("开始视线通视分析...");
        Map tiffMap = CommonMethod.readTiffFile(TerrainAnalysis.demPath);
        double cellSize = (double) tiffMap.get("cellSize");
        GridCoverage2D coverage = (GridCoverage2D) tiffMap.get("coverage");
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) tiffMap.get("crs");

        String[] arr = coordinates.split(",");
        double[][] points = new double[2][2];
        // 第一个点
        points[0][0] = Double.parseDouble(arr[0]);
        points[0][1] = Double.parseDouble(arr[1]);
        // 第二个点
        points[1][0] = Double.parseDouble(arr[2]);
        points[1][1] = Double.parseDouble(arr[3]);
        // 视线长度
        double distance = CommonMethod.getDistance(points);
        // 拆分长度
        double dis_x = Math.abs(points[0][0] - points[1][0]);
        double dis_y = Math.abs(points[0][1] - points[1][1]);
        double max = Math.max(dis_x, dis_y);
        int m = (int) (max / cellSize);
        // 拆分段数
        int segments = m == 0 ? (int) distance : (int) (distance / m);
        List<double[]> pointList = CommonMethod.splitLine(points[0],points[1],segments);
        // 拆分点集合 带高程
        List<Map> list =new ArrayList<>();
        for (int i = 0; i < pointList.size(); i++) {
            Map map= new HashMap();
            map.put("geometry",pointList.get(i));
            // 获取点高程
            DirectPosition2D position =new DirectPosition2D(crs,pointList.get(i)[0],pointList.get(i)[1]);
            Object evaluate =coverage.evaluate(position);
            float[] dem =(float[]) evaluate;
            map.put("gridCode",String.valueOf(dem[0]));
            list.add(map);
        }
        // 去除相等高程的点数据
        List<Map> resultList =CommonMethod.removeRepeatMapByKey(list,"gridCode");
        // 找出视线开始一个高程峰值
        int peakIndex =CommonMethod.findPeakIndex(resultList,"gridCode");
        // 1-绿色可视 2-红色不可视
        List<Map> lineList =new ArrayList<>();
        if(peakIndex == resultList.size()-1){
            Map map =new HashMap();
            Geometry lineString= CommonMethod.createLineString(points);
            map.put("geometry",lineString);
            map.put("visCode",1);
            lineList.add(map);
            String geoJson = CommonMethod.createGeoJson(lineList,"LineString","visCode");
            long end = System.currentTimeMillis();
            System.out.println("提取成功，用时" + (end - start) + "毫秒...");
            return geoJson;
        }
        for (int i = 0; i < resultList.size(); i++) {
            if(i==peakIndex){
                Map map =new HashMap();
                double[][] coords1=new double[2][2];
                coords1[0] =(double[]) resultList.get(0).get("geometry");
                coords1[1] =(double[]) resultList.get(i).get("geometry");
                Geometry lineString1= CommonMethod.createLineString(coords1);
                map.put("geometry",lineString1);
                map.put("visCode",1);
                lineList.add(map);
                Map map1 =new HashMap();
                double[][] coords2=new double[2][2];
                coords2[0] =(double[]) resultList.get(i).get("geometry");
                coords2[1] =(double[]) resultList.get(resultList.size()-1).get("geometry");
                Geometry lineString2= CommonMethod.createLineString(coords2);
                map1.put("geometry",lineString2);
                map1.put("visCode",2);
                lineList.add(map1);
            }
        }
        String geoJson = CommonMethod.createGeoJson(lineList,"LineString","visCode");
        long end = System.currentTimeMillis();
        System.out.println("提取成功，用时" + (end - start) + "毫秒...");
        return geoJson;
    }
}
