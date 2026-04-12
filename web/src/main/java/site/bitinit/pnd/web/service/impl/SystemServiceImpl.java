package site.bitinit.pnd.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.bitinit.pnd.web.config.FileType;
import site.bitinit.pnd.web.config.PndProperties;
import site.bitinit.pnd.web.controller.dto.SystemInfoDto;
import site.bitinit.pnd.web.dao.FileMapper;
import site.bitinit.pnd.web.service.SystemService;

import java.io.File;
import java.util.List;

/**
 * 系统服务实现类。
 * 实现系统信息查询的核心业务逻辑，包括存储容量统计和文件类型统计。
 *
 * @author john
 * @date 2020-02-10
 */
@Slf4j
@Service
public class SystemServiceImpl implements SystemService {

    private final FileMapper fileMapper;
    private final PndProperties pndProperties;

    /**
     * 构造函数，注入依赖对象。
     *
     * @param fileMapper    文件数据访问对象
     * @param pndProperties PND配置属性
     */
    @Autowired
    public SystemServiceImpl(FileMapper fileMapper, PndProperties pndProperties) {
        this.fileMapper = fileMapper;
        this.pndProperties = pndProperties;
    }

    /**
     * 获取系统信息。
     * 统计存储容量、文件数量、文件夹数量、视频数量、音频数量等信息。
     *
     * 实现原理：
     * 1. 获取存储设备的总容量和可用空间
     * 2. 查询所有文件记录
     * 3. 遍历文件记录，统计各类文件数量：
     * - 总文件数：所有文件记录的数量
     * - 文件夹数：类型为FOLDER的文件数量
     * - 普通文件数：总文件数减去文件夹数
     * - 视频数：类型为VIDEO的文件数量
     * - 音频数：类型为AUDIO的文件数量
     * 4. 构建并返回系统信息DTO
     *
     * 存储容量统计：
     * - totalSpace: 存储设备的总容量（字节）
     * - usableSpace: 存储设备的可用空间（字节）
     * - 通过File.getTotalSpace()和File.getUsableSpace()获取
     *
     * 性能考虑：
     * - 需要查询所有文件记录，对于大量文件可能影响性能
     * - 可以考虑使用SQL聚合查询优化性能
     *
     * @return 包含系统统计信息的数据传输对象
     */
    @Override
    public SystemInfoDto systemInfo() {
        log.debug("开始获取系统信息");

        SystemInfoDto.SystemInfoDtoBuilder builder = SystemInfoDto.builder();

        // 获取存储设备的总容量和可用空间
        File systemFile = new File(pndProperties.getPndDataDir());
        long totalSpace = systemFile.getTotalSpace();
        long usableSpace = systemFile.getUsableSpace();
        builder.totalCap(totalSpace)
                .usableCap(usableSpace);

        log.debug("存储容量统计完成 [totalSpace={}, usableSpace={}]", totalSpace, usableSpace);

        // 查询所有文件记录
        List<site.bitinit.pnd.web.entity.File> files = fileMapper.getAllFileType();

        // 统计各类文件数量
        int totalFileNum = 0;
        int folderNum = 0;
        int videoNum = 0;
        int audioNum = 0;
        int pictureNum = 0;
        int docNum = 0;
        int compressNum = 0;
        for (site.bitinit.pnd.web.entity.File file : files) {
            totalFileNum++;
            // 统计文件夹数量
            if (FileType.FOLDER.toString().equals(file.getType())) {
                folderNum++;
            }
            // 统计视频数量
            if (FileType.VIDEO.toString().equals(file.getType())) {
                videoNum++;
            }
            // 统计音频数量
            if (FileType.AUDIO.toString().equals(file.getType())) {
                audioNum++;
            }
            if (FileType.PICTURE.toString().equals(file.getType())) {
                pictureNum++;
            }
            if (FileType.DOC.toString().equals(file.getType())
                    || FileType.PDF.toString().equals(file.getType())
                    || FileType.TXT.toString().equals(file.getType())
                    || FileType.PPT.toString().equals(file.getType())
                    || FileType.CODE.toString().equals(file.getType())
                    || FileType.WEB.toString().equals(file.getType())) {
                docNum++;
            }
            if (FileType.COMPRESS_FILE.toString().equals(file.getType())) {
                compressNum++;
            }
        }

        // 构建并返回系统信息DTO
        int normalFileNum = totalFileNum - folderNum;
        int otherNum = normalFileNum - videoNum - audioNum - pictureNum - docNum - compressNum;
        int recentUploadNum = fileMapper.findRecentFiles(20).size();
        builder.totalNum(totalFileNum).fileNum(totalFileNum - folderNum)
                .folderNum(folderNum)
                .videoNum(videoNum)
                .audioNum(audioNum)
                .pictureNum(pictureNum)
                .docNum(docNum)
                .compressNum(compressNum)
                .otherNum(Math.max(otherNum, 0))
                .recentUploadNum(recentUploadNum);

        SystemInfoDto result = builder.build();
        log.info("系统信息获取完成 [totalFiles={}, folders={}, videos={}, audios={}, pictures={}, docs={}, compress={}, recent={}]",
                totalFileNum, folderNum, videoNum, audioNum, pictureNum, docNum, compressNum, recentUploadNum);

        return result;
    }
}
