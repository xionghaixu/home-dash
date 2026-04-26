package com.hd.controller;

import com.hd.biz.FileBiz;
import com.hd.common.exception.DataFormatException;
import com.hd.common.exception.DataNotFoundException;
import com.hd.dao.entity.File;
import com.hd.model.dto.ResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileController 单元测试
 *
 * @author tester
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FileController REST接口测试")
class FileControllerTest {

    @Mock
    private FileBiz fileBiz;

    @InjectMocks
    private FileController fileController;

    private File testFolder;
    private File testFile;

    @BeforeEach
    void setUp() {
        testFolder = File.builder()
                .id(1L)
                .fileName("testFolder")
                .type("FOLDER")
                .parentId(0L)
                .build();

        testFile = File.builder()
                .id(2L)
                .fileName("test.txt")
                .type("TXT")
                .parentId(1L)
                .size(1024L)
                .resourceId(1L)
                .build();
    }

    @Nested
    @DisplayName("GET /file/parent/{parentId} 测试")
    class GetFilesTests {

        @Test
        @DisplayName("应返回指定父目录的文件列表")
        void shouldReturnFileListByParentId() {
            ResponseDto mockResponse = ResponseDto.success(List.of(testFile));
            when(fileBiz.findByParentId(eq(1L), anyString(), anyString())).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getFiles(1L, "name", "asc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(200, response.getBody().getCode());
        }

        @Test
        @DisplayName("应使用默认排序参数")
        void shouldUseDefaultSortParams() {
            ResponseDto mockResponse = ResponseDto.success(List.of());
            when(fileBiz.findByParentId(eq(0L), eq("name"), eq("asc"))).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getFiles(0L, "name", "asc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("应处理降序排序")
        void shouldHandleDescendingSort() {
            ResponseDto mockResponse = ResponseDto.success(List.of(testFile));
            when(fileBiz.findByParentId(eq(1L), eq("size"), eq("desc"))).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getFiles(1L, "size", "desc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /file/recent 测试")
    class GetRecentFilesTests {

        @Test
        @DisplayName("应返回最近上传的文件列表")
        void shouldReturnRecentFiles() {
            ResponseDto mockResponse = ResponseDto.success(List.of(testFile));
            when(fileBiz.findRecentFiles(eq(10))).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getRecentFiles(10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("应处理limit为null的情况")
        void shouldHandleNullLimit() {
            ResponseDto mockResponse = ResponseDto.success(List.of());
            when(fileBiz.findRecentFiles(isNull())).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getRecentFiles(null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /file/recent-summary 测试")
    class GetRecentUploadSummaryTests {

        @Test
        @DisplayName("应返回上传摘要统计")
        void shouldReturnUploadSummary() {
            ResponseDto mockResponse = ResponseDto.success(Map.of("count", 5));
            when(fileBiz.getRecentUploadSummary(eq(20))).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getRecentUploadSummary(20);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("应使用默认limit值")
        void shouldUseDefaultLimit() {
            ResponseDto mockResponse = ResponseDto.success(Map.of("count", 0));
            when(fileBiz.getRecentUploadSummary(any())).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getRecentUploadSummary(null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /file/{fileId} 测试")
    class GetFileTests {

        @Test
        @DisplayName("应返回文件详情")
        void shouldReturnFileDetail() {
            ResponseDto mockResponse = ResponseDto.success(testFile);
            when(fileBiz.findByFileId(eq(2L))).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getFile(2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("应抛出异常当文件不存在")
        void shouldThrowExceptionWhenFileNotFound() {
            when(fileBiz.findByFileId(eq(999L))).thenThrow(new DataNotFoundException("文件不存在"));

            assertThrows(DataNotFoundException.class, () ->
                fileController.getFile(999L)
            );
        }
    }

    @Nested
    @DisplayName("GET /file/category/{category} 测试")
    class GetFilesByCategoryTests {

        @Test
        @DisplayName("应返回指定分类的文件列表")
        void shouldReturnFilesByCategory() {
            ResponseDto mockResponse = ResponseDto.success(List.of(testFile));
            when(fileBiz.findFilesByCategory(eq("picture"), anyString(), anyString())).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getFilesByCategory("picture", "name", "asc");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("应抛出异常当分类无效")
        void shouldThrowExceptionForInvalidCategory() {
            when(fileBiz.findFilesByCategory(eq("invalid"), anyString(), anyString()))
                    .thenThrow(new DataFormatException("不支持的分类"));

            assertThrows(DataFormatException.class, () ->
                fileController.getFilesByCategory("invalid", "name", "asc")
            );
        }
    }

    @Nested
    @DisplayName("GET /file/category-summary 测试")
    class GetCategorySummaryTests {

        @Test
        @DisplayName("应返回分类摘要")
        void shouldReturnCategorySummary() {
            ResponseDto mockResponse = ResponseDto.success(List.of());
            when(fileBiz.categorySummary()).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.getCategorySummary();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("GET /file/md5/check 测试")
    class CheckFileByMd5Tests {

        @Test
        @DisplayName("应返回MD5检查结果")
        void shouldReturnMD5CheckResult() {
            ResponseDto mockResponse = ResponseDto.success(Map.of("exists", true));
            when(fileBiz.checkFileByMD5(eq("abc123"))).thenReturn(mockResponse);

            ResponseEntity<ResponseDto> response = fileController.checkFileByMd5("abc123");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("应抛出异常当MD5为空")
        void shouldThrowExceptionWhenMD5Empty() {
            when(fileBiz.checkFileByMD5(eq(""))).thenThrow(new DataFormatException("MD5值不能为空"));

            assertThrows(DataFormatException.class, () ->
                fileController.checkFileByMd5("")
            );
        }
    }

    @Nested
    @DisplayName("POST /file 测试")
    class CreateFileTests {

        @Test
        @DisplayName("应成功创建文件")
        void shouldCreateFileSuccessfully() {
            File newFile = File.builder()
                    .fileName("newFile.txt")
                    .type("TXT")
                    .parentId(1L)
                    .build();
            BindingResult bindingResult = new BeanPropertyBindingResult(newFile, "file");

            doNothing().when(fileBiz).createFile(any(File.class));

            ResponseEntity<ResponseDto> response = fileController.createFile(newFile, bindingResult);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("PUT /file/{fileId}/rename 测试")
    class RenameFileTests {

        @Test
        @DisplayName("应成功重命名文件")
        void shouldRenameFileSuccessfully() {
            File fileWithNewName = File.builder()
                    .fileName("renamed.txt")
                    .build();

            doNothing().when(fileBiz).renameFile(anyString(), eq(2L));

            ResponseEntity<ResponseDto> response = fileController.renameFile(2L, fileWithNewName);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(fileBiz).renameFile("renamed.txt", 2L);
        }

        @Test
        @DisplayName("应抛出异常当文件名为空")
        void shouldThrowExceptionWhenFileNameEmpty() {
            File emptyNameFile = File.builder()
                    .fileName("")
                    .build();

            assertThrows(DataFormatException.class, () ->
                fileController.renameFile(2L, emptyNameFile)
            );
        }
    }

    @Nested
    @DisplayName("DELETE /file 测试")
    class DeleteFilesTests {

        @Test
        @DisplayName("应成功批量删除文件")
        void shouldDeleteFilesSuccessfully() {
            List<Long> fileIds = List.of(1L, 2L, 3L);

            doNothing().when(fileBiz).deleteFiles(anyList());

            ResponseEntity<ResponseDto> response = fileController.deleteFiles(fileIds);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(fileBiz).deleteFiles(fileIds);
        }

        @Test
        @DisplayName("应抛出异常当文件ID列表为空")
        void shouldThrowExceptionWhenIdsEmpty() {
            assertThrows(DataFormatException.class, () ->
                fileController.deleteFiles(null)
            );
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlerTests {

        @Test
        @DisplayName("应正确处理DataNotFoundException")
        void shouldHandleDataNotFoundException() {
            when(fileBiz.findByFileId(eq(999L))).thenThrow(new DataNotFoundException("文件不存在"));

            assertThrows(DataNotFoundException.class, () ->
                fileController.getFile(999L)
            );
        }

        @Test
        @DisplayName("应正确处理DataFormatException")
        void shouldHandleDataFormatException() {
            when(fileBiz.findFilesByCategory(eq("invalid"), anyString(), anyString()))
                    .thenThrow(new DataFormatException("不支持的分类"));

            assertThrows(DataFormatException.class, () ->
                fileController.getFilesByCategory("invalid", "name", "asc")
            );
        }
    }
}
