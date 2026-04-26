package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.PreviewBiz;
import com.hd.common.exception.DataNotFoundException;
import com.hd.dao.entity.RecentUse;
import com.hd.dao.entity.Resource;
import com.hd.dao.service.RecentUseDataService;
import com.hd.dao.service.ResourceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预览业务实现类。
 * 实现图片、文本、音频等文件的预览能力。
 * Biz层通过Service访问数据，不直接调用Mapper。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewBizImpl implements PreviewBiz {

    private final ResourceDataService resourceDataService;
    private final RecentUseDataService recentUseDataService;

    @Value("${HomeDash.homeDir:./data}")
    private String homeDir;

    private static final long MAX_TEXT_PREVIEW_SIZE = 1024 * 1024; // 1MB
    private static final long TEXT_SUMMARY_MAX_SIZE = 50 * 1024; // 50KB for summary
    private static final int THUMBNAIL_MAX_WIDTH = 300;
    private static final int THUMBNAIL_MAX_HEIGHT = 300;
    private static final String THUMBNAIL_FORMAT = "png";

    @Override
    public byte[] getImageThumbnail(Long resourceId) {
        log.info("获取图片缩略图 [resourceId={}]", resourceId);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null || resource.getPath() == null) {
            log.warn("资源不存在 [resourceId={}]", resourceId);
            return null;
        }

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                log.warn("文件不存在 [path={}]", filePath);
                return null;
            }

            // 检查缓存的缩略图
            Path thumbnailCachePath = getThumbnailCachePath(resource);
            if (Files.exists(thumbnailCachePath)) {
                log.debug("使用缓存缩略图 [resourceId={}]", resourceId);
                return Files.readAllBytes(thumbnailCachePath);
            }

            // 生成缩略图
            byte[] thumbnailData = generateThumbnail(filePath);
            if (thumbnailData != null) {
                // 保存缓存
                saveThumbnailCache(thumbnailCachePath, thumbnailData);
            }
            return thumbnailData;
        } catch (IOException e) {
            log.error("读取图片文件失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            return null;
        }
    }

    private Path getThumbnailCachePath(Resource resource) {
        String cacheDir = homeDir + "/.cache/thumbnails";
        return Paths.get(cacheDir, resource.getId() + "." + THUMBNAIL_FORMAT);
    }

    private void saveThumbnailCache(Path cachePath, byte[] data) {
        try {
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, data);
            log.debug("缩略图缓存已保存 [path={}]", cachePath);
        } catch (IOException e) {
            log.warn("缩略图缓存保存失败 [path={}, error={}]", cachePath, e.getMessage());
        }
    }

    private byte[] generateThumbnail(Path filePath) {
        try {
            BufferedImage originalImage = ImageIO.read(filePath.toFile());
            if (originalImage == null) {
                log.warn("无法读取图片文件 [path={}]", filePath);
                return null;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 计算缩放后的尺寸，保持宽高比
            double scale = Math.min(
                    (double) THUMBNAIL_MAX_WIDTH / originalWidth,
                    (double) THUMBNAIL_MAX_HEIGHT / originalHeight
            );

            int thumbnailWidth = (int) (originalWidth * scale);
            int thumbnailHeight = (int) (originalHeight * scale);

            // 如果图片本身就很小，直接返回原图
            if (originalWidth <= THUMBNAIL_MAX_WIDTH && originalHeight <= THUMBNAIL_MAX_HEIGHT) {
                return Files.readAllBytes(filePath);
            }

            BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, THUMBNAIL_FORMAT, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("生成缩略图失败 [path={}, error={}]", filePath, e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] getOriginalImage(Long resourceId) {
        log.info("获取原始图片 [resourceId={}]", resourceId);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null || resource.getPath() == null) {
            log.warn("资源不存在 [resourceId={}]", resourceId);
            return null;
        }

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                log.warn("文件不存在 [path={}]", filePath);
                return null;
            }

            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取原始图片失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getImageExif(Long resourceId) {
        log.info("获取图片EXIF信息 [resourceId={}]", resourceId);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null) {
            throw new DataNotFoundException("资源不存在: " + resourceId);
        }

        Map<String, Object> exifData = new LinkedHashMap<>();

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                throw new DataNotFoundException("文件不存在");
            }

            String fileName = filePath.getFileName().toString().toLowerCase();
            String extension = fileName.contains(".") ?
                    fileName.substring(fileName.lastIndexOf(".") + 1) : "";

            exifData.put("format", extension.toUpperCase());

            if (isJpeg(extension) || isTiff(extension) || isPng(extension) || isGif(extension)) {
                extractAdvancedExif(filePath, exifData);
            } else {
                exifData.put("message", "该格式不支持EXIF提取");
            }
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("提取EXIF信息失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            exifData.put("error", e.getMessage());
        }

        return exifData;
    }

    @Override
    public Map<String, Object> getImageInfo(Long resourceId) {
        log.info("获取图片信息 [resourceId={}]", resourceId);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null) {
            throw new DataNotFoundException("资源不存在: " + resourceId);
        }

        Map<String, Object> imageInfo = new LinkedHashMap<>();
        imageInfo.put("resourceId", resource.getId());
        imageInfo.put("size", resource.getSize());

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                throw new DataNotFoundException("文件不存在");
            }

            String fileName = filePath.getFileName().toString();
            imageInfo.put("fileName", fileName);

            String extension = fileName.contains(".") ?
                    fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "";
            imageInfo.put("extension", extension);

            long fileSize = Files.size(filePath);
            imageInfo.put("fileSize", fileSize);
            imageInfo.put("fileSizeFormatted", formatFileSize(fileSize));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            imageInfo.put("createTime", sdf.format(resource.getCreateTime()));
            imageInfo.put("updateTime", sdf.format(resource.getUpdateTime()));

            if (isJpeg(extension) || isPng(extension) || isGif(extension)) {
                imageInfo.put("type", "IMAGE");
                imageInfo.put("supported", true);
            } else {
                imageInfo.put("type", extension.toUpperCase());
                imageInfo.put("supported", false);
            }

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取图片信息失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            imageInfo.put("error", e.getMessage());
        }

        return imageInfo;
    }

    @Override
    public Map<String, Object> getTextPreview(Long resourceId, Long offset, Long limit) {
        log.info("获取文本预览 [resourceId={}, offset={}, limit={}]", resourceId, offset, limit);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null) {
            throw new DataNotFoundException("资源不存在: " + resourceId);
        }

        Map<String, Object> previewInfo = new LinkedHashMap<>();
        previewInfo.put("resourceId", resource.getId());

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                throw new DataNotFoundException("文件不存在");
            }

            long fileSize = Files.size(filePath);
            previewInfo.put("totalSize", fileSize);

            if (offset == null || offset < 0) {
                offset = 0L;
            }
            if (limit == null || limit <= 0) {
                limit = 65536L;
            }
            if (offset + limit > MAX_TEXT_PREVIEW_SIZE) {
                if (fileSize > MAX_TEXT_PREVIEW_SIZE) {
                    limit = MAX_TEXT_PREVIEW_SIZE - offset;
                }
            }

            long actualLimit = Math.min(limit, fileSize - offset);
            if (actualLimit <= 0) {
                previewInfo.put("content", "");
                previewInfo.put("offset", offset);
                previewInfo.put("length", 0);
                previewInfo.put("truncated", fileSize > MAX_TEXT_PREVIEW_SIZE);
                return previewInfo;
            }

            byte[] bytes = Files.readAllBytes(filePath);
            int start = (int) Math.min(offset, bytes.length);
            int length = (int) Math.min(actualLimit, bytes.length - start);

            String content = new String(bytes, start, length, StandardCharsets.UTF_8);
            previewInfo.put("content", content);
            previewInfo.put("offset", offset);
            previewInfo.put("length", length);
            previewInfo.put("hasMore", offset + length < fileSize);
            previewInfo.put("truncated", fileSize > MAX_TEXT_PREVIEW_SIZE);

            String detectedEncoding = detectEncoding(bytes);
            previewInfo.put("encoding", detectedEncoding);

            String extension = getExtension(filePath.getFileName().toString());
            previewInfo.put("isCode", isCodeFile(extension));
            previewInfo.put("isLog", isLogFile(extension));

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取文本预览失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            previewInfo.put("error", e.getMessage());
        }

        return previewInfo;
    }

    @Override
    public Map<String, Object> getTextSummary(Long resourceId) {
        log.info("获取文本摘要 [resourceId={}]", resourceId);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null) {
            throw new DataNotFoundException("资源不存在: " + resourceId);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("resourceId", resource.getId());

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                throw new DataNotFoundException("文件不存在");
            }

            long fileSize = Files.size(filePath);
            summary.put("totalSize", fileSize);

            long readSize = (int) Math.min(fileSize, TEXT_SUMMARY_MAX_SIZE);
            byte[] bytes = Files.readAllBytes(filePath);

            int contentLength = (int) Math.min(readSize, bytes.length);
            String content = new String(bytes, 0, contentLength, StandardCharsets.UTF_8);

            summary.put("lineCount", content.split("\n").length);
            summary.put("charCount", content.length());
            summary.put("preview", content.substring(0, Math.min(500, content.length())));

            if (fileSize > TEXT_SUMMARY_MAX_SIZE) {
                summary.put("truncated", true);
                summary.put("originalSize", fileSize);
            }

            String extension = getExtension(filePath.getFileName().toString());
            summary.put("fileType", extension.toUpperCase());
            summary.put("isCode", isCodeFile(extension));
            summary.put("isLog", isLogFile(extension));

            List<String> keyPhrases = extractKeyPhrases(content);
            summary.put("keyPhrases", keyPhrases);

            String detectedEncoding = detectEncoding(bytes);
            summary.put("encoding", detectedEncoding);

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取文本摘要失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            summary.put("error", e.getMessage());
        }

        return summary;
    }

    @Override
    public Map<String, Object> getAudioMetadata(Long resourceId) {
        log.info("获取音频元数据 [resourceId={}]", resourceId);

        Resource resource = resourceDataService.getById(resourceId);
        if (resource == null) {
            throw new DataNotFoundException("资源不存在: " + resourceId);
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("resourceId", resource.getId());
        metadata.put("size", resource.getSize());

        try {
            Path filePath = Paths.get(homeDir, resource.getPath());
            if (!Files.exists(filePath)) {
                throw new DataNotFoundException("文件不存在");
            }

            String fileName = filePath.getFileName().toString();
            metadata.put("fileName", fileName);

            String extension = getExtension(fileName);
            metadata.put("format", extension.toUpperCase());

            long fileSize = Files.size(filePath);
            metadata.put("fileSize", fileSize);
            metadata.put("fileSizeFormatted", formatFileSize(fileSize));

            // 使用metadata-extractor提取音频元数据（支持MP3, WAV, OGG等）
            extractAudioMetadataWithLibrary(filePath, metadata);

            // 回退到传统ID3 TAG提取
            if (!metadata.containsKey("title") && isMp3(extension)) {
                extractMp3Metadata(filePath, metadata);
            }

            metadata.put("playUrl", "/v1/preview/audio/" + resourceId + "/play");

        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取音频元数据失败 [resourceId={}, error={}]", resourceId, e.getMessage());
            metadata.put("error", e.getMessage());
        }

        return metadata;
    }

    @Override
    @Transactional
    public void recordRecentUse(Long resourceId, String useType) {
        log.info("记录最近使用 [resourceId={}, useType={}]", resourceId, useType);

        RecentUse recentUse = RecentUse.builder()
                .resourceId(resourceId)
                .useType(useType != null ? useType : "PREVIEW")
                .usedAt(new Date())
                .build();

        recentUseDataService.save(recentUse);
        log.debug("最近使用记录已保存 [resourceId={}]", resourceId);
    }

    private void extractAdvancedExif(Path filePath, Map<String, Object> exifData) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(filePath.toFile());

            Map<String, Object> basicInfo = new LinkedHashMap<>();
            Map<String, Object> cameraInfo = new LinkedHashMap<>();
            Map<String, Object> imageParams = new LinkedHashMap<>();
            Map<String, Object> gpsInfo = new LinkedHashMap<>();

            boolean hasExif = false;

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    hasExif = true;
                    String tagName = tag.getTagName();
                    String description = tag.getDescription();

                    if (directory instanceof ExifIFD0Directory) {
                        if (tagName.contains("Make") || tagName.contains("Manufacturer")) {
                            cameraInfo.put("make", description);
                        } else if (tagName.contains("Model")) {
                            cameraInfo.put("model", description);
                        } else if (tagName.contains("Software")) {
                            cameraInfo.put("software", description);
                        } else if (tagName.contains("Date/Time")) {
                            basicInfo.put("dateTime", description);
                        }
                    } else if (directory instanceof ExifSubIFDDirectory) {
                        if (tagName.contains("Exposure Time")) {
                            imageParams.put("exposureTime", description);
                        } else if (tagName.contains("F-Number") || tagName.contains("Aperture")) {
                            imageParams.put("aperture", description);
                        } else if (tagName.contains("ISO")) {
                            imageParams.put("iso", description);
                        } else if (tagName.contains("Focal Length")) {
                            imageParams.put("focalLength", description);
                        } else if (tagName.contains("Flash")) {
                            imageParams.put("flash", description);
                        } else if (tagName.contains("White Balance")) {
                            imageParams.put("whiteBalance", description);
                        } else if (tagName.contains("Metering Mode")) {
                            imageParams.put("meteringMode", description);
                        }
                    } else if (directory instanceof JpegDirectory) {
                        if (tagName.contains("Image Width")) {
                            basicInfo.put("width", description);
                        } else if (tagName.contains("Image Height")) {
                            basicInfo.put("height", description);
                        }
                    } else if (directory instanceof GpsDirectory) {
                        if (tagName.contains("Latitude")) {
                            gpsInfo.put("latitude", description);
                        } else if (tagName.contains("Longitude")) {
                            gpsInfo.put("longitude", description);
                        } else if (tagName.contains("Altitude")) {
                            gpsInfo.put("altitude", description);
                        }
                    }
                }
            }

            exifData.put("hasExif", hasExif);
            if (!basicInfo.isEmpty()) {
                exifData.put("basic", basicInfo);
            }
            if (!cameraInfo.isEmpty()) {
                exifData.put("camera", cameraInfo);
            }
            if (!imageParams.isEmpty()) {
                exifData.put("parameters", imageParams);
            }
            if (!gpsInfo.isEmpty()) {
                exifData.put("gps", gpsInfo);
            }

            if (!hasExif) {
                exifData.put("message", "该图片不包含EXIF信息");
            }
        } catch (Exception e) {
            log.warn("EXIF提取失败 [path={}, error={}]", filePath, e.getMessage());
            exifData.put("error", e.getMessage());
        }
    }

    private void extractAudioMetadataWithLibrary(Path filePath, Map<String, Object> metadata) {
        try {
            Metadata audioMetadata = ImageMetadataReader.readMetadata(filePath.toFile());

            for (Directory directory : audioMetadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String tagName = tag.getTagName();
                    String description = tag.getDescription();

                    if (tagName.contains("Duration") || tagName.contains(" duration")) {
                        metadata.put("duration", description);
                        // 尝试解析秒数
                        try {
                            long durationMs = parseDuration(description);
                            if (durationMs > 0) {
                                metadata.put("durationMs", durationMs);
                                metadata.put("durationFormatted", formatDuration(durationMs));
                            }
                        } catch (Exception ignored) {
                        }
                    } else if (tagName.contains("Sample Rate")) {
                        metadata.put("sampleRate", description);
                    } else if (tagName.contains("Channels")) {
                        metadata.put("channels", description);
                    } else if (tagName.contains("Bitrate") || tagName.contains("Bit Rate")) {
                        metadata.put("bitrate", description);
                    } else if (tagName.contains("Artist") || tagName.contains("Performer")) {
                        metadata.put("artist", description);
                    } else if (tagName.contains("Title") || tagName.contains("Track")) {
                        metadata.put("title", description);
                    } else if (tagName.contains("Album")) {
                        metadata.put("album", description);
                    } else if (tagName.contains("Genre")) {
                        metadata.put("genre", description);
                    } else if (tagName.contains("Year") || tagName.contains("Date")) {
                        metadata.put("year", description);
                    }
                }
            }

            metadata.put("hasMetadata", true);
        } catch (Exception e) {
            log.warn("音频元数据提取失败 [path={}, error={}]", filePath, e.getMessage());
            metadata.put("metadataError", e.getMessage());
        }
    }

    private long parseDuration(String durationStr) {
        if (durationStr == null) return 0;
        // 尝试匹配 "mm:ss" 或 "hh:mm:ss" 格式
        String[] parts = durationStr.split(":");
        if (parts.length == 2) {
            return (Long.parseLong(parts[0].trim()) * 60 + Long.parseLong(parts[1].trim())) * 1000;
        } else if (parts.length == 3) {
            return (Long.parseLong(parts[0].trim()) * 3600 + Long.parseLong(parts[1].trim()) * 60 + Long.parseLong(parts[2].trim())) * 1000;
        }
        // 尝试直接解析毫秒
        try {
            return Long.parseLong(durationStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        long hours = mins / 60;
        mins = mins % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, mins, secs);
        }
        return String.format("%02d:%02d", mins, secs);
    }

    private String detectEncoding(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return "UTF-8";
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return "UTF-16BE";
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return "UTF-16LE";
        }

        Pattern asciiPattern = Pattern.compile("^[\\x20-\\x7E\\x09\\x0A\\x0D]*$");
        String sample = new String(bytes, 0, Math.min(1000, bytes.length), StandardCharsets.ISO_8859_1);
        if (asciiPattern.matcher(sample).matches()) {
            return "ASCII";
        }

        return "UTF-8";
    }

    private List<String> extractKeyPhrases(String content) {
        List<String> phrases = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\b\\w{4,}\\b");
        Matcher matcher = pattern.matcher(content);

        Map<String, Integer> wordCount = new HashMap<>();
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            wordCount.merge(word, 1, Integer::sum);
        }

        wordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> phrases.add(e.getKey()));

        return phrases;
    }

    private void extractMp3Metadata(Path filePath, Map<String, Object> metadata) {
        try {
            byte[] bytes = Files.readAllBytes(filePath);

            if (bytes.length > 128) {
                byte[] last128 = Arrays.copyOfRange(bytes, bytes.length - 128, bytes.length);
                if (last128[0] == 'T' && last128[1] == 'A' && last128[2] == 'G') {
                    String title = new String(last128, 3, 30, StandardCharsets.ISO_8859_1).trim();
                    String artist = new String(last128, 33, 30, StandardCharsets.ISO_8859_1).trim();
                    String album = new String(last128, 63, 30, StandardCharsets.ISO_8859_1).trim();

                    if (!title.isEmpty()) metadata.put("title", title);
                    if (!artist.isEmpty()) metadata.put("artist", artist);
                    if (!album.isEmpty()) metadata.put("album", album);
                }
            }

            metadata.put("hasMetadata", true);
        } catch (Exception e) {
            metadata.put("metadataError", e.getMessage());
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    private boolean isJpeg(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext);
    }

    private boolean isPng(String ext) {
        return "png".equals(ext);
    }

    private boolean isGif(String ext) {
        return "gif".equals(ext);
    }

    private boolean isTiff(String ext) {
        return "tiff".equals(ext) || "tif".equals(ext);
    }

    private boolean isMp3(String ext) {
        return "mp3".equals(ext);
    }

    private boolean isCodeFile(String ext) {
        Set<String> codeExtensions = Set.of(
                "java", "js", "ts", "jsx", "tsx", "py", "go", "rs", "c", "cpp", "h",
                "cs", "php", "rb", "swift", "kt", "scala", "vue", "html", "css", "scss",
                "sass", "less", "xml", "json", "yaml", "yml", "toml", "md", "txt",
                "sh", "bash", "zsh", "ps1", "bat", "cmd", "sql", "graphql", "r"
        );
        return codeExtensions.contains(ext.toLowerCase());
    }

    private boolean isLogFile(String ext) {
        return "log".equals(ext.toLowerCase());
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
