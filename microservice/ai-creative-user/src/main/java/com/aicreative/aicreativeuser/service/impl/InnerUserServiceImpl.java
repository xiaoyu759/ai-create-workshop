package com.aicreative.aicreativeuser.service.impl;

import com.aicreative.aicreativeuser.service.UserService;
import com.aicreative.aicreativeworkshop.innerservice.InnerUserService;
import com.aicreative.aicreativeworkshop.model.entity.User;
import com.aicreative.aicreativeworkshop.model.vo.UserVO;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 内部服务实现类
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }
}
