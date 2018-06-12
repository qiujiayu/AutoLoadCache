package com.jarvis.cache.reflect.generics;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

/**
 * 代码拷贝来自：sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
 * 
 * @author jiayu.qiu
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private Type[] actualTypeArguments;

    private Class<?> rawType;

    private Type ownerType;

    private ParameterizedTypeImpl(Class<?> paramClass, Type[] paramArrayOfType, Type paramType) {
        this.actualTypeArguments = paramArrayOfType;
        this.rawType = paramClass;
        if (paramType != null) {
            this.ownerType = paramType;
        } else {
            this.ownerType = paramClass.getDeclaringClass();
        }
        validateConstructorArguments();
    }

    private void validateConstructorArguments() {
        @SuppressWarnings("rawtypes")
        TypeVariable[] arrayOfTypeVariable = this.rawType.getTypeParameters();

        if (arrayOfTypeVariable.length != this.actualTypeArguments.length) {
            throw new MalformedParameterizedTypeException();
        }
        // for(int i=0; i < this.actualTypeArguments.length; i++);
    }

    public static ParameterizedTypeImpl make(Class<?> paramClass, Type[] paramArrayOfType, Type paramType) {
        return new ParameterizedTypeImpl(paramClass, paramArrayOfType, paramType);
    }

    @Override
    public Type[] getActualTypeArguments() {
        return (Type[]) this.actualTypeArguments.clone();
    }

    @Override
    public Class<?> getRawType() {
        return this.rawType;
    }

    @Override
    public Type getOwnerType() {
        return this.ownerType;
    }

    @Override
    public boolean equals(Object paramObject) {
        if ((paramObject instanceof ParameterizedType)) {
            ParameterizedType localParameterizedType = (ParameterizedType) paramObject;

            if (this == localParameterizedType) {
                return true;
            }
            Type localType1 = localParameterizedType.getOwnerType();
            Type localType2 = localParameterizedType.getRawType();

            return (this.ownerType == null ? localType1 == null : this.ownerType.equals(localType1))
                    && (this.rawType == null ? localType2 == null : this.rawType.equals(localType2))
                    && (Arrays.equals(this.actualTypeArguments, localParameterizedType.getActualTypeArguments()));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.actualTypeArguments) ^ (this.ownerType == null ? 0 : this.ownerType.hashCode())
                ^ (this.rawType == null ? 0 : this.rawType.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder localStringBuilder = new StringBuilder();

        if (this.ownerType != null) {
            if ((this.ownerType instanceof Class<?>))
                localStringBuilder.append(((Class<?>) this.ownerType).getName());
            else {
                localStringBuilder.append(this.ownerType.toString());
            }
            localStringBuilder.append(".");

            if ((this.ownerType instanceof ParameterizedTypeImpl)) {
                localStringBuilder.append(this.rawType.getName()
                        .replace(((ParameterizedTypeImpl) this.ownerType).rawType.getName() + "$", ""));
            } else {
                localStringBuilder.append(this.rawType.getName());
            }
        } else {
            localStringBuilder.append(this.rawType.getName());
        }
        if ((this.actualTypeArguments != null) && (this.actualTypeArguments.length > 0)) {
            localStringBuilder.append("<");
            int i = 1;
            for (Type localType : this.actualTypeArguments) {
                if (i == 0) {
                    localStringBuilder.append(", ");
                }
                if ((localType instanceof Class<?>)) {
                    localStringBuilder.append(((Class<?>) localType).getName());
                } else {
                    // if(null!=localType){
                    localStringBuilder.append(localType.toString());
                    // }
                }

                i = 0;
            }
            localStringBuilder.append(">");
        }

        return localStringBuilder.toString();
    }

}
