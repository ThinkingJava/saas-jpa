package com.multi.tenant.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "用户")
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    @ApiModelProperty(value = "主键id")
    private String id;

    /**
     * 用户名
     */
    @ApiModelProperty(value = "用户名")
    private String userName;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码")
    private String passWord;

    /**
     * 创建时间
     */
    @CreatedDate
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @LastModifiedDate
    @ApiModelProperty(value = "修改时间")
    private LocalDateTime updateTime;

    /**
     * 0-正常，1-删除
     */
    @ApiModelProperty(value = "删除标记,1:已删除,0:正常")
    private String delFlag;


    /**
     * 手机号
     */
    @ApiModelProperty(value = "手机号")
    private String phone;

    /**
     * 租户ID
     */
    @Column(name = "tenant_id", insertable = false,updatable = false)
    @ApiModelProperty(value = "用户所属租户id")
    private String tenantId;

    /**
     * 更新人
     */
    @CreatedBy
    @ApiModelProperty(value = "更新人")
    private String createName;
    /**
     * 更新人
     */
    @LastModifiedBy
    @ApiModelProperty(value = "更新人")
    private String updateName;

}
