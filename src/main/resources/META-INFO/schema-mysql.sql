-- MySQL dump 10.13  Distrib 5.7.17, for macos10.12 (x86_64)
--
-- Host: localhost    Database: home_dash
-- ------------------------------------------------------
-- Server version	5.7.25

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `file`
--

CREATE TABLE `file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `file_name` varchar(100) NOT NULL,
  `parent_id` int(11) NOT NULL DEFAULT '0',
  `type` varchar(45) NOT NULL,
  `size` bigint DEFAULT NULL,
  `resource_id` int(11) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=127 DEFAULT CHARSET=utf8;

--
-- Table structure for table `resource`
--

CREATE TABLE `resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `size` int(11) NOT NULL DEFAULT '0',
  `md5` varchar(32) DEFAULT '',
  `link` int(11) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `path` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resource_md5` (`md5`)
) ENGINE=InnoDB AUTO_INCREMENT=58 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `resource_chunk`
--

CREATE TABLE `resource_chunk` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `chunk_number` int(11) NOT NULL,
  `chunk_size` int(11) NOT NULL,
  `current_chunk_size` int(11) NOT NULL,
  `total_size` int(11) NOT NULL,
  `identifier` varchar(100) NOT NULL,
  `filename` varchar(100) NOT NULL,
  `relative_path` varchar(100) NOT NULL,
  `total_chunks` int(11) NOT NULL,
  `md5` varchar(32) DEFAULT '',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_flag` (`identifier`,`chunk_number`),
  KEY `idx_identifier` (`identifier`)
) ENGINE=InnoDB AUTO_INCREMENT=3810 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
-- ============================================
-- 二阶段：预览与检索整理 - 表结构
-- ============================================

--
-- Table structure for table `file_tag`
--

CREATE TABLE `file_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tag_name` varchar(50) NOT NULL,
  `tag_color` varchar(20) DEFAULT '#409EFF',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `search_history`
--

CREATE TABLE `search_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `keyword` varchar(200) NOT NULL,
  `search_type` varchar(20) DEFAULT 'FILE',
  `search_params` varchar(1000) DEFAULT NULL,
  `searched_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_keyword` (`keyword`),
  KEY `idx_searched_at` (`searched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `favorite`
--

