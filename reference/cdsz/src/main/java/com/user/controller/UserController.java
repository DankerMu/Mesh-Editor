package com.user.controller;

import com.authorize.pojo.Permission;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.tool.PageResult;
import com.user.pojo.*;
import com.user.service.inf.UserService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

import java.util.List;

/**
 * @category
 * @date 2025/3/25 16:18
 * @description TODO
 */
@RestController
@RequestMapping("/user/")
@Api(tags = "用户管理")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @PostMapping("queryUserLike")
    @ApiOperation("模糊查询用户")
    public List<User> queryUserLike(@RequestBody UserParams params)
    {
    	List<User> queryUserLike = userService.queryUserLike(params);
    	LogRecordParams params1 = new LogRecordParams("用户与权限管理", "模糊查询用户");
        logService.addLogRecord(params1);
    	
        return queryUserLike;
//        try {
//        } catch (Exception e) {
//            e.printStackTrace();
//            return PageResult.fail(e.getMessage());
//        }
    }

    @PostMapping("queryUserByPage")
    @ApiOperation("查询用户")
    public PageResult queryUserByPage(@RequestBody UserParams params)
    {
        try {
        	PageResult result = userService.queryUserByPage(params);
        	LogRecordParams params1 = new LogRecordParams("用户与权限管理", "查找用户");
            logService.addLogRecord(params1);
        	
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.fail(e.getMessage());
        }
    }

    @PostMapping("queryUsers")
    @ApiOperation("查询用户")
    public List<User> queryUsers(@RequestBody UserParams params)
    {
    	List<User> userList = userService.queryUserList(params);
    	LogRecordParams params1 = new LogRecordParams("用户与权限管理", "查找用户");
        logService.addLogRecord(params1);
    	
        return userList;
    }

    @PostMapping("findByUserName")
    @ApiOperation("查找用户")
    public User findByUserName(@RequestBody User user)
    {
    	User username = userService.findByUsername(user.getUsername());
    	LogRecordParams params = new LogRecordParams("用户与权限管理", "查找用户");
        logService.addLogRecord(params);
        
        return username;
    }

    @PostMapping("addUser")
    @ApiOperation("新增用户")
    public int addUser(@RequestBody User user)
    {
        int count = userService.addUser(user);
        LogRecordParams params = new LogRecordParams("用户与权限管理", "添加用户成功");
        logService.addLogRecord(params);
        
        return count;
    }

    @PostMapping("deleteUser")
    @ApiOperation("删除用户")
    public int deleteUser(@RequestBody Long id)
    {
        int count = userService.deleteUser(id);
        LogRecordParams params = new LogRecordParams("用户与权限管理", "删除用户成功");
        logService.addLogRecord(params);
        return count;
    }

    @PostMapping("updateUser")
    @ApiOperation("修改用户")
    public int updateUser(@RequestBody User user)
    {
        int count = userService.updateUser(user);
        LogRecordParams params = new LogRecordParams("用户与权限管理", "修改用户成功");
        logService.addLogRecord(params);

        return count;
    }

    @GetMapping("queryAllRoles")
    @ApiOperation("查询所有角色")
    public List<Role> queryAllRoles()
    {
    	LogRecordParams params = new LogRecordParams("用户与权限管理", "查询所有角色");
        logService.addLogRecord(params);
        
        return userService.queryAllRoles();
    }

    @PostMapping("queryRoles")
    @ApiOperation("模糊查询角色")
    public List<Role> queryRoles(@RequestBody RoleParams params)
    {
    	LogRecordParams params1 = new LogRecordParams("用户与权限管理", "模糊查询角色");
        logService.addLogRecord(params1);
        
        return userService.queryRoles(params);
    }

    @PostMapping("queryRoleByPage")
    @ApiOperation("查询角色")
    public PageResult queryRoleByPage(@RequestBody RoleParams params)
    {
        PageResult result = userService.queryRoleByPage(params);
        LogRecordParams params1 = new LogRecordParams("用户与权限管理", "查询角色");
        logService.addLogRecord(params1);

        return result;
    }
    
    @PostMapping("queryRoleUser")
    @ApiOperation("查询角色下用户的个数")
    public int queryRoleUser(@RequestBody Role params)
    {
    	List<UserRole> list = userService.queryRoleUser(params);
    	int result = 0;
    	if(list != null)
    	{
    		result = list.size();
    	}

        return result;
    }

    @PostMapping("addRole")
    @ApiOperation("新增角色")
    public int addRole(@RequestBody Role params)
    {
        int count = userService.addRole(params);
        LogRecordParams params1 = new LogRecordParams("用户与权限管理", "新增角色");
        logService.addLogRecord(params1);

        return count;
    }

    @PostMapping("deleteRole")
    @ApiOperation("删除角色")
    public int deleteRole(@RequestBody RoleParams params)
    {
    	int count = userService.deleteRole(params.getId());
    	LogRecordParams params1 = new LogRecordParams("用户与权限管理", "删除角色");
        logService.addLogRecord(params1);

        return count;
    }

    @PostMapping("updateRole")
    @ApiOperation("修改角色")
    public int updateRole(@RequestBody Role params)
    {
        int count = userService.updateRole(params);
        LogRecordParams params1 = new LogRecordParams("用户与权限管理", "修改角色");
        logService.addLogRecord(params1);

        return count;
    }

    @PostMapping("modifyPassword")
    @ApiOperation("修改密码")
    public int modifyPassword(@RequestBody UserPwdParmas params)
    {
        int count = userService.modifyPassword(params.getUserName(), params.getOldPwd(), params.getNewPwd());
        LogRecordParams params1 = new LogRecordParams("用户与权限管理", "修改密码");
        logService.addLogRecord(params1);

        return count;
    }

//    Permission queryAllPermissById(Permission params);
//    List<Permission> queryAllPermisss();

    @GetMapping("queryAllPermisss")
    @ApiOperation("获取所有权限")
    public List<Permission> queryAllPermisss()
    {
        return userService.queryAllPermisss();
    }

    @PostMapping("queryAllPermissById")
    @ApiOperation("根据权限Id获取权限")
    public Permission queryAllPermissById(Permission params)
    {
        return userService.queryAllPermissById(params);
    }

    @PostMapping("queryPermissionByRoleId")
    @ApiOperation("根据角色Id获取权限")
    List<Permission> queryPermissionByRoleId(@RequestBody RoleParams params)
    {
        return userService.queryPermissionByRoleId(params);
    }

    @PostMapping("queryPermissionByUserName")
    @ApiOperation("根据用户名获取权限")
    List<Permission> queryPermissionByUserName(@RequestBody UserParams params)
    {
        List<Permission> permissions = userService.queryPermissionByUserName(params);

        return permissions;
    }
}
