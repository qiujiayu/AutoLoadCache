package com.test.hessian;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import lombok.Data;

/**
 * @author: jiayu.qiu
 */
@Data
public class MyTO implements Serializable {

    private static final long serialVersionUID=-8977482387706144892L;

    private String id;

    private Timestamp time;

    private List<String> list;

}
