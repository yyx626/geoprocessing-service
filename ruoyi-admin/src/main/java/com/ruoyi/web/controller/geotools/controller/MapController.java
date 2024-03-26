package com.ruoyi.web.controller.geotools.controller;

import com.ruoyi.common.core.controller.BaseController;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * map 地图
 *
 * @author yyx
 */
@Controller
@RequestMapping("/gis/map")
public class MapController extends BaseController
{
    private String prefix = "/gis";

    @RequiresPermissions("gis:map:view")
    @GetMapping()
    public String build()
    {
        return prefix + "/map";
    }
}