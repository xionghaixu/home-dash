package site.bitinit.pnd.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.bitinit.pnd.web.Constants;
import site.bitinit.pnd.web.controller.dto.InstantUploadDto;
import site.bitinit.pnd.web.controller.dto.MoveAndCopyFileDto;
import site.bitinit.pnd.web.controller.dto.ResponseDto;
import site.bitinit.pnd.web.dao.FileMapper;
import site.bitinit.pnd.web.entity.File;
import site.bitinit.pnd.web.exception.DataFormatException;
import site.bitinit.pnd.web.service.FileService;
import site.bitinit.pnd.web.service.ResourceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;

/**
 * 文件管理控制器。
 * 提供文件的增删改查、移动、复制、下载等操作的REST API接口。
 * 所有接口均遵循RESTful规范，返回统一的响应格式。
 *
 * @author john
 * @date 2020-01-05
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION)
public class FileController {

    private final FileService fileService;

    /**
     * 构造函数，注入FileService依赖。
     *
     * @param fileService 文件服务接口
     */
    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 根据父文件夹ID获取文件列表。
     * 支持按文件名、大小、修改时间等字段排序。
     *
     * @param parentId  父文件夹ID，0表示根目录
     * @param sortBy    排序字段，默认为name。支持：name、size、updateTime
     * @param sortOrder 排序方式，默认为asc。支持：asc、desc
     * @return 包含文件列表和文件夹路径的响应对象
     */
    @GetMapping("/file/parent/{parentId}")
    public ResponseEntity<ResponseDto> getFiles(
            @PathVariable(name = "parentId") Long parentId,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        log.info("获取文件列表请求 [parentId={}, sortBy={}, sortOrder={}]", parentId, sortBy, sortOrder);
        ResponseEntity<ResponseDto> response = ResponseEntity.ok(fileService.findByParentId(parentId, sortBy, sortOrder));
        log.debug("获取文件列表成功 [parentId={}, sortBy={}, sortOrder={}]", parentId, sortBy, sortOrder);
        return response;
    }

    /**
     * 获取最近上传文件列表。
     *
     * @param limit 返回数量上限
     * @return 最近上传文件列表
     */
    @GetMapping("/file/recent")
    public ResponseEntity<ResponseDto> getRecentFiles(@RequestParam(required = false) Integer limit) {
        log.info("获取最近上传文件列表请求 [limit={}]", limit);
        return ResponseEntity.ok(fileService.findRecentFiles(limit));
    }

    /**
     * 获取最近上传摘要统计。
     * 包含今日、本周、本月的上传数量和大小统计，以及最近上传文件列表。
     *
     * @param limit 最近文件列表数量上限，默认20
     * @return 最近上传摘要统计
     */
    @GetMapping("/file/recent-summary")
    public ResponseEntity<ResponseDto> getRecentUploadSummary(
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        log.info("获取最近上传摘要统计请求 [limit={}]", limit);
        return ResponseEntity.ok(fileService.getRecentUploadSummary(limit));
    }

    /**
     * 获取基础分类文件列表。
     *
     * @param category  分类名称
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 分类文件列表
     */
    @GetMapping("/file/category/{category}")
    public ResponseEntity<ResponseDto> getFilesByCategory(@PathVariable String category,
            @RequestParam(defaultValue = "updateTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        log.info("获取分类文件列表请求 [category={}, sortBy={}, sortOrder={}]",
                category, sortBy, sortOrder);
        return ResponseEntity.ok(fileService.findFilesByCategory(category, sortBy, sortOrder));
    }

    /**
     * 获取分类数量摘要。
     *
     * @return 分类数量摘要
     */
    @GetMapping("/file/category-summary")
    public ResponseEntity<ResponseDto> getCategorySummary() {
        log.info("获取分类摘要请求");
        return ResponseEntity.ok(fileService.categorySummary());
    }

    /**
     * 根据文件ID获取文件详情。
     *
     * @param fileId 文件ID
     * @return 包含文件详情的响应对象
     */
    @GetMapping("/file/{fileId}")
    public ResponseEntity<ResponseDto> getFile(@PathVariable(name = "fileId") Long fileId) {
        log.info("获取文件详情请求 [fileId={}]", fileId);
        ResponseEntity<ResponseDto> response = ResponseEntity.ok(fileService.findByFileId(fileId));
        log.debug("获取文件详情成功 [fileId={}]", fileId);
        return response;
    }

    /**
     * 创建新文件或文件夹。
     *
     * @param file   文件信息对象
     * @param result 数据验证结果
     * @return 创建成功的响应对象
     * @throws DataFormatException 当数据验证失败时抛出
     */
    @PostMapping("/file")
    public ResponseEntity<ResponseDto> createFile(@Valid @RequestBody File file, BindingResult result) {
        log.info("创建文件请求 [fileName={}, parentId={}, type={}]",
                file.getFileName(), file.getParentId(), file.getType());

        if (result.hasErrors()) {
            StringBuilder errors = new StringBuilder();
            for (FieldError fe : result.getFieldErrors()) {
                errors.append(fe.getDefaultMessage()).append("; ");
            }
            log.warn("文件创建参数验证失败 [fileName={}, parentId={}, errors={}]",
                    file.getFileName(), file.getParentId(), errors.toString());
            throw new DataFormatException(
                    String.format("文件创建参数验证失败 [fileName=%s, parentId=%d, errors=%s]",
                            file.getFileName(), file.getParentId(), errors.toString()));
        }

        fileService.createFile(file);
        log.info("文件创建成功 [fileId={}, fileName={}]", file.getId(), file.getFileName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.success());
    }

    /**
     * 单文件上传，支持 MD5 预检后的秒传兜底。
     *
     * @param file     上传文件
     * @param parentId 父目录ID
     * @return 上传结果
     */
    @PostMapping("/file/upload")
    public ResponseEntity<ResponseDto> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("parentId") Long parentId) {
        log.info("单文件上传请求 [fileName={}, parentId={}, size={}]",
                file != null ? file.getOriginalFilename() : null,
                parentId,
                file != null ? file.getSize() : 0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.uploadWithMD5(file, parentId));
    }

    /**
     * MD5 预检。
     *
     * @param md5 MD5值
     * @return 是否可秒传
     */
    @GetMapping("/file/md5/check")
    public ResponseEntity<ResponseDto> checkFileByMd5(@RequestParam("md5") String md5) {
        log.info("MD5预检请求 [md5={}]", md5);
        return ResponseEntity.ok(fileService.checkFileByMD5(md5));
    }

    /**
     * 秒传接口。
     *
     * @param dto 秒传请求参数
     * @return 秒传结果
     */
    @PostMapping("/file/instant-upload")
    public ResponseEntity<ResponseDto> instantUpload(@RequestBody InstantUploadDto dto) {
        if (Objects.isNull(dto)) {
            throw new DataFormatException("秒传参数不能为空");
        }
        log.info("秒传请求 [md5={}, fileName={}, parentId={}]",
                dto.getMd5(), dto.getFileName(), dto.getParentId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.instantUpload(dto.getMd5(), dto.getFileName(), dto.getParentId()));
    }

    /**
     * 校验文件完整性。
     *
     * @param fileId 文件ID
     * @return 完整性校验结果
     */
    @GetMapping("/file/{fileId}/verify-md5")
    public ResponseEntity<ResponseDto> verifyFileMd5(@PathVariable Long fileId) {
        log.info("文件MD5校验请求 [fileId={}]", fileId);
        return ResponseEntity.ok(fileService.verifyFileMD5(fileId));
    }

    /**
     * 重命名文件或文件夹。
     *
     * @param fileId 文件ID
     * @param file   包含新文件名的文件对象
     * @return 重命名成功的响应对象
     */
    @PutMapping("/file/{fileId}/rename")
    public ResponseEntity<ResponseDto> renameFile(@PathVariable(name = "fileId") Long fileId,
            @RequestBody File file) {
        log.info("重命名文件请求 [fileId={}, newFileName={}]", fileId,
                file != null ? file.getFileName() : null);

        // 参数校验
        if (Objects.isNull(file)) {
            throw new DataFormatException("文件信息不能为空");
        }
        if (Objects.isNull(file.getFileName()) || file.getFileName().trim().isEmpty()) {
            throw new DataFormatException("新文件名不能为空");
        }

        fileService.renameFile(file.getFileName(), fileId);
        log.info("文件重命名成功 [fileId={}, newFileName={}]", fileId, file.getFileName());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.success());
    }

    /**
     * 移动或复制文件。
     * 根据dto中的type字段判断操作类型：move表示移动，copy表示复制。
     *
     * <p><b>实现原理：</b>
     * <ol>
     *   <li>检查dto中的type字段，判断操作类型</li>
     *   <li>如果是移动操作（move）：targetIds必须只有一个值（只能移动到一个目标文件夹）</li>
     *   <li>如果是复制操作（copy）：允许复制到多个目标文件夹</li>
     *   <li>如果type既不是move也不是copy，抛出异常</li>
     * </ol>
     *
     * <p><b>业务规则：</b>
     * <ul>
     *   <li>移动操作：只能移动到一个目标文件夹</li>
     *   <li>复制操作：可以复制到多个目标文件夹</li>
     *   <li>不能将文件夹移动到自身或其子文件夹中</li>
     *   <li>目标目录存在同名文件时返回错误</li>
     * </ul>
     *
     * <p><b>错误码：</b>
     * <ul>
     *   <li>1101: 文件不存在</li>
     *   <li>1102: 目标目录已存在同名文件</li>
     *   <li>1201: 不能将文件夹移动到自身或子文件夹</li>
     *   <li>1202: 目标文件夹不存在</li>
     * </ul>
     *
     * @param dto 包含文件ID列表、目标文件夹ID列表和操作类型的数据传输对象
     * @return 操作结果响应
     * @throws DataFormatException 当操作类型不正确或参数无效时抛出
     */
    @PutMapping("/file")
    public ResponseEntity<ResponseDto> copyOrMoveFiles(@Valid @RequestBody MoveAndCopyFileDto dto) {
        log.info("文件操作请求 [fileIds={}, targetIds={}, type={}]",
                dto != null ? dto.getFileIds() : null,
                dto != null ? dto.getTargetIds() : null,
                dto != null ? dto.getType() : null);

        // 参数校验（由 @Valid 自动完成）
        if (Objects.isNull(dto)) {
            throw new DataFormatException("操作参数不能为空");
        }

        // 判断操作类型
        if (MoveAndCopyFileDto.MOVE_TYPE.equals(dto.getType())) {
            // 移动操作：验证targetIds必须只有一个值
            if (dto.getTargetIds().size() != 1) {
                log.warn("移动操作参数错误 [fileIds={}, targetIds={}, type={}]",
                        dto.getFileIds(), dto.getTargetIds(), dto.getType());
                throw new DataFormatException(
                        String.format("移动操作时targetIds必须只有一个值 [fileIds=%s, targetIds=%s, type=%s]",
                                dto.getFileIds(), dto.getTargetIds(), dto.getType()));
            }
            // 执行移动操作
            fileService.moveFiles(dto.getFileIds(), dto.getTargetIds().get(0));
            log.info("文件移动成功 [fileIds={}, targetId={}]", dto.getFileIds(), dto.getTargetIds().get(0));
        } else if (MoveAndCopyFileDto.COPY_TYPE.equals(dto.getType())) {
            // 复制操作：允许复制到多个目标文件夹
            fileService.copyFiles(dto.getFileIds(), dto.getTargetIds());
            log.info("文件复制成功 [fileIds={}, targetIds={}]", dto.getFileIds(), dto.getTargetIds());
        } else {
            // 操作类型不正确
            log.warn("操作类型不正确 [fileIds={}, targetIds={}, type={}]",
                    dto.getFileIds(), dto.getTargetIds(), dto.getType());
            throw new DataFormatException(
                    String.format("操作类型不正确，必须是move或copy [fileIds=%s, targetIds=%s, type=%s]",
                            dto.getFileIds(), dto.getTargetIds(), dto.getType()));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.success());
    }

    /**
     * 批量删除文件或文件夹。
     *
     * @param fileIds 要删除的文件ID列表
     * @return 删除成功的响应对象
     */
    @DeleteMapping("/file")
    public ResponseEntity<ResponseDto> deleteFiles(@RequestBody List<Long> fileIds) {
        log.info("删除文件请求 [fileIds={}]", fileIds);

        // 参数校验
        if (Objects.isNull(fileIds) || fileIds.isEmpty()) {
            throw new DataFormatException("要删除的文件ID列表不能为空");
        }

        fileService.deleteFiles(fileIds);
        log.info("文件删除成功 [fileIds={}]", fileIds);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.success());
    }

    /**
     * 下载文件。
     * 根据文件ID加载文件资源并提供下载。
     *
     * 实现原理：
     * 1. 根据文件ID加载文件资源和文件信息
     * 2. 尝试获取文件的MIME类型：
     * - 通过ServletContext根据文件扩展名推断MIME类型
     * - 如果无法推断，使用默认类型"application/octet-stream"
     * 3. 构建HTTP响应头：
     * - Content-Type: 文件的MIME类型
     * - Content-Disposition: 附件下载，文件名使用UTF-8编码
     * 4. 返回文件资源响应实体
     *
     * 文件名编码说明：
     * - 使用UTF-8编码文件名，支持中文文件名
     * - 格式：attachment; filename*=UTF-8''编码后的文件名
     * - 符合RFC 5987规范，确保浏览器正确解析文件名
     *
     * @param fileId  文件ID
     * @param request HTTP请求对象，用于获取文件MIME类型
     * @return 文件资源响应实体
     * @throws UnsupportedEncodingException 当文件名编码失败时抛出
     */
    @GetMapping("/file/{fileId}/download")
    public org.springframework.http.ResponseEntity<Resource> downloadFile(@PathVariable(name = "fileId") Long fileId,
            HttpServletRequest request) throws UnsupportedEncodingException {
        log.info("下载文件请求 [fileId={}]", fileId);

        // 加载文件资源和文件信息
        FileService.ResourceWrapper resourceWrapper = fileService.loadResource(fileId);

        // 尝试获取文件的MIME类型
        String contentType = null;
        try {
            // 通过ServletContext根据文件扩展名推断MIME类型
            contentType = request.getServletContext().getMimeType(resourceWrapper.resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            // 获取MIME类型失败，使用默认类型
            log.debug("无法获取文件MIME类型，使用默认类型 [fileId={}, fileName={}]",
                    fileId, resourceWrapper.file.getFileName());
        }

        // 如果无法推断MIME类型，使用默认类型
        if (Objects.isNull(contentType)) {
            contentType = "application/octet-stream";
        }

        log.info("文件下载开始 [fileId={}, fileName={}, contentType={}]",
                fileId, resourceWrapper.file.getFileName(), contentType);

        // 构建并返回文件下载响应
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        // 使用UTF-8编码文件名，支持中文文件名
                        "attachment; filename*=UTF-8''"
                                + URLEncoder.encode(resourceWrapper.file.getFileName(), "UTF-8"))
                .body(resourceWrapper.resource);
    }

    /**
     * 处理客户端中断下载异常。
     * 当客户端取消文件下载时，记录警告日志。
     *
     * @param e 客户端中断异常
     */
    @ExceptionHandler
    public void clientAbortException(ClientAbortException e) {
        log.warn("客户端取消文件下载 [error={}]", e.getMessage());
    }
}
