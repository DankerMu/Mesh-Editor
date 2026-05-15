package com.user.dao;

import com.authorize.pojo.Permission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.user.pojo.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    User findByUsername(String username);

    List<User> queryUsers(String username);

    Set<Role> queryRoleByUserId(Long id);

    User queryUserByName(String username);
    IPage<User> queryUserByPage(IPage page, @Param("params") UserParams params);
    List<User> queryUserLike(@Param("params") UserParams params);

    int addUser(@Param("params") User params);
    int addUserRole(@Param("params") UserRole params);

    int deleteUserById(@Param("userId") Long id);

    int deleteUserRoleByUserId(@Param("userId") Long id);

    int updateUser(@Param("params") User params);

    int updateUserRole(@Param("params") UserRole params);

    Role queryRoleByRoleName(@Param("params") RoleParams params);
    Role queryRoleByRoleId(@Param("params") Role params);

    List<Role> queryAllRoles();

    IPage<Role> queryRoleByPage(IPage page, @Param("params") RoleParams params);
    List<Role> queryRoles(@Param("params") RoleParams params);

    int addRole(@Param("params") Role role);

    int addRolePermission(@Param("params") RolePermission rolePermission);

    int deleteRoleById(@Param("roleId") Long id);

    int deleteRolePermissionById(@Param("roleId") Long id);

    int updateRole(@Param("params") Role role);
    List<Role> queryRoleIdByUserName(@Param("params") UserParams params);
    List<Permission> queryPermissionByRoleId(@Param("params") RoleParams params);

    int modifyPassword(@Param("username")String username, @Param("password") String password);

    Permission queryAllPermissById(@Param("params") Permission params);
    List<Permission> queryAllPermisss();
    
    List<UserRole> queryRoleUser(@Param("params") Role params);
}