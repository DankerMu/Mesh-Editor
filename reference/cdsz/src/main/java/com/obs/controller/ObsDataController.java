package com.obs.controller;

import com.obs.pojo.ObsDataEntity;
import com.obs.pojo.ObsDataParams;
import com.obs.service.inf.ObsDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @category
 * @date 2025/3/18 10:11
 * @description TODO
 */
@RestController
@RequestMapping("/obs/")
public class ObsDataController {

    @Resource
    private ObsDataService obsDataService;
    @PostMapping("queryObsData")
    public List<ObsDataEntity> queryObsData(@RequestBody ObsDataParams params)
    {
        List<ObsDataEntity> result = obsDataService.queryObsData(params);

        return result;
    }
}
