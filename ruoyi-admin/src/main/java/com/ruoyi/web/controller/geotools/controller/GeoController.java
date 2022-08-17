package com.ruoyi.web.controller.geotools.controller;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.web.controller.geotools.CommonMethod;
import com.ruoyi.web.controller.geotools.service.TerrainAnalysis;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.operation.TransformException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author yyx
 * @date 2022/8/16 - 上午 9:15
 */
@Api("GP Service Test")
@RestController
@RequestMapping("/geoTools")
public class GeoController extends BaseController {

    @ApiOperation("提取等高线")
    @PostMapping("/contour")
    @ResponseBody
    public R<String> getContour(String geoJson, Double interval) throws IOException {
        String result = TerrainAnalysis.calContour(JSON.parse(geoJson).toString(), interval);
        return R.ok(result, "success");
    }

    @ApiOperation("坡度分析")
    @PostMapping("/slope")
    @ResponseBody
    public R<String> getSlope(String geoJson) throws IOException, SchemaException {
        String result = TerrainAnalysis.calSlope(JSON.parse(geoJson).toString());
        return R.ok(result, "success");
    }

    @ApiOperation("坡向分析")
    @PostMapping("/aspect")
    @ResponseBody
    public R<String> getAspect(String geoJson) throws IOException, SchemaException {
        String result = TerrainAnalysis.calAspect(JSON.parse(geoJson).toString());
        return R.ok(result, "success");
    }

    @ApiOperation("提取地形因子")
    @PostMapping("/factors")
    @ResponseBody
    public R<String> getFactors(String point) throws IOException, TransformException {
        String[] arr = point.split(",");
        String result = TerrainAnalysis.getTerrainFactors(Double.parseDouble(arr[0]), Double.parseDouble(arr[1]));
        return R.ok(result, "success");
    }

    @ApiOperation("视线-通视分析")
    @PostMapping("/lineView")
    @ResponseBody
    public R<String> getViewLine(String lineCoordinates) throws SchemaException, IOException {
        String result = TerrainAnalysis.lineOfSign(lineCoordinates);
        return R.ok(result, "success");
    }

    @ApiOperation("视域-通视分析")
    @PostMapping("/viewShed")
    @ResponseBody
    public R<String> getViewShed(String viewPoint, String geoJson) throws SchemaException, IOException, TransformException {
        // 观察点坐标
        String[] arr = viewPoint.split(",");
        double[] point = new double[2];
        point[0] = Double.parseDouble(arr[0]);
        point[1] = Double.parseDouble(arr[1]);
        // 判断观察点是否在视域范围内
        List<Geometry> geometries = CommonMethod.readGeoJson(JSON.parse(geoJson).toString());
        if (!CommonMethod.isPointInPolygon(point, geometries.get(0))) return R.fail("观察点不在视域范围内！");
        String result = TerrainAnalysis.viewShed(point, JSON.parse(geoJson).toString());
        return R.ok(result, "success");
    }
}
