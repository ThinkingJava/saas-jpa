package com.multi.tenant.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class SysUserMultiKeys implements Serializable {

    /**
     * 用户ID
     */
    @ApiModelProperty(value = "用户id")
    private String userId;

    /**
     * 角色ID
     */
    @ApiModelProperty(value = "角色id")
    private String roleId;


    public SysUserMultiKeys() {
    }

    public SysUserMultiKeys(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }


}
