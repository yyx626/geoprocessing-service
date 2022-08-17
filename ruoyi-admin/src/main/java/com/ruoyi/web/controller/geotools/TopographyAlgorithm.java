package com.ruoyi.web.controller.geotools;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.*;

/**
 * @author yyx
 * @date 2022/8/16 - 上午 9:43
 */
public class TopographyAlgorithm {
    private float cellSize = 1;
    private Raster rasterData;
    private GridCoverage2D coverage;
    private double noDataValue;
    private int Cols;
    private int Rows;

    private float[][] pJuanJiFx = new float[3][3];
    private float[][] pJuanJiFy = new float[3][3];

    private float diffence = 0;

    private GeometryFactory geometryFactory = new GeometryFactory();

    public TopographyAlgorithm(GridCoverage2D coverage, Raster dem, float cellSize) {
        this.coverage = coverage;
        this.rasterData = dem;
        this.Cols = dem.getWidth();
        this.Rows = dem.getHeight();
        this.cellSize = cellSize;
        this.noDataValue = this.coverage.getSampleDimension(0).getNoDataValues()[0];
        this.initJuanJi();
        this.calculateDiffence(this.pJuanJiFy);
    }

    private void initJuanJi() {
        this.pJuanJiFy[0][0] = -1;
        this.pJuanJiFy[0][1] = 0;
        this.pJuanJiFy[0][2] = 1;
        this.pJuanJiFy[1][0] = -2;
        this.pJuanJiFy[1][1] = 0;
        this.pJuanJiFy[1][2] = 2;
        this.pJuanJiFy[2][0] = -1;
        this.pJuanJiFy[2][1] = 0;
        this.pJuanJiFy[2][2] = 1;

        this.pJuanJiFx[0][0] = -1;
        this.pJuanJiFx[0][1] = -2;
        this.pJuanJiFx[0][2] = -1;
        this.pJuanJiFx[1][0] = 0;
        this.pJuanJiFx[1][1] = 0;
        this.pJuanJiFx[1][2] = 0;
        this.pJuanJiFx[2][0] = 1;
        this.pJuanJiFx[2][1] = 2;
        this.pJuanJiFx[2][2] = 1;
    }

