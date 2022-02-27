package com.multi.tenant.repository;

import com.multi.tenant.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {
}
