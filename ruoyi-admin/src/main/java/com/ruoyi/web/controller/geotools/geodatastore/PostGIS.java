package com.ruoyi.web.controller.geotools.geodatastore;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yyx
 * @date 2022/8/18 - 上午 11:22
 */
public class PostGIS {
    /**
     * @param dbType:    数据库类型，postgis or mysql
     * @param host:      ip地址
     * @param port:      端口号
     * @param database:  需要连接的数据库
     * @param userName:  用户名
     * @param password:  密码
     * @param tableName: 需要连接的表名
     * @return: 返回为FeatureCollection类型
     */
    public static SimpleFeatureCollection connAndGetCollection(String dbType, String host, String port, String database, String userName, String password, String tableName) {
        Map<String, Object> params = new HashMap<String, Object>();
        DataStore pgDatastore = null;
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, dbType); //需要连接何种数据库，postgis or mysql
        params.put(PostgisNGDataStoreFactory.HOST.key, host);//ip地址
        params.put(PostgisNGDataStoreFactory.PORT.key, new Integer(port));//端口号
        params.put(PostgisNGDataStoreFactory.DATABASE.key, database);//需要连接的数据库
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, "public");//架构
        params.put(PostgisNGDataStoreFactory.USER.key, userName);//需要连接数据库的名称
        params.put(PostgisNGDataStoreFactory.PASSWD.key, password);//数据库的密码
        SimpleFeatureCollection fcollection = null;
        try {
            //获取存储空间
            pgDatastore = DataStoreFinder.getDataStore(params);
            //根据表名获取source
            SimpleFeatureSource fSource = pgDatastore.getFeatureSource(tableName);
            if (pgDatastore != null) {
                System.out.println("系统连接到位于：" + host + "的空间数据库" + database
                        + "成功！");
                fcollection = fSource.getFeatures();
            } else {
                System.out.println("系统连接到位于：" + host + "的空间数据库" + database
                        + "失败！请检查相关参数");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("系统连接到位于：" + host + "的空间数据库" + database
                    + "失败！请检查相关参数");
        }
        return fcollection;
    }
}
