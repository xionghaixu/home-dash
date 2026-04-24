package com.hd.biz;

import com.hd.model.dto.SystemInfoDto;

/**
 * 系统业务接口。
 */
public interface SystemBiz {

	/**
	 * 查询系统信息。
	 *
	 * @return 系统信息
	 */
	SystemInfoDto systemInfo();
}

