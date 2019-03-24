package pojo;

import java.io.Serializable;

/**
 * @author andrew
 * @date 2019/03/24
 */
public class ResponseData implements Serializable {

    private String data;
    private Class<?> returnType;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }
}
