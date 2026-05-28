package com.hd.biz;

import com.hd.model.dto.ResponseDto;

import java.util.List;

public interface RecycleBinBiz {
    
    ResponseDto softDelete(List<Long> fileIds);
    
    ResponseDto list();
    
    ResponseDto restore(List<Long> fileIds);
    
    ResponseDto empty();
}
