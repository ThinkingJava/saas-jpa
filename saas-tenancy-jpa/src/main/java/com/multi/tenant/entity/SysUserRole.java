package com.multi.tenant.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;

@Data
@ApiModel(value = "用户角色")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@IdClass(SysUserMultiKeys.class) //定义的联合主键
public class SysUserRole implements Serializable {


    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Id
    @ApiModelProperty(value = "用户id")
    private String userId;

    /**
     * 角色ID
     */
    @Id
    @ApiModelProperty(value = "角色id")
    private String roleId;

    /**
     * 租户ID
     */
    @ApiModelProperty(value = "用户所属租户id")
    private String tenantId;
}
