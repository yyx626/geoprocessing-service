package com.ruoyi.web.controller.geotools.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间处理工具
 * @author yyx
 * @date 2022/8/16 - 上午 9:16
 */
public class GeoUtils {
    private static final WKTReader READER = new WKTReader();

    /**
     * 要素集合根节点
     */
    private static final String[] COLLECTION_TYPE = new String[]{"FeatureCollection"};

    /**
     * 地理要素类型
     */
    private static final String[] FEATURES_TYPE = new String[]{"Feature"};

    /**
     * 地理数据类型
     * 点、线、面、几何集合
     */
    private static final String[] GEO_TYPE = new String[]{"Geometry", "Point", "LineString", "Polygon", "MultiPoint", "MultiLineString", "MultiPolygon", "GeometryCollection"};

    /**
     * 获取 Geo 几何类型
     * @param wktStr WKT 字符串
     * @return Geo 几何类型
     */
    public static String getGeometryType(String wktStr) {
        String type = null;
        if (StringUtils.isNotEmpty(wktStr)) {
            try {
                Geometry read = READER.read(wktStr);
                type = read.getGeometryType();
            }catch (Exception e) {
                System.out.println("非规范 WKT 字符串："+ e);
                e.printStackTrace();
            }
        }
        return type;
    }

    /**
     * 是规范的 WKT
     * @param wktStr WKT 字符串
     * @return 是、否
     */
    public static boolean isWkt(String wktStr) {
        for (String s : GEO_TYPE) {
            if (wktStr.toLowerCase().startsWith(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 不是规范的 WKT
     * @param wktStr WKT 字符串
     * @return 是、否
     */
    public static boolean isNotWkt(String wktStr) {
        return !isWkt(wktStr);
    }

    /**
     * 是规范的 GeoJson
     * @param geoJsonStr GeoJson 字符串
     * @return 是、否
     */
    public static boolean isGeoJson(String geoJsonStr) {
        try {
            JSONObject jsonObject = JSON.parseObject(geoJsonStr);
            return isGeoJson(jsonObject);
        }catch (Exception e) {
            return false;
        }
    }

    /**
     * 不是规范的 GeoJson
     * @param geoJsonStr GeoJson 字符串
     * @return 是、否
     */
    public static boolean isNotGeoJson(String geoJsonStr) {
        return !isGeoJson(geoJsonStr);
    }

    /**
     * 是规范的 GeoJson
     * @param geoJson GeoJson 对象
     * @return 是、否
     */
    public static boolean isGeoJson(JSONObject geoJson) {
        String type = geoJson.getString("type");
        boolean mark = false;
        // 判断根节点
        if (ArrayUtils.contains(COLLECTION_TYPE, type)) {
            JSONArray jsonArray = geoJson.getObject("features", JSONArray.class);
            for (Object jsonStr : jsonArray) {
                JSONObject jsonObject = JSON.parseObject(String.valueOf(jsonStr));
                type = jsonObject.getString("type");
                // 判断地理要素
                if (ArrayUtils.contains(FEATURES_TYPE, type)) {
                    type = jsonObject.getObject("geometry", JSONObject.class).getString("type");
                    // 判断几何要素
                    mark = ArrayUtils.contains(GEO_TYPE, type);
                }
                if (!mark) {
                    return false;
                }
            }
        }else {
            // 判断地理要素
            if (ArrayUtils.contains(FEATURES_TYPE, type)) {
                type = geoJson.getObject("geometry", JSONObject.class).getString("type");
            }
            // 数据是几何数据
            mark = ArrayUtils.contains(GEO_TYPE, type);
        }
        return mark;
    }

    /**
     * 不是规范的 GeoJson
     * @param geoJson GeoJson 对象
     * @return 是、否
     */
    public static boolean isNotGeoJson(JSONObject geoJson) {
        return !isGeoJson(geoJson);
    }

    /**
     * GeoJson 转 WKT
     * @param geoJson GeoJson 对象
     * @return WKT 字符串
     */
    public static String geoJsonToWkt(JSONObject geoJson) {
        String wkt = null;
        try {
            if(isGeoJson(geoJson)){
                String type = geoJson.getString("type");
                // 判断是否根节点
                if (ArrayUtils.contains(COLLECTION_TYPE, type)) {
                    JSONArray geometriesArray = geoJson.getObject("features", JSONArray.class);
                    // 定义一个数组装图形对象
                    int size = geometriesArray.size();
                    Geometry[] geometries = new Geometry[size];
                    for (int i = 0; i < size; i++){
                        String str = JSON.parseObject(String.valueOf(geometriesArray.get(i))).getString("geometry");
                        Geometry geometry = GeoJSONReader.parseGeometry(str);
                        geometries[i] = geometry;
                    }
                    GeometryCollection geometryCollection = new GeometryCollection(geometries, new GeometryFactory());
                    wkt = geometryCollection.toText();
                }else {
                    String geoStr = geoJson.toString();
                    // 判断是否地理要素节点
                    if (ArrayUtils.contains(FEATURES_TYPE, type)) {
                        geoStr = geoJson.getString("geometry");
                    }
                    Geometry read = GeoJSONReader.parseGeometry(geoStr);
                    wkt = read.toText();
                }
            }
        } catch (Exception e){
            System.out.println("GeoJson 转 WKT 出现异常："+ e);
            e.printStackTrace();
        }
        return wkt;
    }

    /**
     * WKT 转 GeoJson
     * @param wktStr WKT 字符串
     * @return GeoJson 对象
     */
    public static JSONObject wktToGeoJson(String wktStr) {
        JSONObject jsonObject = new JSONObject();
        try {
            Geometry geometry = READER.read(wktStr);
            String geoJSON = GeoJSONWriter.toGeoJSON(geometry);
            jsonObject = JSON.parseObject(geoJSON);
        } catch (Exception e) {
            System.out.println("WKT 转 GeoJson 出现异常："+ e);
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * WKT 转 Feature
     * @param wktStr WKT 字符串
     * @return Feature JSON 对象
     */
    public static JSONObject wktToFeature(String wktStr) {
        JSONObject jsonObject = new JSONObject();
        try {
            SimpleFeatureType type = DataUtilities.createType("Link", "geometry:"+getGeometryType(wktStr));
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
            // 按照TYPE中声明的顺序为属性赋值就可以，其他方法我暂未尝试
            featureBuilder.add(READER.read(wktStr));
            SimpleFeature feature = featureBuilder.buildFeature(null);
            String fJson = GeoJSONWriter.toGeoJSON(feature);
            jsonObject = JSON.parseObject(fJson);
        }catch (Exception e) {
            System.out.println("WKT 转 Feature 出现异常："+ e);
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * WKT 转 FeatureCollection
     * @param wktStr  WKT 字符串
     * @return FeatureCollection JSON 对象
     */
    public static JSONObject wktToFeatureCollection(String wktStr) {
        JSONObject jsonObject = new JSONObject();
        try {
            String geometryType = getGeometryType(wktStr);
            if (StringUtils.isNotEmpty(geometryType)) {
                SimpleFeatureType type = DataUtilities.createType("Link", "geometry:" + geometryType);
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
                List<SimpleFeature> features = new ArrayList<>();
                SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
                featureBuilder.add(READER.read(wktStr));
                SimpleFeature feature = featureBuilder.buildFeature(null);
                features.add(feature);
                String fCJson = GeoJSONWriter.toGeoJSON(collection);
                jsonObject = JSON.parseObject(fCJson);
            }
        }catch (Exception e) {
            System.out.println("WKT 转 FeatureCollection 出现异常："+ e);
            e.printStackTrace();
        }
        return jsonObject;
    }
}
