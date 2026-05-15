package com.user.service;

import com.authorize.pojo.Permission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tool.PageBuilder;
import com.tool.PageResult;
import com.user.dao.UserMapper;
import com.user.pojo.*;
import com.user.service.inf.UserService;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.security.Permissions;
import java.util.*;

/**
 * @category
 * @date 2025/3/25 16:12
 * @description TODO
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Override
    @Transactional
    public PageResult queryUserByPage(UserParams params) {
        PageResult pageResult = new PageResult();
        IPage<User> userIPage = userMapper.queryUserByPage(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);


        RoleParams roleParams = new RoleParams();
        for(User user : userIPage.getRecords())
        {
            roleParams.setRoleName(user.getRolename());
            Role role = userMapper.queryRoleByRoleName(roleParams);
            user.setRoles(new HashSet<>());
            user.getRoles().add(role);
        }
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(userIPage.getTotal());
        pageResult.setData(userIPage.getRecords());

        return pageResult;
    }

    @Override
    @Transactional
    public List<User> queryUserLike(UserParams params) {
        List<User> users = userMapper.queryUserLike(params);
        RoleParams roleParams = new RoleParams();
        for(User user : users)
        {
            roleParams.setRoleName(user.getRolename());
            Role role = userMapper.queryRoleByRoleName(roleParams);
            user.setRoles(new HashSet<Role>());
            user.getRoles().add(role);
        }

        return users;
    }

    @Override
    public List<User> queryUserList(UserParams params) {
        List<User> users = userMapper.queryUsers(params.getUsername());

        return users;
    }

    @Override
    @Transactional
    public User findByUsername(String userName) {
        User user = userMapper.findByUsername(userName);
        UserParams userParams = new UserParams();
        userParams.setUsername(userName);
        List<Role> roles = userMapper.queryRoleIdByUserName(userParams);
        if(roles == null || roles.size() == 0)
        {
        	return null;
        }
        user.setRoles(new HashSet<>());
        Set<Long> set = new HashSet<>();
        StringBuilder roleNames = new StringBuilder();
        for(Role role : roles)
        {
            Role role1 = userMapper.queryRoleByRoleId(role);
            if(!set.contains(role1.getId()))
            {
                set.add(role1.getId());
                roleNames.append(role1.getRoleName()).append(",");
            }

            RoleParams roleParams = new RoleParams();
            roleParams.setId(role1.getId());
            List<Permission> permissions = userMapper.queryPermissionByRoleId(roleParams);
            role1.setPermissions(new HashSet<>());
            for(Permission permission : permissions)
            {
                role1.getPermissions().add(permission);
            }
            user.getRoles().add(role1);
        }
        user.setRolename(roleNames.substring(0, roleNames.length() - 1));

        return user;
    }

    @Override
    @Transactional
    public int addUser(User user)
    {
        int addCount = userMapper.addUser(user);
        User userFromDb = userMapper.findByUsername(user.getUsername());
        Long userId = userFromDb.getId();
        Set<Role> roles = user.getRoles();
        for(Role role : roles)
        {
            UserRole userRole = new UserRole();
            RoleParams roleParams = new RoleParams();
            roleParams.setRoleName(role.getRoleName());
            userMapper.queryRoleByRoleName(roleParams);
            userRole.setRoleId(role.getId());
            userRole.setUserId(userId);
            int addUserRoleCount = userMapper.addUserRole(userRole);
            if(addUserRoleCount <=0 )
            {
                addCount = -1;
            }
        }

        return addCount;
    }

    @Override
    @Transactional
    public int deleteUser(Long id) {
        int deleteCount = userMapper.deleteUserById(id);
        int deleteUserRoleCount = userMapper.deleteUserRoleByUserId(id);
        if(deleteCount < 0 || deleteUserRoleCount < 0)
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }

    @Override
    @Transactional
    public int updateUser(User params) {
        int updateCount = userMapper.updateUser(params);
        Long userId = params.getId();
        Set<Role> roles = params.getRoles();
        for(Role role : roles)
        {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            int d = userMapper.updateUserRole(userRole);
            if(d <= 0)
            {
                updateCount = -1;
            }
        }

        return updateCount;
    }

    @Override
    public Role queryRoleByRoleId(Role params) {
        Role role = userMapper.queryRoleByRoleId(params);

        return role;
    }

    @Override
    public List<Role> queryAllRoles()
    {
        List<Role> roles = userMapper.queryAllRoles();

        return roles;
    }

    @Override
    @Transactional
    public List<Role> queryRoles(@Param("params") RoleParams params) {
        List<Role> roles = userMapper.queryRoles(params);

        RoleParams roleParams = new RoleParams();
        List<Role> result = new ArrayList<>();
        Set<Long> set = new HashSet<>();
        for(Role role : roles)
        {
            if(set.contains(role.getId()))
            {
                continue;
            }
            else
            {
                set.add(role.getId());
            }
            roleParams.setId(role.getId());
            List<Permission> permissions = userMapper.queryPermissionByRoleId(roleParams);
            role.setPermissions(new HashSet<>());
            for(Permission permission : permissions)
            {
                role.getPermissions().add(permission);
            }
            result.add(role);
        }
        return result;
    }

    @Override
    @Transactional
    public PageResult queryRoleByPage(RoleParams params) {
        PageResult pageResult = new PageResult();
        IPage<Role> roleIPage = userMapper.queryRoleByPage(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);

        List<Role> result = new ArrayList<>();
        RoleParams roleParams = new RoleParams();
        Set<Long> set = new HashSet<>();
        for(Role role : roleIPage.getRecords())
        {
            if(set.contains(role.getId()))
            {
                continue;
            }
            else
            {
                set.add(role.getId());
            }
            roleParams.setId(role.getId());
            List<Permission> permissions = userMapper.queryPermissionByRoleId(roleParams);
            role.setPermissions(new HashSet<>());
            for(Permission permission : permissions)
            {
                role.getPermissions().add(permission);
            }
            result.add(role);
        }

        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal((long) result.size());
        pageResult.setData(result);

        return pageResult;
    }

    @Override
    @Transactional
    public int addRole(Role params) {
        int count = userMapper.addRole(params);
//        Long id = params.getId();
        RoleParams roleParams = new RoleParams();
        roleParams.setRoleName(params.getRoleName());
        Role role = userMapper.queryRoleByRoleName(roleParams);
        Set<Permission> permissions = params.getPermissions();
        for(Permission permission : permissions)
        {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permission.getId());
            int i = userMapper.addRolePermission(rolePermission);
            if(i <= 0)
            {
                count = -1;
            }
        }

        return count;
    }

    @Override
    @Transactional
    public int deleteRole(Long id) {
        int count = userMapper.deleteRoleById(id);
        int i = userMapper.deleteRolePermissionById(id);
        if(count >= 0 && i >= 0)
        {
            return count;
        }

        return -1;
    }

    @Override
    @Transactional
    public int updateRole(Role params) {
        int count = userMapper.updateRole(params);
        int count1 = userMapper.deleteRolePermissionById(params.getId());
        Set<Permission> permissions = params.getPermissions();
        if(permissions != null && permissions.size() > 0)
        {
            for(Permission permission : permissions)
            {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(params.getId());
                rolePermission.setPermissionId(permission.getId());
                int added = userMapper.addRolePermission(rolePermission);
                if(added < 0 || count1 < 0)
                {
                    count = -1;
                }
            }
        }

        return count;
    }

    @Override
    public List<Role> queryRoleIdByUserName(UserParams params) {
        List<Role> roles = userMapper.queryRoleIdByUserName(params);

        return roles;
    }

    @Override
    public List<Permission> queryPermissionByRoleId(RoleParams params) {
        List<Permission> permission = userMapper.queryPermissionByRoleId(params);
        return permission;
    }

    @Override
    @Transactional
    public List<Permission> queryPermissionByUserName(UserParams params) {
        List<Role> roles = userMapper.queryRoleIdByUserName(params);
        List<Permission> result = new ArrayList<>();
        Set<Long> set = new HashSet<>();
        for(Role role : roles)
        {
            RoleParams roleParams = new RoleParams();
            roleParams.setId(role.getId());
            List<Permission> permissions = userMapper.queryPermissionByRoleId(roleParams);
            for(Permission permission : permissions)
            {
                if(set.contains(permission.getId()))
                {
                    continue;
                }
                set.add(permission.getId());
                result.add(permission);
            }
        }

        return result;
    }

    @Override
    @Transactional
    public int modifyPassword(String username, String oldPassword, String password) {
        User user = userMapper.queryUserByName(username);
        int count = -1;
        if(user != null && user.getPassword().equals(oldPassword))
        {
            count = userMapper.modifyPassword(username, password);
        }


        return count;
    }

    @Override
    public Permission queryAllPermissById(Permission params) {
        return userMapper.queryAllPermissById(params);
    }

    @Override
    public List<Permission> queryAllPermisss() {
        return userMapper.queryAllPermisss();
    }

	@Override
	public List<UserRole> queryRoleUser(Role params) {
		
		return userMapper.queryRoleUser(params);
	}
}
