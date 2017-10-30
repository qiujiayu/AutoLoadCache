package com.test.fastjson;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * @author: jiayu.qiu
 */
@Data
public class User implements Serializable {

    private static final long serialVersionUID=-1360495988946413478L;

    private Integer id;

    private String name;

    private Date birthday;

}
