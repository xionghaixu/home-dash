package com.hd.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import com.hd.common.enums.FileType;
import com.hd.dao.mapper.FileMapper;
import com.hd.dao.entity.File;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 根目录初始化。
 * 为阶段一工作台补齐默认目录结构，避免新部署后根目录为空。
 */
@Slf4j
@Component
@DependsOn("databaseInitializer")
public class WorkspaceInitializer {

    private static final List<String> DEFAULT_FOLDERS = List.of("图片", "视频", "音频", "文档", "压缩包", "其他");

    private final FileMapper fileMapper;

    @Autowired
    public WorkspaceInitializer(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @PostConstruct
    public void initDefaultFolders() {
        for (String folderName : DEFAULT_FOLDERS) {
            Integer count = fileMapper.countByParentIdAndFileName(File.ROOT_FILE.getId(), folderName, null);
            if (count != null && count > 0) {
                continue;
            }

            File folder = File.builder()
                    .parentId(File.ROOT_FILE.getId())
                    .fileName(folderName)
                    .type(FileType.FOLDER.toString())
                    .build();
            fileMapper.save(folder);
            log.info("已初始化默认目录 [folderName={}, folderId={}]", folderName, folder.getId());
        }
    }
}