CREATE TABLE `favorite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_id` (`resource_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `recent_use`
--

CREATE TABLE `recent_use` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `use_type` varchar(20) NOT NULL,
  `used_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resource_type` (`resource_id`,`use_type`),
  KEY `idx_used_at` (`used_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `file_remark`
--

CREATE TABLE `file_remark` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `remark_content` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `file_tag_relation`
--

CREATE TABLE `file_tag_relation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `file_id` bigint(20) NOT NULL,
  `tag_id` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_tag` (`file_id`,`tag_id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Table structure for table `filter_view`
--

CREATE TABLE `filter_view` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `view_name` varchar(50) NOT NULL,
  `view_params` varchar(1000) NOT NULL,
  `is_default` tinyint(1) DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_view_name` (`view_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Dump completed on 2020-01-31 14:52:17
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-01-31 14:52:17

-- ============================================
-- 阶段三：媒体中心与 NAS 体验 - 表结构 (MySQL)
-- ============================================

-- 媒体扫描任务表
CREATE TABLE `media_scan_task` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `media_type` varchar(20) NOT NULL,
    `task_type` varchar(30) NOT NULL,
    `status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `retry_count` int(11) DEFAULT '0',
    `max_retries` int(11) DEFAULT '3',
    `error_message` varchar(500) DEFAULT NULL,
    `priority` int(11) DEFAULT '0',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `start_time` datetime DEFAULT NULL,
    `finish_time` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_media_scan_task_status` (`status`),
    KEY `idx_media_scan_task_file_type` (`file_id`, `media_type`),
    KEY `idx_media_scan_task_priority` (`priority`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 缩略图记录表
CREATE TABLE `media_thumbnail` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `thumbnail_type` varchar(20) NOT NULL,
    `width` int(11) DEFAULT NULL,
    `height` int(11) DEFAULT NULL,
    `thumbnail_path` varchar(200) NOT NULL,
    `file_size` bigint(20) DEFAULT NULL,
    `generate_status` varchar(20) NOT NULL DEFAULT 'SUCCESS',
    `generate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_thumbnail_file_type_size` (`file_id`, `thumbnail_type`, `width`, `height`),
    KEY `idx_media_thumbnail_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 图片元数据表
CREATE TABLE `media_picture_metadata` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `width` int(11) DEFAULT NULL,
    `height` int(11) DEFAULT NULL,
    `taken_at` datetime DEFAULT NULL,
    `camera_make` varchar(100) DEFAULT NULL,
    `camera_model` varchar(100) DEFAULT NULL,
    `lens_model` varchar(100) DEFAULT NULL,
    `focal_length` varchar(30) DEFAULT NULL,
    `aperture` varchar(30) DEFAULT NULL,
    `exposure_time` varchar(30) DEFAULT NULL,
    `iso` int(11) DEFAULT NULL,
    `gps_latitude` decimal(10, 6) DEFAULT NULL,
    `gps_longitude` decimal(10, 6) DEFAULT NULL,
    `location_name` varchar(200) DEFAULT NULL,
    `orientation` int(11) DEFAULT '0',
    `color_mode` varchar(20) DEFAULT 'RGB',
    `scan_status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_picture_file_id` (`file_id`),
    KEY `idx_media_picture_taken_at` (`taken_at`),
    KEY `idx_media_picture_scan_status` (`scan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 视频元数据表
CREATE TABLE `media_video_metadata` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `duration` bigint(20) DEFAULT NULL,
    `width` int(11) DEFAULT NULL,
    `height` int(11) DEFAULT NULL,
    `resolution` varchar(20) DEFAULT NULL,
    `bitrate` bigint(20) DEFAULT NULL,
    `frame_rate` decimal(5, 2) DEFAULT NULL,
    `video_codec` varchar(50) DEFAULT NULL,
    `audio_codec` varchar(50) DEFAULT NULL,
    `audio_channels` int(11) DEFAULT NULL,
    `audio_bitrate` bigint(20) DEFAULT NULL,
    `has_audio` tinyint(1) DEFAULT '1',
    `has_subtitle` tinyint(1) DEFAULT '0',
    `cover_generated` tinyint(1) DEFAULT '0',
    `cover_path` varchar(200) DEFAULT NULL,
    `scan_status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_video_file_id` (`file_id`),
    KEY `idx_media_video_scan_status` (`scan_status`),
    KEY `idx_media_video_duration` (`duration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 视频系列表
CREATE TABLE `media_video_series` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `series_name` varchar(100) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `poster_path` varchar(200) DEFAULT NULL,
    `total_episodes` int(11) DEFAULT '0',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_media_video_series_name` (`series_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 视频集数关联表
CREATE TABLE `media_video_episode` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `series_id` bigint(20) NOT NULL,
    `file_id` bigint(20) NOT NULL,
    `episode_number` int(11) NOT NULL,
    `season_number` int(11) DEFAULT '1',
    `episode_title` varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_video_episode_series_ep` (`series_id`, `episode_number`, `season_number`),
    KEY `idx_media_video_episode_series` (`series_id`),
    KEY `idx_media_video_episode_file` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 音频元数据表
CREATE TABLE `media_audio_metadata` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `title` varchar(200) DEFAULT NULL,
    `album` varchar(200) DEFAULT NULL,
    `artist` varchar(200) DEFAULT NULL,
    `album_artist` varchar(200) DEFAULT NULL,
    `genre` varchar(100) DEFAULT NULL,
    `track_number` int(11) DEFAULT NULL,
    `disc_number` int(11) DEFAULT '1',
    `year` int(11) DEFAULT NULL,
    `duration` bigint(20) DEFAULT NULL,
    `bitrate` bigint(20) DEFAULT NULL,
    `sample_rate` int(11) DEFAULT NULL,
    `cover_path` varchar(200) DEFAULT NULL,
    `lyrics` text,
    `scan_status` varchar(20) NOT NULL DEFAULT 'PENDING',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_audio_file_id` (`file_id`),
    KEY `idx_media_audio_title` (`title`),
    KEY `idx_media_audio_album` (`album`),
    KEY `idx_media_audio_artist` (`artist`),
    KEY `idx_media_audio_scan_status` (`scan_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 播放列表表
CREATE TABLE `media_audio_playlist` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `playlist_name` varchar(100) NOT NULL,
    `description` varchar(500) DEFAULT NULL,
    `cover_path` varchar(200) DEFAULT NULL,
    `play_mode` varchar(20) DEFAULT 'ORDER',
    `total_tracks` int(11) DEFAULT '0',
    `total_duration` bigint(20) DEFAULT '0',
    `is_default` tinyint(1) DEFAULT '0',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_media_audio_playlist_name` (`playlist_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 播放列表曲目关联表
CREATE TABLE `media_audio_playlist_item` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `playlist_id` bigint(20) NOT NULL,
    `file_id` bigint(20) NOT NULL,
    `position` int(11) NOT NULL,
    `added_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_audio_playlist_item` (`playlist_id`, `file_id`),
    KEY `idx_media_audio_playlist_item_playlist` (`playlist_id`),
    KEY `idx_media_audio_playlist_item_position` (`playlist_id`, `position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 相册表
CREATE TABLE `media_album` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `album_name` varchar(100) NOT NULL,
    `album_type` varchar(20) NOT NULL DEFAULT 'AUTO',
    `cover_file_id` bigint(20) DEFAULT NULL,
    `description` varchar(500) DEFAULT NULL,
    `rule_expression` varchar(200) DEFAULT NULL,
    `photo_count` int(11) DEFAULT '0',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_media_album_type` (`album_type`),
    KEY `idx_media_album_name` (`album_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 相册与图片关联表
CREATE TABLE `media_album_item` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `album_id` bigint(20) NOT NULL,
    `file_id` bigint(20) NOT NULL,
    `taken_at` datetime DEFAULT NULL,
    `sort_order` int(11) DEFAULT NULL,
    `added_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_album_item` (`album_id`, `file_id`),
    KEY `idx_media_album_item_album` (`album_id`),
    KEY `idx_media_album_item_file` (`file_id`),
    KEY `idx_media_album_item_taken` (`album_id`, `taken_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 播放历史表
CREATE TABLE `play_history` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `media_type` varchar(20) NOT NULL,
    `play_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `play_duration` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_play_history_file` (`file_id`),
    KEY `idx_play_history_time` (`play_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 视频观看进度表
CREATE TABLE `watch_progress` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `current_position` bigint(20) NOT NULL DEFAULT '0',
    `duration` bigint(20) NOT NULL,
    `progress_percent` decimal(5, 2) GENERATED ALWAYS AS (CASE WHEN `duration` > 0 THEN (`current_position` * 100.0 / `duration`) ELSE 0 END) STORED,
    `finished` tinyint(1) DEFAULT '0',
    `last_watched` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_watch_progress_file_id` (`file_id`),
    KEY `idx_watch_progress_percent` (`progress_percent`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 字幕关联表
CREATE TABLE `subtitle` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `file_id` bigint(20) NOT NULL,
    `subtitle_path` varchar(200) NOT NULL,
    `language` varchar(10) NOT NULL,
    `subtitle_type` varchar(20) NOT NULL DEFAULT 'EXTERNAL',
    `format` varchar(10) NOT NULL DEFAULT 'SRT',
    `is_default` tinyint(1) DEFAULT '0',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_subtitle_file` (`file_id`),
    KEY `idx_subtitle_language` (`file_id`, `language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 首页聚合缓存表
CREATE TABLE `media_summary_cache` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `cache_key` varchar(50) NOT NULL,
    `cache_value` text NOT NULL,
    `expire_time` datetime NOT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_media_summary_cache_key` (`cache_key`),
    KEY `idx_media_summary_cache_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
