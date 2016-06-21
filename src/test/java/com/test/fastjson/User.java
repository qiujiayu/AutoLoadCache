package com.test.fastjson;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {

    private static final long serialVersionUID=-1360495988946413478L;

    private Integer id;

    private String name;

    private Date birthday;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id=id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday=birthday;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", birthday=" + birthday + "]";
    }

}
