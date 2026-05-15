package com.user.service.inf;

import com.authorize.pojo.Permission;
import com.tool.PageResult;
import com.user.pojo.*;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @category
 * @date 2025/3/25 16:11
 * @description TODO
 */
public interface UserService {
    PageResult queryUserByPage(UserParams params);
    List<User> queryUserLike(UserParams params);

    List<User> queryUserList(UserParams params);

    User findByUsername(String userName);

    int addUser(User params);

    int deleteUser(Long id);

    int updateUser(User params);
    Role queryRoleByRoleId(Role params);
    List<Role> queryAllRoles();
    List<Role> queryRoles(RoleParams params);

    PageResult queryRoleByPage(RoleParams params);
    List<UserRole> queryRoleUser(Role params);

    int addRole(Role params);

    int deleteRole(Long id);

    int updateRole(Role params);

    List<Role> queryRoleIdByUserName(UserParams params);

    List<Permission> queryPermissionByRoleId(RoleParams params);

    List<Permission> queryPermissionByUserName(UserParams params);
    int modifyPassword(String username, String oldPassword, String password);

    Permission queryAllPermissById(Permission params);
    List<Permission> queryAllPermisss();
}
