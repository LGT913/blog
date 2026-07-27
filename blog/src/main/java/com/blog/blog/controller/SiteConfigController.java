package com.blog.blog.controller;

import com.blog.blog.common.Result;
import com.blog.blog.entity.SiteConfig;
import com.blog.blog.service.SiteConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site")
public class SiteConfigController {

    @Autowired
    private SiteConfigService siteConfigService;

    @GetMapping("/config")
    public Result<SiteConfig> getConfig(@RequestParam String configKey) {
        SiteConfig siteConfig = siteConfigService.getByConfigKey(configKey);
        return Result.success(siteConfig);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/config")
    public Result<SiteConfig> saveConfig(@RequestBody SiteConfig siteConfig) {
        SiteConfig savedConfig = siteConfigService.saveConfig(
                siteConfig.getConfigKey(), siteConfig.getConfigValue(), siteConfig.getDescription());
        return Result.success(savedConfig);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/config/{id}")
    public Result<String> deleteConfig(@PathVariable Long id) {
        siteConfigService.deleteConfig(id);
        return Result.success("删除成功");
    }
}
