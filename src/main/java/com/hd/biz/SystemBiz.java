package com.hd.biz;

import com.hd.model.dto.SystemInfoDto;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.biz
 * @createTime 2026/04/24 11:17
 * @description 系统业务接口。
 */
public interface SystemBiz {

	/**
	 * 查询系统信息。
	 *
	 * @return 系统信息
	 */
	SystemInfoDto systemInfo();
}

