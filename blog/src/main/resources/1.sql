INSERT INTO site_config (config_key, config_value, description, create_time, update_time)
VALUES (
           'blog_info',
           '{"siteName":"我的个人博客","siteDesc":"技术学习与分享的小站","copyright":"©2026 个人博客 版权所有"}',
           '博客基础配置信息',
           NOW(),
           NOW()
       );