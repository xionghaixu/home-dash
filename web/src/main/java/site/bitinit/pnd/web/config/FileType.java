package site.bitinit.pnd.web.config;

/**
 * 文件类型枚举。
 * 定义系统中支持的文件类型分类，包括文件夹、视频、音频、PDF、压缩文件、图片、文档等。
 *
 * @author john
 * @date 2020-01-11
 */
public enum FileType {
    /** 默认文件类型，用于无法识别的文件扩展名。 */
    DEFAULT,

    /** 文件夹类型，表示一个目录。 */
    FOLDER,

    /** 视频文件类型，如mp4、flv、avi、mkv等。 */
    VIDEO,

    /** 音频文件类型，如mp3等。 */
    AUDIO,

    /** PDF文档类型。 */
    PDF,

    /** 压缩文件类型，如zip、rar、7z、tar.gz等。 */
    COMPRESS_FILE,

    /** 图片文件类型，如png、jpg、jpeg、gif、ico等。 */
    PICTURE,

    /** Word文档类型，如doc、docx等。 */
    DOC,

    /** PowerPoint文档类型，如ppt、pptx等。 */
    PPT,

    /** 纯文本文件类型，如txt等。 */
    TXT,

    /** BitTorrent种子文件类型，如torrent等。 */
    TORRENT,

    /** 网页文件类型，如html、htm等。 */
    WEB,

    /** 代码文件类型，如java、js、py、go、vue、json等。 */
    CODE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
