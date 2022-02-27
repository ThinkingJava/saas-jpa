package com.multi.tenant.service.impl;

import com.multi.tenant.dto.SysUserDTO;
import com.multi.tenant.entity.QSysRole;
import com.multi.tenant.entity.QSysUser;
import com.multi.tenant.entity.QSysUserRole;
import com.multi.tenant.entity.SysUser;
import com.multi.tenant.multitenancy.TenantContextHolder;
import com.multi.tenant.qo.SysUserQO;
import com.multi.tenant.repository.SysUserRepository;
import com.multi.tenant.service.SysUserService;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author: yangchenghuan
 * @Date: 2021/12/26 12:06
 * @Description:
 */
@Slf4j
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    SysUserRepository sysUserRepository;

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    private final QSysUser qSysUser = QSysUser.sysUser;
    private final QSysUserRole qSysUserRole = QSysUserRole.sysUserRole;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveUser(SysUserQO sysUserQO) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(sysUserQO, sysUser);
//        sysUser.setId(UUID.randomUUID().toString().replaceAll("-",""));
//        sysUser.setTenantId(TenantContextHolder.getTenantId());
        sysUserRepository.save(sysUser);
        return true;
    }

    @Override
    public List<SysUserDTO> page(SysUserQO sysUserQO) {
        BooleanExpression booleanExpression = jpaQueryFactory.select(QSysRole.sysRole.roleId).from(QSysRole.sysRole).where(QSysRole.sysRole.roleId.eq(qSysUserRole.roleId)).exists();
        List<SysUser> list = jpaQueryFactory.selectFrom(qSysUser)
                .join(qSysUserRole).on(qSysUserRole.userId.eq(qSysUser.id))
                .where(qSysUserRole.userId.eq(qSysUser.id))
                .where(booleanExpression).fetch();
//        List<SysUser> list = sysUserRepository.findAll();
        return list.stream().map(t -> {
            SysUserDTO sysUserDTO = new SysUserDTO();
            BeanUtils.copyProperties(t, sysUserDTO);
            return sysUserDTO;
        }).collect(Collectors.toList());
    }
}
