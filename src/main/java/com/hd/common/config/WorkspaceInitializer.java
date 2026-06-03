package com.hd.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import com.hd.common.enums.FileTypeEnum;
import com.hd.dao.entity.File;
import com.hd.dao.service.FileDataService;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.config
 * @createTime 2026/04/23 23:34
 * @description 根目录初始化。为阶段一工作台补齐默认目录结构，避免新部署后根目录为空。
 */
@Slf4j
@Component
@DependsOn("databaseInitializer")
public class WorkspaceInitializer {

    private static final List<String> DEFAULT_FOLDERS = List.of("图片", "视频", "音频", "文档", "压缩包", "其他");

    private final FileDataService fileDataService;

    @Autowired
    public WorkspaceInitializer(FileDataService fileDataService) {
        this.fileDataService = fileDataService;
    }

    @PostConstruct
    public void initDefaultFolders() {
        for (String folderName : DEFAULT_FOLDERS) {
            long count = fileDataService.lambdaQuery()
                    .eq(File::getParentId, File.ROOT_FILE.getId())
                    .eq(File::getFileName, folderName)
                    .count();
            if (count > 0) {
                continue;
            }

            File folder = File.builder()
                    .parentId(File.ROOT_FILE.getId())
                    .fileName(folderName)
                    .type(FileTypeEnum.FOLDER.toString())
                    .build();
            fileDataService.save(folder);
            log.info("已初始化默认目录 [folderName={}, folderId={}]", folderName, folder.getId());
        }

        repairOrphanFiles();
    }

    private void repairOrphanFiles() {
        List<Long> validFolderIds = fileDataService.lambdaQuery()
                .eq(File::getType, FileTypeEnum.FOLDER.toString())
                .list().stream()
                .map(File::getId)
                .toList();

        List<File> orphanFiles = fileDataService.lambdaQuery()
                .ne(File::getParentId, File.ROOT_FILE.getId())
                .list().stream()
                .filter(file -> !validFolderIds.contains(file.getParentId()))
                .toList();

        for (File orphanFile : orphanFiles) {
            Long invalidParentId = orphanFile.getParentId();
            orphanFile.setParentId(File.ROOT_FILE.getId());
            fileDataService.updateById(orphanFile);
            log.warn("已将孤儿文件回收到根目录 [fileId={}, fileName={}, invalidParentId={}]",
                    orphanFile.getId(), orphanFile.getFileName(), invalidParentId);
        }
    }
}

