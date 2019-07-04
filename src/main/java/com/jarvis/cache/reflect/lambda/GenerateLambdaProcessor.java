package com.jarvis.cache.reflect.lambda;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
 * This AbstractProcessor only exists in order to auto-generate source code, namely an interface with multiple signatures.
 * Since the processor is part of the project, where the annotation it processes i located, it is important the compilation
 * proceeds in wel defined rounds, i.e. the processor must be compiled, before the annotated class, which triggers the processor.
 * Being a Maven project, this is currently handled in the pom.xml file, by the maven-compiler-plugin.
 * 
 * @author Anders Granau Høfft
 */
public class GenerateLambdaProcessor extends AbstractProcessor {

	private static final String END_OF_SIGNATURE = ");";
	private static final String NEWLINE_TAB = "\n\t";
	private static final String NEWLINE = "\n";
	private static final String INTERFACE_NAME = "Lambda";
	private static final String PACKAGE = "com.hervian.lambda";
	
	static final String METHOD_NAME = "invoke_for_";	
	private static final String METHOD_NAME_BOOLEAN 	= METHOD_NAME+boolean.class.getSimpleName();
	private static final String METHOD_NAME_CHAR 			= METHOD_NAME+char.class.getSimpleName();
	private static final String METHOD_NAME_BYTE 			= METHOD_NAME+byte.class.getSimpleName();
	private static final String METHOD_NAME_SHORT 		= METHOD_NAME+short.class.getSimpleName();
	private static final String METHOD_NAME_INT 			= METHOD_NAME+int.class.getSimpleName();
	private static final String METHOD_NAME_FLOAT 		= METHOD_NAME+float.class.getSimpleName();
	private static final String METHOD_NAME_LONG 			= METHOD_NAME+long.class.getSimpleName();
	private static final String METHOD_NAME_DOUBLE 		= METHOD_NAME+double.class.getSimpleName();
	private static final String METHOD_NAME_OBJECT 		= METHOD_NAME+Object.class.getSimpleName();
	private static final String METHOD_NAME_VOID 			= METHOD_NAME+void.class.getSimpleName();
	
	private static final String METHOD_NAME_PART_BOOLEAN 	= " "+METHOD_NAME_BOOLEAN	+"(";
	private static final String METHOD_NAME_PART_CHAR 		= " "+METHOD_NAME_CHAR		+"(";
	private static final String METHOD_NAME_PART_BYTE 		= " "+METHOD_NAME_BYTE		+"(";
	private static final String METHOD_NAME_PART_SHORT 		= " "+METHOD_NAME_SHORT		+"(";
	private static final String METHOD_NAME_PART_INT 			= " "+METHOD_NAME_INT			+"(";
	private static final String METHOD_NAME_PART_FLOAT 		= " "+METHOD_NAME_FLOAT		+"(";
	private static final String METHOD_NAME_PART_LONG 		= " "+METHOD_NAME_LONG		+"(";
	private static final String METHOD_NAME_PART_DOUBLE 	= " "+METHOD_NAME_DOUBLE	+"(";
	private static final String METHOD_NAME_PART_OBJECT 	= " "+METHOD_NAME_OBJECT	+"(";
	private static final String METHOD_NAME_PART_VOID 		= " "+METHOD_NAME_VOID		+"(";
	
