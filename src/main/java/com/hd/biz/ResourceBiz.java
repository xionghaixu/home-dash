package com.hd.biz;

import com.hd.model.dto.ResourceChunkDTO;
import com.hd.model.dto.MergeFileDTO;
import com.hd.model.dto.ResponseDTO;

import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.biz
 * @createTime 2026/04/24 11:17
 * @description 资源业务接口。
 */
public interface ResourceBiz {

    boolean checkChunk(ResourceChunkDTO chunk);

    void saveChunk(ResourceChunkDTO chunk);

    void mergeChunk(MergeFileDTO mergeFileDto);

    List<ResourceChunkDTO> getUploadedChunks(String identifier);

    boolean verifyChunkIntegrity(String identifier, Integer chunkNumber, String expectedMd5);

    void cancelUpload(String identifier);

    int cleanupTimeoutUploads(int timeoutMinutes);

    ResponseDTO transferTasks();

    int clearTransferTasks(String status);

    boolean clearTransferTask(String identifier);

    void updateTransferStatus(String identifier, String fileName, String status, Long totalSize, Long parentId);
}

