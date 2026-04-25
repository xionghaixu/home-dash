package com.hd.biz.impl;

import com.hd.common.config.HomeDashProperties;
import com.hd.common.enums.FileType;
import com.hd.common.exception.DataFormatException;
import com.hd.common.exception.DataNotFoundException;
import com.hd.common.exception.FileAlreadyExistsException;
import com.hd.dao.entity.File;
import com.hd.dao.entity.Resource;
import com.hd.dao.service.FileDataService;
import com.hd.dao.service.ResourceDataService;
import com.hd.model.dto.ResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileBizImpl 单元测试
 *
 * @author tester
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileBizImpl 业务逻辑测试")
class FileBizImplTest {

    @Mock
    private FileDataService fileDataService;

    @Mock
    private ResourceDataService resourceDataService;

    @Mock
    private HomeDashProperties homeDashProperties;

    @InjectMocks
    private FileBizImpl fileBiz;

    private File testFolder;
    private File testFile;
    private Resource testResource;

    @BeforeEach
    void setUp() {
        testFolder = File.builder()
                .id(1L)
                .fileName("testFolder")
                .type(FileType.FOLDER.toString())
                .parentId(0L)
                .createTime(new Date())
                .updateTime(new Date())
                .build();

        testFile = File.builder()
                .id(2L)
                .fileName("test.txt")
                .type(FileType.TXT.toString())
                .parentId(1L)
                .size(1024L)
                .resourceId(1L)
                .createTime(new Date())
                .updateTime(new Date())
                .build();

        testResource = Resource.builder()
                .id(1L)
                .path("2026/04/test.txt")
                .size(1024L)
                .link(1)
                .md5("abc123")
                .build();
    }

    @Nested
    @DisplayName("findByFileId 方法测试")
    class FindByFileIdTests {

        @Test
        @DisplayName("应返回文件详情")
        void shouldReturnFileDetail() {
            when(fileDataService.getById(2L)).thenReturn(testFile);

            ResponseDto result = fileBiz.findByFileId(2L);

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("应抛出异常当文件不存在")
        void shouldThrowExceptionWhenFileNotFound() {
            when(fileDataService.getById(999L)).thenReturn(null);

            assertThrows(DataNotFoundException.class, () ->
                fileBiz.findByFileId(999L)
            );
        }
    }

    @Nested
    @DisplayName("createFile 方法测试")
    class CreateFileTests {

        @Test
        @DisplayName("应成功创建文件")
        void shouldCreateFileSuccessfully() {
            when(fileDataService.getById(1L)).thenReturn(testFolder);
            when(fileDataService.count(any())).thenReturn(0L);

            File newFile = File.builder()
                    .fileName("newFile.txt")
                    .type(FileType.TXT.toString())
                    .parentId(1L)
                    .build();

            assertDoesNotThrow(() -> fileBiz.createFile(newFile));
            verify(fileDataService).save(any(File.class));
        }

        @Test
        @DisplayName("应抛出异常当父文件夹不存在")
        void shouldThrowExceptionWhenParentNotFound() {
            when(fileDataService.getById(999L)).thenReturn(null);

            File newFile = File.builder()
                    .fileName("newFile.txt")
                    .type(FileType.TXT.toString())
                    .parentId(999L)
                    .build();

            assertThrows(DataFormatException.class, () ->
                fileBiz.createFile(newFile)
            );
        }

        @Test
        @DisplayName("应抛出异常当父文件夹不是文件夹类型")
        void shouldThrowExceptionWhenParentIsNotFolder() {
            when(fileDataService.getById(2L)).thenReturn(testFile);

            File newFile = File.builder()
                    .fileName("newFile.txt")
                    .type(FileType.TXT.toString())
                    .parentId(2L)
                    .build();

            assertThrows(DataFormatException.class, () ->
                fileBiz.createFile(newFile)
            );
        }

        @Test
        @DisplayName("应抛出异常当文件名重复")
        void shouldThrowExceptionWhenFileNameDuplicate() {
            when(fileDataService.getById(1L)).thenReturn(testFolder);
            when(fileDataService.count(any())).thenReturn(1L);

            File newFile = File.builder()
                    .fileName("test.txt")
                    .type(FileType.TXT.toString())
                    .parentId(1L)
                    .build();

            assertThrows(FileAlreadyExistsException.class, () ->
                fileBiz.createFile(newFile)
            );
        }
    }

    @Nested
    @DisplayName("renameFile 方法测试")
    class RenameFileTests {

        @Test
        @DisplayName("应成功重命名文件")
        void shouldRenameFileSuccessfully() {
            when(fileDataService.getById(2L)).thenReturn(testFile);
            when(fileDataService.count(any())).thenReturn(0L);

            assertDoesNotThrow(() -> fileBiz.renameFile("renamed.txt", 2L));
            verify(fileDataService).updateById(any(File.class));
        }

