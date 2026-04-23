package com.hd.biz.resource;

import com.hd.dao.entity.ResourceChunk;
import com.hd.model.dto.MergeFileDto;
import com.hd.model.dto.ResponseDto;

import java.util.List;

/**
 * 资源业务接口。
 */
public interface ResourceBiz {

    boolean checkChunk(ResourceChunk chunk);

    void saveChunk(ResourceChunk chunk);

    void mergeChunk(MergeFileDto mergeFileDto);

    List<ResourceChunk> getUploadedChunks(String identifier);

    boolean verifyChunkIntegrity(String identifier, Integer chunkNumber, String expectedMd5);

    void cancelUpload(String identifier);

    int cleanupTimeoutUploads(int timeoutMinutes);

    ResponseDto transferTasks();

    int clearTransferTasks(String status);

    boolean clearTransferTask(String identifier);
}

