package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.SystemBiz;
import com.hd.common.config.HomeDashProperties;
import com.hd.common.enums.FileType;
import com.hd.dao.entity.File;
import com.hd.dao.service.FileDataService;
import com.hd.model.dto.SystemInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.biz.impl
 * @createTime 2026/04/24 11:17
 * @description 系统业务实现。
 */
@Slf4j
@Service
public class SystemBizImpl implements SystemBiz {

    private final FileDataService fileDataService;
    private final HomeDashProperties homeDashProperties;

    @Autowired
    public SystemBizImpl(FileDataService fileDataService, HomeDashProperties homeDashProperties) {
        this.fileDataService = fileDataService;
        this.homeDashProperties = homeDashProperties;
    }

    @Override
    public SystemInfoDto systemInfo() {
        log.info("开始查询系统信息");

        SystemInfoDto.SystemInfoDtoBuilder builder = SystemInfoDto.builder();

        java.io.File systemFile = new java.io.File(homeDashProperties.getHomeDashDataDir());
        long totalSpace = systemFile.getTotalSpace();
        long usableSpace = systemFile.getUsableSpace();
        long usedSpace = totalSpace - usableSpace;
        int usagePercent = totalSpace > 0 ? (int) ((usedSpace * 100) / totalSpace) : 0;

        builder.totalCap(totalSpace)
                .usableCap(usableSpace)
                .usedCap(usedSpace)
                .usagePercent(usagePercent);

        String warningLevel;
        String warningMessage;
        if (usagePercent >= 90) {
            warningLevel = "critical";
            warningMessage = "存储空间即将耗尽，请及时清理";
        } else if (usagePercent >= 80) {
            warningLevel = "warning";
            warningMessage = "存储空间使用率较高，请注意";
        } else {
            warningLevel = "normal";
            warningMessage = null;
        }
        builder.warningLevel(warningLevel)
                .warningMessage(warningMessage);

        List<File> files = fileDataService.list();
        int totalFileNum = 0;
        int folderNum = 0;
        int videoNum = 0;
        int audioNum = 0;
        int pictureNum = 0;
        int docNum = 0;
        int compressNum = 0;

        for (File file : files) {
            totalFileNum++;
            String type = file.getType();
            if (FileType.FOLDER.toString().equals(type)) {
                folderNum++;
            }
            if (FileType.VIDEO.toString().equals(type)) {
                videoNum++;
            }
            if (FileType.AUDIO.toString().equals(type)) {
                audioNum++;
            }
            if (FileType.PICTURE.toString().equals(type)) {
                pictureNum++;
            }
            if (FileType.DOC.toString().equals(type)
                    || FileType.PDF.toString().equals(type)
                    || FileType.TXT.toString().equals(type)
                    || FileType.PPT.toString().equals(type)
                    || FileType.CODE.toString().equals(type)
                    || FileType.WEB.toString().equals(type)) {
                docNum++;
            }
            if (FileType.COMPRESS_FILE.toString().equals(type)) {
                compressNum++;
            }
        }

        int normalFileNum = totalFileNum - folderNum;
        int otherNum = normalFileNum - videoNum - audioNum - pictureNum - docNum - compressNum;
        LambdaQueryWrapper<File> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.orderByDesc(File::getCreateTime);
        recentWrapper.last("LIMIT 20");
        int recentUploadNum = fileDataService.list(recentWrapper).size();

        builder.totalNum(totalFileNum)
                .fileNum(totalFileNum - folderNum)
                .folderNum(folderNum)
                .videoNum(videoNum)
                .audioNum(audioNum)
                .pictureNum(pictureNum)
                .docNum(docNum)
                .compressNum(compressNum)
                .otherNum(Math.max(otherNum, 0))
                .recentUploadNum(recentUploadNum);

        SystemInfoDto result = builder.build();
        log.info("系统信息查询完成 [totalFiles={}, folders={}, videos={}, audios={}, pictures={}, docs={}, compress={}, recent={}]",
                totalFileNum, folderNum, videoNum, audioNum, pictureNum, docNum, compressNum, recentUploadNum);
        return result;
    }
}