        @Test
        @DisplayName("应抛出异常当文件不存在")
        void shouldThrowExceptionWhenFileNotFound() {
            when(fileDataService.getById(999L)).thenReturn(null);

            assertThrows(DataNotFoundException.class, () ->
                fileBiz.renameFile("renamed.txt", 999L)
            );
        }

        @Test
        @DisplayName("应抛出异常当文件名为空")
        void shouldThrowExceptionWhenFileNameEmpty() {
            assertThrows(DataFormatException.class, () ->
                fileBiz.renameFile("", 2L)
            );
        }

        @Test
        @DisplayName("应抛出异常当文件ID为空")
        void shouldThrowExceptionWhenIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                fileBiz.renameFile("newName.txt", null)
            );
        }
    }

    @Nested
    @DisplayName("deleteFiles 方法测试")
    class DeleteFilesTests {

        @Test
        @DisplayName("应成功批量删除文件")
        void shouldDeleteFilesSuccessfully() {
            when(fileDataService.listByIds(List.of(2L))).thenReturn(List.of(testFile));
            when(fileDataService.list()).thenReturn(List.of());
            doNothing().when(fileDataService).removeByIds(anyList());

            assertDoesNotThrow(() -> fileBiz.deleteFiles(List.of(2L)));
        }

        @Test
        @DisplayName("应抛出异常当文件ID列表为空")
        void shouldThrowExceptionWhenIdsEmpty() {
            assertThrows(NullPointerException.class, () ->
                fileBiz.deleteFiles(null)
            );
        }

        @Test
        @DisplayName("应处理部分文件不存在的情况")
        void shouldHandlePartialNotFound() {
            when(fileDataService.listByIds(List.of(2L, 999L))).thenReturn(List.of(testFile));
            when(fileDataService.list()).thenReturn(List.of());

            assertDoesNotThrow(() -> fileBiz.deleteFiles(List.of(2L, 999L)));
        }
    }

    @Nested
    @DisplayName("checkFileByMD5 方法测试")
    class CheckFileByMD5Tests {

        @Test
        @DisplayName("应返回存在结果当MD5存在")
        void shouldReturnExistsWhenMD5Exists() {
            when(resourceDataService.getOne(any())).thenReturn(testResource);

            ResponseDto result = fileBiz.checkFileByMD5("abc123");

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("应返回不存在结果当MD5不存在")
        void shouldReturnNotExistsWhenMD5NotFound() {
            when(resourceDataService.getOne(any())).thenReturn(null);

            ResponseDto result = fileBiz.checkFileByMD5("notexist");

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("应抛出异常当MD5为空")
        void shouldThrowExceptionWhenMD5Empty() {
            assertThrows(DataFormatException.class, () ->
                fileBiz.checkFileByMD5("")
            );
        }

        @Test
        @DisplayName("应抛出异常当MD5为空白字符串")
        void shouldThrowExceptionWhenMD5Blank() {
            assertThrows(DataFormatException.class, () ->
                fileBiz.checkFileByMD5("   ")
            );
        }

        @Test
        @DisplayName("应处理MD5大小写")
        void shouldHandleMD5CaseInsensitive() {
            when(resourceDataService.getOne(any())).thenReturn(null);

            ResponseDto result = fileBiz.checkFileByMD5("ABC123");

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("categorySummary 方法测试")
    class CategorySummaryTests {

        @Test
        @DisplayName("应返回分类摘要统计")
        void shouldReturnCategorySummary() {
            when(fileDataService.list()).thenReturn(List.of(testFile));

            ResponseDto result = fileBiz.categorySummary();

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }

        @Test
        @DisplayName("应正确统计各分类数量")
        void shouldCountEachCategory() {
            File imageFile = File.builder()
                    .id(3L)
                    .fileName("image.jpg")
                    .type(FileType.PICTURE.toString())
                    .parentId(0L)
                    .size(2048L)
                    .build();

            when(fileDataService.list()).thenReturn(List.of(testFile, imageFile));

            ResponseDto result = fileBiz.categorySummary();

            assertNotNull(result);
        }

        @Test
        @DisplayName("应正确处理空列表")
        void shouldHandleEmptyList() {
            when(fileDataService.list()).thenReturn(List.of());

            ResponseDto result = fileBiz.categorySummary();

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("loadResource 方法测试")
    class LoadResourceTests {

        @Test
        @DisplayName("应抛出异常当文件不存在")
        void shouldThrowExceptionWhenFileNotFound() {
            when(fileDataService.getById(999L)).thenReturn(null);

            assertThrows(DataNotFoundException.class, () ->
                fileBiz.loadResource(999L)
            );
        }

        @Test
        @DisplayName("应抛出异常当文件无关联资源")
        void shouldThrowExceptionWhenNoResource() {
            File fileWithoutResource = File.builder()
                    .id(3L)
                    .fileName("empty.txt")
                    .type(FileType.TXT.toString())
                    .parentId(0L)
                    .resourceId(null)
                    .build();
            when(fileDataService.getById(3L)).thenReturn(fileWithoutResource);

            assertThrows(DataNotFoundException.class, () ->
                fileBiz.loadResource(3L)
            );
        }
    }
}