    private void calculateDiffence(float[][] arr) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.diffence = this.diffence + Math.abs(arr[i][j]);
            }
        }
    }

    private float diffenceFx(int colNo, int rowNo) {
        float Fx = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Fx = Fx + (float) rasterData.getSampleFloat(rowNo + i, colNo + j, 0) * pJuanJiFx[i + 1][j + 1];
            }
        }
        return Fx / (this.diffence * this.cellSize);
    }

    private float diffenceFy(int colNo, int rowNo) {
        float Fy = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Fy = Fy + (float) rasterData.getSampleFloat(rowNo + i, colNo + j, 0) * pJuanJiFy[i + 1][j + 1];
            }
        }
        return Fy / (this.diffence * this.cellSize);
    }

    public float cellSlope(int colNo, int rowNo) {
        float fx = (float) (this.diffenceFx(colNo, rowNo) * 0.00001036);
        float fy = (float) (this.diffenceFy(colNo, rowNo) * 0.00001036);
        return (float) (Math.atan(Math.sqrt(fx * fx + fy * fy)) * 180 / Math.PI);
    }

    public float cellAspect(int colNo, int rowNo) {
        float aspect;
        float cell;
        float fx = this.diffenceFx(colNo, rowNo);
        float fy = this.diffenceFy(colNo, rowNo);

        cell = (float) (57.29578 * Math.atan2(fy, -fx));
        if (cell < 0) {
            aspect = (float) (90.0 - cell);
        } else if (cell > 90.0) {
            aspect = (float) (450.0 - cell);
        } else {
            aspect = (float) (90.0 - cell);
        }
        return aspect;
    }

    /**
     * 根据坐标反查行列号
     *
     * @param point
     * @return
     * @throws TransformException
     */
    public Point2D getIndex(double[] point) throws TransformException {
        CoordinateReferenceSystem crs = this.coverage.getCoordinateReferenceSystem2D();
        DirectPosition2D position2D = new DirectPosition2D(crs, point[0], point[1]);
        Point2D point2D = this.coverage.getGridGeometry().worldToGrid(position2D);
        return point2D;
    }

    /**
     * 根据行列号查栅格中心经纬度坐标
     *
     * @param colNo
     * @param rowNo
     * @return
     * @throws TransformException
     */
    public double[] getCellCoordinate(int colNo, int rowNo) throws TransformException {
        GridCoordinates2D coordinates2D = new GridCoordinates2D(colNo, rowNo);
        DirectPosition pos = this.coverage.getGridGeometry().gridToWorld(coordinates2D);
        double[] result = pos.getCoordinate();
        return result;
    }

    /**
     * 点连线经过的栅格
     *
     * @param pointList
     * @return
     * @throws TransformException
     */
    public List<String> findBlock(List<double[]> pointList) throws TransformException {
        List<String> gridList = new ArrayList<>();
        for (int i = 0; i < pointList.size(); i++) {
            Point2D index = getIndex(pointList.get(i));
            String indexStr = (int) index.getX() + "," + (int) index.getY();
            gridList.add(indexStr);
        }
        HashSet set = new HashSet(gridList);
        gridList.clear();
        gridList.addAll(set);
        return gridList;
    }

    /**
     * 获取拆分段数
     *
     * @param start
     * @param end
     * @return
     */
    public int getSplitSegments(double[] start, double[] end) {
        double[][] points = new double[2][2];
        points[0] = start;
        points[1] = end;
        // 视线长度
        double distance = CommonMethod.getDistance(points);
        // 拆分长度
        double dis_x = Math.abs(start[0] - end[0]);
        double dis_y = Math.abs(start[1] - end[1]);
        double max = Math.max(dis_x, dis_y);
        int m = (int) (max / this.cellSize);
        // 拆分段数
        int segments = m == 0 ? (int) distance : (int) (distance / m);
        return segments;
    }

    /**
     * 判断指定栅格是否有值
     *
     * @param colNo
     * @param rowNo
     * @return
     */
    private boolean hasData(int colNo, int rowNo) {
        return this.rasterData.getSampleFloat(colNo, rowNo, 0) != this.noDataValue;
    }

    /**
     * 获取栅格像元值
     *
     * @param colNo
     * @param rowNo
     * @return
     */
    private float getValue(int colNo, int rowNo) {
        return this.rasterData.getSampleFloat(colNo, rowNo, 0);
    }

    /**
     * 获取每个栅格的geometry
     *
     * @param colNo
     * @param rowNo
     * @return
     */
    public Geometry getGeo(int colNo, int rowNo) {
        Envelope2D coverageEnvelope2D = this.coverage.getEnvelope2D();
        double minX = coverageEnvelope2D.getMinX();
        double maxY = coverageEnvelope2D.getMaxY();
        Coordinate[] coordinateArray = new Coordinate[5];
        coordinateArray[0] = new Coordinate(minX + colNo * this.cellSize, maxY - rowNo * this.cellSize);
        coordinateArray[1] = new Coordinate(minX + (colNo + 1) * this.cellSize, maxY - rowNo * this.cellSize);
        coordinateArray[2] = new Coordinate(minX + (colNo + 1) * this.cellSize, maxY - (rowNo + 1) * this.cellSize);
        coordinateArray[3] = new Coordinate(minX + colNo * this.cellSize, maxY - (rowNo + 1) * this.cellSize);
        coordinateArray[4] = new Coordinate(minX + colNo * this.cellSize, maxY - rowNo * this.cellSize);
        Geometry geometry = geometryFactory.createPolygon(coordinateArray);
        return geometry;
    }

    /**
     * 计算坡度
     * @return
     */
    public String calculateSlope() throws SchemaException {
        List<Map> list = new ArrayList<>();
        float[][] result = new float[this.Rows][this.Cols];
        for (int i = 1; i < this.Rows - 1; i++) {
            for (int j = 1; j < this.Cols - 1; j++) {
                if (this.hasData(j, i)) {
                    result[i][j] = this.cellSlope(i, j);
                    Map map = new HashMap();
                    map.put("geometry", this.getGeo(j, i));
                    map.put("gridCode", String.valueOf(result[i][j]));
                    list.add(map);
                }
            }
        }
        return CommonMethod.createGeoJson(list, "Polygon", "gridCode");
    }

    /**
     * 计算坡向
     * @return
     */
    public String calculateAspect() throws SchemaException {
        List<Map> list = new ArrayList<>();
        float[][] result = new float[this.Rows][this.Cols];
        for (int i = 1; i < this.Rows - 1; i++) {
            for (int j = 1; j < this.Cols - 1; j++) {
                if (this.hasData(j, i)) {
                    result[i][j] = this.cellAspect(i, j);
                    Map map = new HashMap();
                    map.put("geometry", this.getGeo(j, i));
                    map.put("gridCode", String.valueOf(result[i][j]));
                    list.add(map);
                }
            }
        }
        return CommonMethod.createGeoJson(list, "Polygon", "gridCode");
    }

    /**
     * 计算可视域
     * @param point
     * @return
     * @throws TransformException
     */
    public String calculateViewShed(double[] point) throws TransformException, SchemaException {
        Point2D point2D = getIndex(point);
        int col = (int) point2D.getX();
        int row = (int) point2D.getY();
        List<Map> resultList = new ArrayList<>();
        for (int i = 0; i < this.Rows; i++) {
            for (int j = 0; j < this.Cols; j++) {
                if (this.hasData(j, i)) {
                    // 遍历到的栅格中心坐标(目标点)
                    double[] cellCoordinate = getCellCoordinate(j, i);
                    // 获取观察点到目标点连线拆分段数
                    int splitSegments = getSplitSegments(point, cellCoordinate);
                    // 拆分观察点到目标点连线为点集合
                    List<double[]> pointList = CommonMethod.splitLine(point, cellCoordinate, splitSegments);
                    // 观察点高程
                    float startDem = this.getValue(col, row);
                    // 目标点高程
                    float endDem = this.getValue(j, i);
                    // 观察点到目标点之间经过的栅格
                    List<String> gridList = findBlock(pointList);
                    boolean isVisible = true;
                    // 遍历经过的栅格 若存在一个栅格的高程值大于观察点和目标点的高程 则两点不可视
                    for (int k = 0; k < gridList.size(); k++) {
                        isVisible = true;
                        String[] str = gridList.get(k).split(",");
                        int tmpCol = Integer.parseInt(str[0]);
                        int tmpRow = Integer.parseInt(str[1]);
                        float tmpDem = this.getValue(tmpCol, tmpRow);
                        if (tmpDem > startDem && tmpDem > endDem) {
                            isVisible = false;
                            break;
                        }
                    }
                    Map map =new HashMap();
                    map.put("geometry",this.getGeo(j,i));
                    map.put("gridCode",isVisible?1:0);
                    resultList.add(map);
                }
            }
        }
        return CommonMethod.createGeoJson(resultList,"Polygon","gridCode");
    }
}



















