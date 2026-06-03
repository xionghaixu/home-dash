package com.hd.biz;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import com.hd.dao.entity.File;
import com.hd.model.dto.ResponseDTO;

import java.util.List;

import com.hd.model.dto.CreateFileDTO;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.biz
 * @createTime 2026/04/24 11:17
 * @description 文件业务接口。
 */
public interface FileBiz {

    ResponseDTO findByParentId(Long parentId, String sortBy, String sortOrder);

    ResponseDTO findRecentFiles(Integer limit);

    ResponseDTO getRecentUploadSummary(Integer limit);

    ResponseDTO findFilesByCategory(String category, String sortBy, String sortOrder);

    ResponseDTO categorySummary();

    ResponseDTO findByFileId(Long fileId);

    ResponseDTO getFolderSize(Long folderId);

    void createFile(CreateFileDTO dto);

    void renameFile(String fileName, Long id);

    void moveFiles(List<Long> ids, Long targetId);

    void copyFiles(List<Long> fileIds, List<Long> targetIds);

    void deleteFiles(List<Long> ids);

    void permanentlyDelete(List<Long> ids);

    ResourceWrapper loadResource(Long fileId);

    ResponseDTO uploadWithMD5(MultipartFile file, Long parentId);

    ResponseDTO checkFileByMD5(String md5);

    ResponseDTO verifyFileMD5(Long fileId);

    ResponseDTO instantUpload(String md5, String fileName, Long parentId);

    String getTextFileContent(Long fileId);

    String getAudioFileUrl(Long fileId);

    class ResourceWrapper {
        public Resource resource;
        public File file;

        public ResourceWrapper(Resource resource, File file) {
            this.resource = resource;
            this.file = file;
        }
    }
}

