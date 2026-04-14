package site.bitinit.pnd.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.bitinit.pnd.web.Constants;
import site.bitinit.pnd.web.controller.dto.MergeFileDto;
import site.bitinit.pnd.web.controller.dto.ResponseDto;
import site.bitinit.pnd.web.entity.File;
import site.bitinit.pnd.web.entity.ResourceChunk;
import site.bitinit.pnd.web.exception.DataFormatException;
import site.bitinit.pnd.web.service.FileService;
import site.bitinit.pnd.web.service.ResourceService;

import jakarta.servlet.http.HttpServletRequest;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 资源上传控制器。
 * 处理大文件分块上传的相关操作，包括分块检查、分块上传、分块合并、
 * 断点续传、完整性校验、取消上传等功能。
 *
 * 支持的功能：
 * 1. 分块上传：上传文件分块到服务器
 * 2. 断点续传：查询已上传的分块信息
 * 3. 完整性校验：验证分块数据的MD5值
 * 4. 取消上传：清理未完成的上传任务
 * 5. 合并分块：将所有分块合并为完整文件
 *
 * @author john
 * @date 2020-01-27
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION)
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * 构造函数，注入ResourceService依赖。
     *
     * @param resourceService 资源服务接口
     */
    @Autowired
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 检查文件分块是否已上传。
     * 用于实现断点续传功能，客户端在上传前先检查分块是否存在。
     *
     * @param chunk 包含文件标识符和分块编号的分块信息
     * @return 如果分块已上传返回成功响应，否则返回304状态码
     */
    @GetMapping("/resource/chunk")
    public ResponseEntity<ResponseDto> checkChunk(ResourceChunk chunk) {
        log.debug("检查文件分块请求 [identifier={}, chunkNumber={}]",
                chunk != null ? chunk.getIdentifier() : null,
                chunk != null ? chunk.getChunkNumber() : null);

        // 参数校验
        if (Objects.isNull(chunk)) {
            log.warn("检查分块失败：分块信息为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("分块信息不能为空"));
        }
        if (Objects.isNull(chunk.getIdentifier()) || chunk.getIdentifier().trim().isEmpty()) {
            log.warn("检查分块失败：文件标识符为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("文件标识符不能为空"));
        }

        if (resourceService.checkChunk(chunk)) {
            log.debug("文件分块已存在 [identifier={}, chunkNumber={}]",
                    chunk.getIdentifier(), chunk.getChunkNumber());
            return ResponseEntity.ok(ResponseDto.success("该文件块已经上传"));
        }
        log.debug("文件分块不存在 [identifier={}, chunkNumber={}]",
                chunk.getIdentifier(), chunk.getChunkNumber());
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .body(ResponseDto.success());
    }

    /**
     * 获取文件的所有已上传分块信息。
     * 用于断点续传功能，客户端可以查询已上传的分块，从中断处继续上传。
     *
     * @param identifier 文件唯一标识符
     * @return 已上传的分块列表
     */
    @GetMapping("/resource/chunks/{identifier}")
    public ResponseEntity<ResponseDto> getUploadedChunks(@PathVariable String identifier) {
        log.debug("获取已上传分块列表请求 [identifier={}]", identifier);

        // 参数校验
        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("获取已上传分块失败：文件标识符为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("文件标识符不能为空"));
        }

        java.util.List<ResourceChunk> chunks = resourceService.getUploadedChunks(identifier);

        log.info("已上传分块列表返回成功 [identifier={}, uploadedChunks={}]",
                identifier, chunks.size());
        return ResponseEntity.ok(ResponseDto.success(chunks));
    }

    /**
     * 上传文件分块。
     * 接收客户端上传的文件分块并保存到临时目录。
     * 支持自动重试和MD5完整性校验。
     *
     * @param chunk 包含分块数据和元信息的分块对象
     * @return 上传成功的响应对象
     */
    @PostMapping("/resource/chunk")
    public ResponseEntity<ResponseDto> uploadChunk(ResourceChunk chunk) {
        log.info("上传文件分块请求 [identifier={}, chunkNumber={}, fileName={}, fileSize={}]",
                chunk != null ? chunk.getIdentifier() : null,
                chunk != null ? chunk.getChunkNumber() : null,
                chunk != null ? chunk.getFilename() : null,
                (chunk != null && chunk.getFile() != null) ? chunk.getFile().getSize() : 0);

        // 参数校验
        if (Objects.isNull(chunk)) {
            throw new DataFormatException("分块信息不能为空");
        }
        if (Objects.isNull(chunk.getIdentifier()) || chunk.getIdentifier().trim().isEmpty()) {
            throw new DataFormatException("文件标识符不能为空");
        }

        resourceService.saveChunk(chunk);

        log.info("文件分块上传成功 [identifier={}, chunkNumber={}]",
                chunk.getIdentifier(), chunk.getChunkNumber());
        return ResponseEntity.ok(ResponseDto.success());
    }

    /**
     * 验证指定分块的完整性。
     * 通过MD5校验检测分块是否损坏或被篡改。
     *
     * @param identifier   文件唯一标识符
     * @param chunkNumber  分块编号
     * @param expectedMd5  预期的MD5值（可选查询参数）
     * @return 包含验证结果的响应对象
     */
    @GetMapping("/resource/chunk/verify")
    public ResponseEntity<ResponseDto> verifyChunkIntegrity(
            @RequestParam String identifier,
            @RequestParam Integer chunkNumber,
            @RequestParam(required = false) String expectedMd5) {

        log.debug("验证分块完整性请求 [identifier={}, chunkNumber={}, expectedMd5={}]",
                identifier, chunkNumber, expectedMd5 != null ? expectedMd5 : "null");

        // 参数校验
        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("验证分块完整性失败：文件标识符为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("文件标识符不能为空"));
        }
        if (Objects.isNull(chunkNumber)) {
            log.warn("验证分块完整性失败：分块编号为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("分块编号不能为空"));
        }

        boolean isIntact = resourceService.verifyChunkIntegrity(identifier, chunkNumber, expectedMd5);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("identifier", identifier);
        resultData.put("chunkNumber", chunkNumber);
        resultData.put("isIntact", isIntact);

        if (isIntact) {
            log.info("分块完整性验证通过 [identifier={}, chunkNumber={}]",
                    identifier, chunkNumber);
            return ResponseEntity.ok(ResponseDto.success(resultData));
        } else {
            log.warn("分块完整性验证失败 [identifier={}, chunkNumber={}]",
                    identifier, chunkNumber);
            return ResponseEntity.ok(ResponseDto.success(resultData));
        }
    }

    /**
     * 取消文件上传。
     * 清理指定文件的所有临时分块和数据库记录。
     * 用于用户主动取消上传或清理超时的上传任务。
     *
     * @param identifier 文件唯一标识符
     * @return 取消成功的响应对象
     */
    @DeleteMapping("/resource/upload/{identifier}")
    public ResponseEntity<ResponseDto> cancelUpload(@PathVariable String identifier) {
        log.info("取消文件上传请求 [identifier={}]", identifier);

        // 参数校验
        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("取消上传失败：文件标识符为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("文件标识符不能为空"));
        }

        resourceService.cancelUpload(identifier);

        log.info("文件上传取消成功 [identifier={}]", identifier);
        return ResponseEntity.ok(ResponseDto.success("上传任务已取消"));
    }

    /**
     * 手动触发清理超时的未完成上传任务。
     * 管理员接口，用于手动清理长时间未完成的分块上传任务。
     *
     * @param timeoutMinutes 超时时间（分钟），可选参数，默认60分钟
     * @return 包含清理任务数量的响应对象
     */
    @PostMapping("/resource/cleanup")
    public ResponseEntity<ResponseDto> cleanupTimeoutUploads(
            @RequestParam(required = false, defaultValue = "60") int timeoutMinutes) {

        log.info("手动触发清理超时上传任务请求 [timeoutMinutes={}]", timeoutMinutes);

        int cleanedCount = resourceService.cleanupTimeoutUploads(timeoutMinutes);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("cleanedCount", cleanedCount);
        resultData.put("timeoutMinutes", timeoutMinutes);

        log.info("超时上传任务清理完成 [cleanedCount={}, timeoutMinutes={}]",
                cleanedCount, timeoutMinutes);
        return ResponseEntity.ok(ResponseDto.success(resultData));
    }

    /**
     * 获取传输任务列表。
     *
     * @return 传输任务及摘要
     */
    @GetMapping("/resource/transfers")
    public ResponseEntity<ResponseDto> getTransferTasks() {
        log.info("获取传输任务列表请求");
        return ResponseEntity.ok(resourceService.transferTasks());
    }

    /**
     * 清理传输任务记录。
     *
     * @param status 需要清理的状态，默认completed
     * @return 清理结果
     */
    @DeleteMapping("/resource/transfers")
    public ResponseEntity<ResponseDto> clearTransferTasks(
            @RequestParam(required = false, defaultValue = "completed") String status) {
        log.info("清理传输任务记录请求 [status={}]", status);

        int clearedCount = resourceService.clearTransferTasks(status);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("status", status);
        resultData.put("clearedCount", clearedCount);

        return ResponseEntity.ok(ResponseDto.success(resultData));
    }

    /**
     * 清除单条传输任务记录。
     *
     * @param identifier 文件唯一标识符
     * @return 清除结果
     */
    @DeleteMapping("/resource/transfer/{identifier}")
    public ResponseEntity<ResponseDto> clearTransferTask(@PathVariable String identifier) {
        log.info("清除单条传输任务记录请求 [identifier={}]", identifier);

        if (identifier == null || identifier.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseDto.fail("文件标识符不能为空"));
        }

        boolean success = resourceService.clearTransferTask(identifier);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("identifier", identifier);
        resultData.put("cleared", success);

        if (success) {
            return ResponseEntity.ok(ResponseDto.success(resultData, "传输任务记录已清除"));
        } else {
            return ResponseEntity.ok(ResponseDto.success(resultData, "传输任务记录不存在"));
        }
    }

    /**
     * 合并文件分块。
     * 将所有分块合并成完整文件，并创建文件记录。
     *
     * @param fileDto 包含文件信息和唯一标识符的数据传输对象
     * @return 合并成功的响应对象
     */
    @PostMapping("/resource/merge")
    public ResponseEntity<ResponseDto> mergeResource(@RequestBody MergeFileDto fileDto) {
        log.info("合并文件分块请求 [identifier={}, fileName={}, fileSize={}]",
                fileDto != null ? fileDto.getIdentifier() : null,
                fileDto != null ? fileDto.getFileName() : null,
                fileDto != null ? fileDto.getSize() : 0);

        // 参数校验
        if (Objects.isNull(fileDto)) {
            throw new DataFormatException("合并文件信息不能为空");
        }
        if (Objects.isNull(fileDto.getIdentifier()) || fileDto.getIdentifier().trim().isEmpty()) {
            throw new DataFormatException("文件标识符不能为空");
        }

        resourceService.mergeChunk(fileDto);

        log.info("文件分块合并成功 [identifier={}, fileName={}, resourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), fileDto.getResourceId());
        return ResponseEntity.ok(ResponseDto.success());
    }

    /**
     * 处理EOF异常。
     * 当客户端中断上传连接时，记录警告日志。
     *
     * @param e EOF异常对象
     */
    @ExceptionHandler
    public void eofException(EOFException e) {
        log.warn("客户端中断上传连接 [error={}]", e.getMessage());
    }
}
