package com.jarvis.cache.demo.condition;

import org.springframework.data.domain.Pageable;

import com.jarvis.cache.demo.entity.UserDO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询条件
 * 
 * @author jiayu.qiu
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserCondition extends UserDO {

    private static final long serialVersionUID = -5111314038991538777L;

    private Pageable pageable;
}
