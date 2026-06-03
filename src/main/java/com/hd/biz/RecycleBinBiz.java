package com.hd.biz;

import com.hd.model.dto.ResponseDTO;

import java.util.List;

public interface RecycleBinBiz {
    
    ResponseDTO softDelete(List<Long> fileIds);
    
    ResponseDTO list();
    
    ResponseDTO restore(List<Long> fileIds);
    
    ResponseDTO empty();
}