	private Filer filer;
	private static boolean fileCreated;

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotataions = new LinkedHashSet<String>();
    annotataions.add(GenerateLambda.class.getCanonicalName());
    return annotataions;
  }
	
	@Override
	public void init(ProcessingEnvironment processingEnv) {
		filer = processingEnv.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (!roundEnv.processingOver() && !fileCreated) {
			Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateLambda.class);
			for (Element element : elements) {
				if (element.getKind() == ElementKind.CLASS) {
					GenerateLambda generateSignatureContainerAnnotation = element.getAnnotation(GenerateLambda.class);
					generateCode(PACKAGE, INTERFACE_NAME, generateSignatureContainerAnnotation);
					return true;
				}
				break;
			}
		}
		return true;
	}

	private void generateCode(String packageOfMarkerClass, String className, GenerateLambda generateSignatureContainerAnnotation) {
		String fqcn = packageOfMarkerClass + "." + className;
		try (Writer writer = filer.createSourceFile(fqcn).openWriter()) {
			StringBuilder javaFile = new StringBuilder();
			javaFile.append("package ").append(packageOfMarkerClass).append(";");
			javaFile.append("\n\n/**\n * Copyright 2016 Anders Granau Høfft"
					+ "\n * The invocation methods throws an AbstractMethodError, if arguments provided does not match "
					+ "\n * the type defined by the Method over which the lambda was created."
					+ "\n * A typical example of this is that the caller forget to cast a primitive number to its proper type. "
					+ "\n * Fx. forgetting to explicitly cast a number as a short, byte etc. "
					+ "\n * The AbstractMethodException will also be thrown if the caller does not provide"
					+ "\n * an Object instance as the first argument to a non-static method, and vice versa."
					+ "\n * @author Anders Granau Høfft").append("\n */")
							.append("\n@javax.annotation.Generated(value=\"com.hervian.lambda.GenerateLambdaProcessor\", date=\"").append(new Date()).append("\")")
							.append("\npublic interface " + className + "{\n");
			generateAbstractandConcreteMethods(javaFile, generateSignatureContainerAnnotation);
			javaFile.append("\n}");
			writer.write(javaFile.toString());
			fileCreated = true;
		} catch (IOException e) {
			throw new RuntimeException("An exception occurred while generating the source file "+METHOD_NAME, e);
		}
	}

	private void generateAbstractandConcreteMethods(StringBuilder javaFile, GenerateLambda generateSignatureContainerAnnotation) {
		MethodParameter[] types = generateSignatureContainerAnnotation.paramTypes();
		int maxNumberOfParams = generateSignatureContainerAnnotation.maxNumberOfParameters();
		generateAbstractMethods(javaFile, types, maxNumberOfParams);
	}

	private void generateAbstractMethods(StringBuilder javaFile, MethodParameter[] types, int maxNumberOfParams) {
		List<String> returnTypes = Arrays.asList(types).stream().map(type -> type.getTypeAsSourceCodeString()).collect(Collectors.toList());
		returnTypes.add("void");
		ICombinatoricsVector<MethodParameter> originalVector = Factory.createVector(types);
		generateInterfaceMethodsForStaticCallsWithMaxNumOfArgs(javaFile, originalVector, returnTypes, maxNumberOfParams + 1);
		generateInterfaceMethodCombinationsRecursively(javaFile, originalVector, returnTypes, maxNumberOfParams);
	}

	private void generateInterfaceMethodsForStaticCallsWithMaxNumOfArgs(StringBuilder javaFile,
			ICombinatoricsVector<MethodParameter> originalVector, List<String> returnTypes, int numberOfParams) {
		Generator<MethodParameter> gen = Factory.createPermutationWithRepetitionGenerator(originalVector, numberOfParams);
		for (String returnTypeAsString : returnTypes) {
			for (ICombinatoricsVector<MethodParameter> paramType : gen) {
				if (paramType.getVector().get(0) == MethodParameter.OBJECT) {
					String parameters = getParametersString(paramType, javaFile);
					javaFile.append(NEWLINE_TAB).append(returnTypeAsString).append(getSignatureExclArgsAndReturn(returnTypeAsString)).append(parameters).append(END_OF_SIGNATURE);
				}
			}
		}
	}

	private void generateInterfaceMethodCombinationsRecursively(StringBuilder javaFile,
			ICombinatoricsVector<MethodParameter> originalVector, List<String> returnTypes, int numberOfParams) {
		if (numberOfParams >= 0) {
			javaFile.append(NEWLINE);
			Generator<MethodParameter> gen = Factory.createPermutationWithRepetitionGenerator(originalVector, numberOfParams);
			for (String returnTypeAsString : returnTypes) {
				generateInterfaceMethods(gen, returnTypeAsString, javaFile);
			}
			generateInterfaceMethodCombinationsRecursively(javaFile, originalVector, returnTypes, --numberOfParams);
		}
	}

	private void generateInterfaceMethods(Generator<MethodParameter> gen, String returnTypeAsString, StringBuilder javaFile) {
		javaFile.append(NEWLINE);
		for (ICombinatoricsVector<MethodParameter> paramType : gen) {
			String parameters = getParametersString(paramType, javaFile);
			javaFile.append(NEWLINE_TAB).append(returnTypeAsString).append(getSignatureExclArgsAndReturn(returnTypeAsString)).append(parameters).append(END_OF_SIGNATURE);
		}
	}

	private String getParametersString(ICombinatoricsVector<MethodParameter> paramType, StringBuilder javaFile) {
		AtomicInteger atomicInteger = new AtomicInteger(1);
		return paramType.getVector().stream()
				.map(t -> t.getTypeAsSourceCodeString() + " arg" + atomicInteger.getAndIncrement())
				.collect(Collectors.joining(", "));
	}
	
	private static String getSignatureExclArgsAndReturn(String returnType){
		switch (returnType){
		case "boolean": return METHOD_NAME_PART_BOOLEAN;
		case "byte": return METHOD_NAME_PART_BYTE;
		case "char": return METHOD_NAME_PART_CHAR;
		case "double": return METHOD_NAME_PART_DOUBLE;
		case "float": return METHOD_NAME_PART_FLOAT;
		case "int": return METHOD_NAME_PART_INT;
		case "long": return METHOD_NAME_PART_LONG;
		case "Object": return METHOD_NAME_PART_OBJECT;
		case "short": return METHOD_NAME_PART_SHORT;
		case "void" : return METHOD_NAME_PART_VOID;
		default: return METHOD_NAME_PART_OBJECT;
		}
	}
	
	static String getMethodName(String returnType){
		switch (returnType){
		case "boolean": return METHOD_NAME_BOOLEAN;
		case "byte": return METHOD_NAME_BYTE;
		case "char": return METHOD_NAME_CHAR;
		case "double": return METHOD_NAME_DOUBLE;
		case "float": return METHOD_NAME_FLOAT;
		case "int": return METHOD_NAME_INT;
		case "long": return METHOD_NAME_LONG;
		case "Object": return METHOD_NAME_OBJECT;
		case "short": return METHOD_NAME_SHORT;
		case "void" : return METHOD_NAME_VOID;
		default: return METHOD_NAME_OBJECT;
		}
	}


}
