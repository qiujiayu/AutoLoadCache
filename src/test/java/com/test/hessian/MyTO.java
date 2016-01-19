package com.test.hessian;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

public class MyTO implements Serializable {

    private static final long serialVersionUID=-8977482387706144892L;

    private String id;

    private Timestamp time;

    private List<String> list;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id=id;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time=time;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list=list;
    }

}
