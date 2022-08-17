package com.ruoyi.web.controller.geotools;


import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.*;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.algorithm.PointLocator;
import org.locationtech.jts.geom.*;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yyx
 * @date 2022/8/16 - 上午 10:25
 */
public class CommonMethod {

    /**
     * 读取 GeoJSON
     *
     * @param json
     * @return
     * @throws IOException
     */
    public static List<Geometry> readGeoJson(String json) throws IOException {
        GeoJSONReader geoJSONReader = new GeoJSONReader(json);
        SimpleFeatureCollection collection = geoJSONReader.getFeatures();
        FeatureIterator<SimpleFeature> iterator = collection.features();
        List<Geometry> all = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                all.add(geometry);
            }
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
        return all;
    }

    /**
     * 从shp中读取FeatureCollection
     *
     * @param shpPath
     * @return
     */
    public static SimpleFeatureCollection readFeatureCollection(String shpPath) {
        SimpleFeatureCollection featureCollection = null;
        File shpFile = new File(shpPath);
        try {
            ShapefileDataStore shapefileDataStore = new ShapefileDataStore(shpFile.toURI().toURL());
            // 设置编码，防止属性中的中文字符出现乱码
            shapefileDataStore.setCharset(Charset.forName("UTF-8"));
            // 这个typeName不传递，默认是文件名称
            FeatureSource featureSource = shapefileDataStore.getFeatureSource(shapefileDataStore.getTypeNames()[0]);
            featureCollection = (SimpleFeatureCollection) featureSource.getFeatures();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return featureCollection;
    }

    /**
     * 生成shp文件
     *
     * @param shpPath
     * @param collection
     * @throws IOException
     */
    public static void createShp(String shpPath, SimpleFeatureCollection collection) throws IOException {
        File shpFile = new File(shpPath);
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        SimpleFeatureType simpleFeatureType = collection.getSchema();
        // 创建shpstore需要的参数
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", shpFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(simpleFeatureType);
        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        featureStore.setTransaction(transaction);
        featureStore.addFeatures(collection);
        featureStore.setTransaction(transaction);
        transaction.commit();
        transaction.close();
    }

    /**
     * 读取tiff文件
     *
     * @param tiffPath
     * @return
     * @throws IOException
     */
    public static Coverage readTiff(String tiffPath) throws IOException {
        File f = new File(tiffPath);
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        ParameterValue<String> gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);
        GridCoverage2D coverage = new GeoTiffReader(f).read(new GeneralParameterValue[]{policy, gridSize, useJaiRead});
        return coverage;
    }

    /**
     * 读取tiff文件
     *
     * @param tiffPath
     * @return
     * @throws IOException
     */
    public static Map readTiffFile(String tiffPath) throws IOException {
        File file = new File(tiffPath);
        AbstractGridCoverage2DReader geoTiffReader = new GeoTiffReader(file);
        GridCoverage2D coverage = geoTiffReader.read(null);
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
        RenderedImage sourceImage = coverage.getRenderableImage(0, 1).createDefaultRendering();
        Raster sourceRaster = sourceImage.getData();
        double cellSize = 1;
        double[][] resolutionLevels = geoTiffReader.getResolutionLevels();
        if (resolutionLevels.length > 0) {
            if (resolutionLevels[0].length > 0) {
                cellSize = resolutionLevels[0][0];
            }
        }
        Map map = new HashMap();
        map.put("coverage", coverage);
        map.put("raster", sourceRaster);
        map.put("cellSize", cellSize);
        map.put("crs", crs);
        return map;
    }

    /**
     * 生成tiff文件
     *
     * @param outTiffPath
     * @param coverage
     * @throws IOException
     */
    public static void writeTiff(String outTiffPath, GridCoverage coverage) throws IOException {
        File file = new File(outTiffPath);
        GeoTiffWriter geoTiffWriter = new GeoTiffWriter(file);
        final GeoTiffFormat format = new GeoTiffFormat();
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        // 设置写出参数
        wp.setCompressionMode(GeoTiffWriteParams.MODE_DEFAULT);
        wp.setTilingMode(GeoTiffWriteParams.MODE_DEFAULT);
        ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        geoTiffWriter.write((GridCoverage) coverage, paramWrite.values().toArray(new GeneralParameterValue[4]));
        geoTiffWriter.dispose();
    }

    /**
     * 按GeoJSON掩膜提取tiff
     *
     * @param geoJsonStr
     * @param tiffPath
     * @return
     * @throws IOException
     */
    public static Coverage clipByGeoJson(String geoJsonStr, String tiffPath) throws IOException {
        GeoJSONReader geoJSONReader = new GeoJSONReader(geoJsonStr);
        SimpleFeatureCollection collection = geoJSONReader.getFeatures();
        FeatureIterator<SimpleFeature> iterator = collection.features();
        List<Geometry> all = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                all.add(geometry);
            }
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
        Coverage coverage = readTiff(tiffPath);
        Coverage clippedCoverage = null;
        if (all.size() > 0) {
            CoverageProcessor processor = new CoverageProcessor();
            ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
            params.parameter("Source").setValue(coverage);
            GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
            Geometry[] a = all.toArray(new Geometry[0]);
            GeometryCollection c = new GeometryCollection(a, factory);
            Envelope envelope = all.get(0).getEnvelopeInternal();
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(x1, x2, y1, y2, coverage.getCoordinateReferenceSystem());
            params.parameter("ENVELOPE").setValue(referencedEnvelope);
            params.parameter("ROI").setValue(c);
            params.parameter("ForceMosaic").setValue(true);
            clippedCoverage = processor.doOperation(params);
        }
        if (all.size() == 0) {
            System.out.println("Crop by shapeFile but no simple features matched extent!");
        }
        return clippedCoverage;
    }

    /**
     * 按GeoJSON掩膜提取tiff并生成裁剪结果tiff文件
     *
     * @param geoJsonStr
     * @param inputPath
     * @param outputPath
     * @throws IOException
     */
    public static void clipByGeoJson(String geoJsonStr, String inputPath, String outputPath) throws IOException {
        GeoJSONReader geoJSONReader = new GeoJSONReader(geoJsonStr);
        SimpleFeatureCollection collection = geoJSONReader.getFeatures();
        FeatureIterator<SimpleFeature> iterator = collection.features();
        List<Geometry> all = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                all.add(geometry);
            }
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
        Coverage coverage = readTiff(inputPath);
        Coverage clippedCoverage = null;
        if (all.size() > 0) {
            CoverageProcessor processor = new CoverageProcessor();
            ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
            params.parameter("Source").setValue(coverage);
            GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
            Geometry[] a = all.toArray(new Geometry[0]);
            GeometryCollection c = new GeometryCollection(a, factory);
            Envelope envelope = all.get(0).getEnvelopeInternal();
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(x1, x2, y1, y2, coverage.getCoordinateReferenceSystem());
            params.parameter("ENVELOPE").setValue(referencedEnvelope);
            params.parameter("ROI").setValue(c);
            params.parameter("ForceMosaic").setValue(true);
            clippedCoverage = processor.doOperation(params);
        }
        if (all.size() == 0) {
            System.out.println("Crop by shapeFile but no simple features matched extent!");
        }
        writeTiff(outputPath, (GridCoverage) clippedCoverage);
    }

    /**
     * 按shpFile掩膜提取tiff并生成裁剪结果tiff文件
     *
     * @param shpPath
     * @param tiffPath
     * @param outTiffPath
     * @return
     * @throws IOException
     */
    public static Coverage clipByShp(String shpPath, String tiffPath, String outTiffPath) throws IOException {
        SimpleFeatureCollection collection = readFeatureCollection(shpPath);
        FeatureIterator<SimpleFeature> iterator = collection.features();
        List<Geometry> all = new ArrayList<>();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                all.add(geometry);
            }
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
        Coverage coverage = readTiff(tiffPath);
        Coverage clippedCoverage = null;
        if (all.size() > 0) {
            CoverageProcessor processor = new CoverageProcessor();
            ParameterValueGroup params = processor.getOperation("CoverageCrop").getParameters();
            params.parameter("Source").setValue(coverage);
            GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
            Geometry[] a = all.toArray(new Geometry[0]);
            GeometryCollection c = new GeometryCollection(a, factory);
            Envelope envelope = all.get(0).getEnvelopeInternal();
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(x1, x2, y1, y2, coverage.getCoordinateReferenceSystem());
            params.parameter("ENVELOPE").setValue(referencedEnvelope);
            params.parameter("ROI").setValue(c);
            params.parameter("ForceMosaic").setValue(true);
            clippedCoverage = processor.doOperation(params);
        }
        if (all.size() == 0) {
            System.out.println("Crop by shapeFile but no simple features matched extent!");
        }
        writeTiff(outTiffPath, (GridCoverage) clippedCoverage);
        return clippedCoverage;
    }

    /**
     * 根据 geometry,property 生成 GeoJSON
     *
     * @param list
     * @param geometryType
     * @param propertyName
     * @return
     * @throws SchemaException
     */
    public static String createGeoJson(List<Map> list, String geometryType, String propertyName) throws SchemaException {
        final SimpleFeatureType TYPE = DataUtilities.createType("Grid", "geometry:" + geometryType + "," + propertyName + ":String");
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        List<SimpleFeature> features = new ArrayList<>();
        SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
        for (int i = 0; i < list.size(); i++) {
            Geometry geometry = (Geometry) list.get(i).get("geometry");
            featureBuilder.add(geometry);
            featureBuilder.add(list.get(i).get(propertyName));
            SimpleFeature feature = featureBuilder.buildFeature(null);
            features.add(feature);
        }
        String geoJSON = GeoJSONWriter.toGeoJSON(collection);
        return geoJSON;
    }

    /**
     * 点是否在多边形内部
     *
     * @param pointArr
     * @param polygon
     * @return
     */
    public static boolean isPointInPolygon(double[] pointArr, Geometry polygon) {
        Coordinate point = new Coordinate(pointArr[0], pointArr[1]);
        PointLocator pointLocator = new PointLocator();
        return pointLocator.intersects(point, polygon);
    }

    /**
     * 去除list中mapKey值相同的map
     *
     * @param list
     * @param mapKey
     * @return
     */
    public static List<Map> removeRepeatMapByKey(List<Map> list, String mapKey) {
        List<Map> resultList = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).get(mapKey).equals(list.get(i + 1).get(mapKey))) {
                continue;
            } else {
                resultList.add(list.get(i));
            }
        }
        return resultList;
    }

    /**
     * 找第一个峰值
     *
     * @param list
     * @param mapKey
     * @return
     */
    public static int findPeakIndex(List<Map> list, String mapKey) {
        if (list.size() == 0) return 0;
        int resultIndex = 0;
        for (int i = 1; i < list.size() - 1; i++) {
            float dem0 = Float.parseFloat(String.valueOf(list.get(i - 1).get(mapKey)));
            float dem1 = Float.parseFloat(String.valueOf(list.get(i).get(mapKey)));
            float dem2 = Float.parseFloat(String.valueOf(list.get(i + 1).get(mapKey)));
            if (dem1 > dem0 && dem1 > dem2) {
                resultIndex = i;
                break;
            }
        }
        return resultIndex;
    }

    /**
     * 两点生成线
     * @param points
     * @return
     */
    public static Geometry createLineString(double[][] points){
        Coordinate[] coordinateArray = new Coordinate[2];
        coordinateArray[0] =new Coordinate(points[0][0],points[0][1]);
        coordinateArray[1] =new Coordinate(points[1][0],points[1][1]);
        GeometryFactory geometryFactory =new GeometryFactory();
        Geometry lineString = geometryFactory.createLineString(coordinateArray);
        return lineString;
    }
    /**
     * 计算两点距离
     *
     * @param points
     * @return
     */
    public static double getDistance(double[][] points) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        geodeticCalculator.setStartingGeographicPoint(points[0][0], points[0][1]);
        geodeticCalculator.setDestinationGeographicPoint(points[1][0], points[1][1]);
        double distance = geodeticCalculator.getOrthodromicDistance();
        return distance;
    }

    /**
     * 线拆分成多段
     *
     * @param start
     * @param end
     * @param segments
     * @return
     */
    public static List<double[]> splitLine(double[] start, double[] end, int segments) {
        List<double[]> points = new ArrayList<>();
        double x_delta = (end[0] - start[0]) / segments;
        double y_delta = (end[1] - start[1]) / segments;
        points.add(start);
        for (int i = 1; i < segments; i++) {
            double[] p = new double[2];
            p[0] = start[0] + i * x_delta;
            p[1] = start[1] + i * y_delta;
            points.add(p);
        }
        points.add(end);
        return points;
    }

    /**
     * 拆线等分
     *
     * @param p0
     * @param p1
     * @param num
     * @return
     */
    public static List<Coordinate> polyLineDivide(Coordinate p0, Coordinate p1, int num) {
        double factor = 1.0D / num;
        int points = num - 1;
        List<Coordinate> coordinates = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            coordinates.add(pointAlong(p0, p1, factor * (i + 1)));
        }
        return coordinates;
    }

    /**
     * 线段比例点坐标计算
     * @param p0
     * @param p1
     * @param factor
     * @return
     */
    public static Coordinate pointAlong(Coordinate p0, Coordinate p1, double factor) {
        Coordinate coordinate = new Coordinate();
        coordinate.x = p0.x + factor * (p1.x - p0.x);
        coordinate.y = p0.y + factor * (p1.y - p0.y);
        coordinate.z = p0.y + factor * (p1.z - p0.z);
        return coordinate;
    }
}















