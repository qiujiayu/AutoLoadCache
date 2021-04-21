package com.jarvis.cache.reflect.lambda;

/**
 * Copyright 2016 Anders Granau Høfft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * END OF NOTICE
 * 
 * @author Anders Granau Høfft
 *
 */
enum MethodParameter {
  
  BOOLEAN(boolean.class),
  CHAR(char.class),
  SHORT(short.class),
  BYTE(byte.class),
  INT(int.class),
  FLOAT(float.class),
  LONG(long.class),
  DOUBLE(double.class),
  OBJECT(Object.class);
  
  private Class<?> type;
  
  private MethodParameter(Class<?> type){
    this.type = type;
  }
  
  Class<?> getType(){
    return type;
  }
  
  String getTypeAsSourceCodeString(){
    return getType().getSimpleName();
  }
  
}
