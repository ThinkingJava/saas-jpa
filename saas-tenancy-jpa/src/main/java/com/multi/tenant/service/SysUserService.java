package com.multi.tenant.service;


import com.multi.tenant.dto.SysUserDTO;
import com.multi.tenant.qo.SysUserQO;

import java.util.List;

public interface SysUserService {

    /**
     * 保存用户信息
     */
    Boolean saveUser(SysUserQO sysUserQO);

    /**
     * 分页查询
     */
    List<SysUserDTO> page(SysUserQO sysUserQO);

    Boolean updateUser(SysUserQO sysUserQO);
}
