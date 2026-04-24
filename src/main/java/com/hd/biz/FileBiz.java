package com.hd.biz;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import com.hd.dao.entity.File;
import com.hd.model.dto.ResponseDto;

import java.util.List;

/**
 * 文件业务接口。
 */
public interface FileBiz {

    ResponseDto findByParentId(Long parentId, String sortBy, String sortOrder);

    ResponseDto findRecentFiles(Integer limit);

    ResponseDto getRecentUploadSummary(Integer limit);

    ResponseDto findFilesByCategory(String category, String sortBy, String sortOrder);

    ResponseDto categorySummary();

    ResponseDto findByFileId(Long fileId);

    void createFile(File file);

    void renameFile(String fileName, Long id);

    void moveFiles(List<Long> ids, Long targetId);

    void copyFiles(List<Long> fileIds, List<Long> targetIds);

    void deleteFiles(List<Long> ids);

    ResourceWrapper loadResource(Long fileId);

    ResponseDto uploadWithMD5(MultipartFile file, Long parentId);

    ResponseDto checkFileByMD5(String md5);

    ResponseDto verifyFileMD5(Long fileId);

    ResponseDto instantUpload(String md5, String fileName, Long parentId);

    class ResourceWrapper {
        public Resource resource;
        public File file;

        public ResourceWrapper(Resource resource, File file) {
            this.resource = resource;
            this.file = file;
        }
    }
}

