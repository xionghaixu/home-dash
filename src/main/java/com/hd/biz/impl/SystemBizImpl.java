package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.SystemBiz;
import com.hd.common.config.HomeDashProperties;
import com.hd.common.enums.FileTypeEnum;
import com.hd.dao.entity.File;
import com.hd.dao.service.FileDataService;
import com.hd.model.dto.SystemInfoDTO;
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
    public SystemInfoDTO systemInfo() {
        log.info("开始查询系统信息");

        SystemInfoDTO.SystemInfoDTOBuilder builder = SystemInfoDTO.builder();

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

        long totalFileNum = fileDataService.count();
        long folderNum = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.FOLDER.toString())
        );
        long videoNum = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.VIDEO.toString())
        );
        long audioNum = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.AUDIO.toString())
        );
        long pictureNum = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.PICTURE.toString())
        );
        long docNum = fileDataService.count(
                new LambdaQueryWrapper<File>().in(File::getType, List.of(
                        FileTypeEnum.DOC.toString(), FileTypeEnum.PDF.toString(), FileTypeEnum.TXT.toString(),
                        FileTypeEnum.PPT.toString(), FileTypeEnum.CODE.toString(), FileTypeEnum.WEB.toString()
                ))
        );
        long compressNum = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.COMPRESS_FILE.toString())
        );

        long normalFileNum = totalFileNum - folderNum;
        long otherNum = normalFileNum - videoNum - audioNum - pictureNum - docNum - compressNum;

        LambdaQueryWrapper<File> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.and(wrapper -> wrapper
                .ne(File::getType, FileTypeEnum.FOLDER.toString())
                .or(sub -> sub
                        .eq(File::getType, FileTypeEnum.FOLDER.toString())
                        .and(cond -> cond.ne(File::getParentId, File.ROOT_FILE.getId())
                                .or().notIn(File::getFileName, List.of("图片", "视频", "音频", "文档", "压缩包", "其他")))
                )
        );
        recentWrapper.orderByDesc(File::getCreateTime);
        recentWrapper.last("LIMIT 20");
        int recentUploadNum = fileDataService.list(recentWrapper).size();

        builder.totalNum((int) totalFileNum)
                .fileNum((int) normalFileNum)
                .folderNum((int) folderNum)
                .videoNum((int) videoNum)
                .audioNum((int) audioNum)
                .pictureNum((int) pictureNum)
                .docNum((int) docNum)
                .compressNum((int) compressNum)
                .otherNum((int) Math.max(otherNum, 0))
                .recentUploadNum(recentUploadNum);

        SystemInfoDTO result = builder.build();
        log.info("系统信息查询完成 [totalFiles={}, folders={}, videos={}, audios={}, pictures={}, docs={}, compress={}, recent={}]",
                totalFileNum, folderNum, videoNum, audioNum, pictureNum, docNum, compressNum, recentUploadNum);
        return result;
    }
}





