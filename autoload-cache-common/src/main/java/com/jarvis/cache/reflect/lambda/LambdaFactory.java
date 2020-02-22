package com.jarvis.cache.reflect.lambda;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;




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
 * In essence, this class simply wraps the functionality of {@link LambdaMetafactory#metafactory(MethodHandles.Lookup, String, MethodType, MethodType, MethodHandle, MethodType)}.
 * <br>
 * However, some additional logic is needed to handle the case of 
 * <ul>
 * <li>static Method vs instance method.
 * <li>primitive method parameters, vs Object (or subclasses thereof, including boxed primitives)
 * <li>private methods
 * </ul>
 * @author Anders Granau Høfft
 */
public class LambdaFactory {
	
	private static Field lookupClassAllowedModesField;
	private static final int ALL_MODES = (MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC);
		
	/**
	 * creates a Lambda with the same access rights as a Method with setAccessible()==true. 
	 * That is, both private, package, protected and public methods are accessible to the created Lambda.
	 * @param method A Method object which defines what to invoke.
	 * @return A dynamically generated class that implements the Lambda interface and the a method that corresponds to the Method. 
	 * The implementation offers invocation speed similar to that of a direct method invocation.
	 * @throws Throwable
	 */
	public static Lambda create(Method method) throws Throwable {
	 return privateCreate(method, false);
	}
	
	 /**
	  * Same as {@link #create(Method)} except that this method returns a Lambda that will <em>not</em> be subject to dynamic method dispatch.
	  * <p>Example:
	  * <br>Let class A implement a method called 'someMethod'. And let class B extend A and override 'someMethod'.
	  * <br>Then, calling {@link #createSpecial(Method)} with a Method object referring to A.someMethod, will return a Lambda 
	  * that calls A.someMethod, even when invoked with B as the instance.
	  */
	public static Lambda createSpecial(Method method) throws Throwable {
		return privateCreate(method, true);
	}
	
	private static Lambda privateCreate(Method method, boolean createSpecial) throws Throwable {
		Class<?> returnType = method.getReturnType();
		String signatureName = GenerateLambdaProcessor.getMethodName(returnType.getSimpleName());
		return createSpecial 
				? createSpecial(method, Lambda.class, signatureName)
				: create(method, Lambda.class, signatureName);
	}

	/**
	 * Same as {@link #create(Method)} but with an extra parameter that allows for more fine grained configuration of the access rights
	 * of the generated Lambda implementation.
	 * The lookup's access rights reflect the class, which created it. 
	 * To access private methods of a class using this constructor, the Lookup must either have been created in the given class, 
	 * or the Method must have setAccessible()==true. Create a Lookup like this: MethodHandles.lookup().
	 * @param method A Method object which defines what to invoke.
	 * @param lookup A Lookup describing the access rights of the generated Lambda. Create a Lookup like this: MethodHandles.lookup().
	 * @return A dynamically generated class that implements the Lambda interface and the a method that corresponds to the Method. 
	 * The implementation offers invocation speed similar to that of a direct method invocation.
	 * @throws Throwable
	 */
	public static Lambda create(Method method, MethodHandles.Lookup lookup) throws Throwable {
		return create(method, lookup, false);
	}
	
	/**
	 * Same as {@link #create(Method, MethodHandles.Lookup)} except that this method returns a Lambda that will <em>not</em> be subject to dynamic method dispatch.
	 * See {@link #createSpecial(Method)}
	 */
	public static Lambda createSpecial(Method method, MethodHandles.Lookup lookup) throws Throwable {
		return create(method, lookup, true);
	}
	
	private static Lambda create(Method method, MethodHandles.Lookup lookup, boolean invokeSpecial) throws Throwable {
		Class<?> returnType = method.getReturnType();
		String signatureName = GenerateLambdaProcessor.getMethodName(returnType.getSimpleName());
		return createLambda(method, lookup, Lambda.class, signatureName, invokeSpecial);
	}
	
	/**
	 * Similar to {@link #create(Method)}, except that this factory method returns a dynamically generated 
	 * implementation of the argument provided interface.
	 * The provided signatureName must identify a method, whose arguments corresponds to the Method. (If the Method
	 * is a non-static method the interface method's first parameter must be an Object, and the subsequent parameters
	 * must match the Method.)
	 * <p>Example:<br>
	 * Method method = MyClass.class.getDeclaredMethod("myStaticMethod", int.class, int.class);<br>
	 * IntBinaryOperator sam = LambdaFactory.create(method, IntBinaryOperator.class, "applyAsInt");<br>
	 * int result = sam.applyAsInt(3, 11);<br>
	 * @param method A Method object which defines what to invoke.
	 * @param interfaceClass The interface, which the dynamically generated class shall implement.
	 * @param signatatureName The name of an abstract method from the interface, which the dynamically create class shall implement.
	 * @return A dynamically generated implementation of the argument provided interface. The implementation offers invocation speed similar to that of a direct method invocation.
	 * @throws Throwable
	 */
	public static <T> T create(Method method, Class<T> interfaceClass, String signatatureName) throws Throwable {
		return create(method, interfaceClass, signatatureName, false);
	}
	
	/**
	 * Same as {@link #create(Method)} except that this method returns a Lambda that will <em>not</em> be subject to dynamic method dispatch.
	 * See {@link #createSpecial(Method)}
	 */
	public static <T> T createSpecial(Method method, Class<T> interfaceClass, String signatatureName) throws Throwable {
		return create(method, interfaceClass, signatatureName, true);
	}
	
	private static <T> T create(Method method, Class<T> interfaceClass, String signatureName, boolean invokeSpecial) throws Throwable {
		MethodHandles.Lookup lookup = MethodHandles.lookup().in(method.getDeclaringClass());
		setAccessible(lookup);
		return createLambda(method, lookup, interfaceClass, signatureName, invokeSpecial);
	}
	
	/**
	 * Same as {@link #create(Method, Class, String)}, but with an additional parameter in the form of a Lookup object.
	 * See {@link #create(Method, MethodHandles.Lookup)} for a description of the Lookup parameter.
	 * @param method
	 * @param lookup
	 * @param interfaceClass
	 * @param signatatureName
	 * @return
	 * @throws Throwable
	 */
	public static <T> T create(Method method, MethodHandles.Lookup lookup, Class<T> interfaceClass, String signatatureName) throws Throwable {
		return createLambda(method, lookup, interfaceClass, signatatureName, false);
	}
	
	public static <T> T createSpecial(Method method, MethodHandles.Lookup lookup, Class<T> interfaceClass, String signatatureName) throws Throwable {
		return createLambda(method, lookup, interfaceClass, signatatureName, true);
	}
	
	public static <T> T createLambda(Method method, MethodHandles.Lookup lookup, Class<T> interfaceClass, String signatatureName, boolean createSpecial) throws Throwable {
		if (method.isAccessible()){
			lookup = lookup.in(method.getDeclaringClass());
			setAccessible(lookup);
		}
		return privateCreateLambda(method, lookup, interfaceClass, signatatureName, createSpecial);
	}
	
	/**
	 * This method uses {@link LambdaMetafactory} to create a lambda. 
	 * <br>
	 * The lambda will implement the argument provided interface.
	 * <br>
	 * This interface is expected to contain a signature that matches the method, for which we are creating the lambda.
	 * In the context of the lambda-factory project, this interface will always be the samem, namely an auto-generate interface
	 * with abstract methods for all combinations of primitives + Object (up until some max number of arguments.)
	 * <p>
	 * 
	 * @param method The {@link Method} we are trying to create "direct invocation fast" access to.
	 * @param setAccessible a boolean flag indicating whether or not the returned lambda shall force access private methods. 
	 * This corresponds to {@link Method#setAccessible(boolean)}. 
	 * @param interfaceClass The interface, which the created lambda will implement. 
	 * In the context of the lambda-factory project this will always be the same, namely a compile time auto-generated interface. See {@link GenerateLambdaProcessor}.
	 * @param the name of the method from the interface, which shall be implemented. This argument exists for the sake of jUnit testing.
	 * @return An instance of the argument provided interface, which implements only 1 of the interface's methods, namely the one whose signature matches the methods, we are create fast access to.
	 * @throws Throwable
	 */
	private static <T> T privateCreateLambda(Method method, MethodHandles.Lookup lookup, Class<T> interfaceClass, String signatureName, boolean createSpecial) throws Throwable {
		MethodHandle methodHandle = createSpecial? lookup.unreflectSpecial(method, method.getDeclaringClass()) : lookup.unreflect(method);
		MethodType instantiatedMethodType = methodHandle.type();
		MethodType signature = createLambdaMethodType(method, instantiatedMethodType);

		CallSite site = createCallSite(signatureName, lookup, methodHandle, instantiatedMethodType, signature,interfaceClass);
		MethodHandle factory = site.getTarget();
		return (T) factory.invoke();
	}

	private static MethodType createLambdaMethodType(Method method, MethodType instantiatedMethodType) {
		boolean isStatic = Modifier.isStatic(method.getModifiers());
		MethodType signature = isStatic ? instantiatedMethodType : instantiatedMethodType.changeParameterType(0, Object.class);

		Class<?>[] params = method.getParameterTypes();
		for (int i=0; i<params.length; i++){
			if (Object.class.isAssignableFrom(params[i])){
				signature = signature.changeParameterType(isStatic ? i : i+1, Object.class);
			}
		}
		if (Object.class.isAssignableFrom(signature.returnType())){
			signature = signature.changeReturnType(Object.class);
		}
		
		return signature;
	}

	private static CallSite createCallSite(String signatureName, MethodHandles.Lookup lookup, MethodHandle methodHandle,
			MethodType instantiatedMethodType, MethodType signature, Class<?> interfaceClass) throws LambdaConversionException {
		return LambdaMetafactory.metafactory(
				lookup, 
				signatureName,
				MethodType.methodType(interfaceClass), 
				signature, 
				methodHandle, 
				instantiatedMethodType);
	}

	static void setAccessible(MethodHandles.Lookup lookup) throws NoSuchFieldException, IllegalAccessException {
		getLookupsModifiersField().set(lookup, ALL_MODES);
	}

	/**
	 * * Enable access to private methods
	 * Source: https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/
	 * @return
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	static Field getLookupsModifiersField() throws NoSuchFieldException, IllegalAccessException {
		if (lookupClassAllowedModesField == null || !lookupClassAllowedModesField.isAccessible()) {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);

			Field allowedModes = MethodHandles.Lookup.class.getDeclaredField("allowedModes");
			allowedModes.setAccessible(true);
			int modifiers = allowedModes.getModifiers();
			modifiersField.setInt(allowedModes, modifiers & ~Modifier.FINAL); //Remove the final flag (~ performs a "bitwise complement" on a numerical value)
			
			lookupClassAllowedModesField = allowedModes;
		}
		return lookupClassAllowedModesField;
	}

}