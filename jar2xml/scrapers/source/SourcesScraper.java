package jar2xml.scrapers.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import jar2xml.IDocScraper;
import jar2xml.utils.Utils;
import org.objectweb.asm.tree.ClassNode;

public class SourcesScraper implements IDocScraper {
  private final String[] EMPTY_RESPONSE = new String[0];

  private File sourcesDir;

  public SourcesScraper(File sourcesDir) throws FileNotFoundException {
    if (!sourcesDir.exists())
      throw new FileNotFoundException(sourcesDir.getAbsolutePath());

    if (!sourcesDir.isDirectory())
      throw new IllegalArgumentException(sourcesDir.getAbsolutePath() + " is not a directory.");

    this.sourcesDir = sourcesDir;
  }

  @Override public String[] getParameterNames(ClassNode asm, String name, Type[] ptypes, boolean isVarArgs) {
    // Check whether we can determine class
    if (isInnerClassAnonymous(asm)) return EMPTY_RESPONSE;

    String fileName = getFileNameForClass(asm);
    boolean isInnerClass = asm.name.contains("$");

    try {
      List<Parameter> params;

      if (isInnerClass) {
        if (name.contains(".")) name = name.split("\\.")[1];
        String className = asm.name.split("\\$")[1];

        params = getParamsForInnerClassMethodName(fileName, className, name, ptypes);
      } else {
        params = getParamsForMethodName(fileName, name, ptypes);
      }

      if (params == null) {
        System.err.println("Not found for " + asm.name + "." + name + "(" + Arrays.toString(ptypes)+ ")");
        return EMPTY_RESPONSE;
      }

      return getMethodParamNames(params);
    } catch (FileNotFoundException e) {
      System.err.println(String.format("File {%s} for class {%s} not found.", fileName, asm.name));
    } catch (ParseException e) {
      System.err.println(String.format("Failed to parse file {%s} for class {%s}.", fileName, asm.name));
    } catch (StringIndexOutOfBoundsException ignore) {
      //Got it from japa.parser internal. Need to investigate circumstances.
    }

    return EMPTY_RESPONSE;
  }

  private boolean isInnerClassAnonymous(ClassNode clazz) {
    if (!clazz.name.contains("$")) return false;

    String innerClassName = clazz.name.split("\\$")[1];

    return innerClassName.matches("[0-9]+");
  }

  private String getFileNameForClass(ClassNode clazz) {
    String fileName = sourcesDir.getPath() + "/";

    if (clazz.name.contains("$")) {
      fileName += clazz.name.split("\\$")[0];
    } else {
      fileName += clazz.name;
    }

    return fileName + ".java";
  }

  private String[] getMethodParamNames(List<Parameter> parameters) {
    if (parameters == null) {
      return EMPTY_RESPONSE;
    } else {
      String[] results = new String[parameters.size()];
      for (int i = 0; i < results.length; i++) {
        results[i] = parameters.get(i).getId().getName();
      }

      return results;
    }
  }

  private List<Parameter> getParamsForInnerClassMethodName(String file, String clazzName, String name, Type[] methodParams)
      throws FileNotFoundException, ParseException {
    CompilationUnit javaFile = JavaParser.parse(new FileInputStream(file));
    TypeDeclaration clazz = javaFile.getTypes().get(0);

    for (BodyDeclaration bodyDeclaration : clazz.getMembers()) {
      if (bodyDeclaration instanceof ClassOrInterfaceDeclaration) {
        ClassOrInterfaceDeclaration innerClass = (ClassOrInterfaceDeclaration) bodyDeclaration;

        if (innerClass.getName().equals(clazzName)) {
          return checkAllClassMembers(innerClass.getName(), innerClass.getMembers(), name, methodParams);
        }
      }
    }

    System.err.println(String.format("Method {%s(%s)} not found in {%s}", name, Utils.getMethodParamsString(methodParams), file));

    return null;
  }

  private List<Parameter> getParamsForMethodName(String file, String name, Type[] methodParams)
      throws FileNotFoundException, ParseException {
    CompilationUnit javaFile = JavaParser.parse(new FileInputStream(file));

    TypeDeclaration clazz = javaFile.getTypes().get(0);
    return checkAllClassMembers(clazz.getName(), clazz.getMembers(), name, methodParams);
  }

  private List<Parameter> checkAllClassMembers(String clazzName, List<BodyDeclaration> members, String targetMethodName,
      Type[] targetMethodParams) {
    for (BodyDeclaration bodyDeclaration : members) {
      if (bodyDeclaration instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) bodyDeclaration;

        if (checkIfMethodMatches(method.getName(), method.getParameters(), targetMethodName, targetMethodParams)) {
          System.out.println("Found " + clazzName + "." + method.getName() + "(" + Utils.getMethodParamsString(method.getParameters()) + ")");
          return method.getParameters();
        }
      } else if (bodyDeclaration instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructor = (ConstructorDeclaration) bodyDeclaration;

        if (checkIfMethodMatches(constructor.getName(), constructor.getParameters(), targetMethodName, targetMethodParams)) {
          System.out.println("Found " + clazzName + "." + constructor.getName() + "(" + Utils.getMethodParamsString(constructor.getParameters()) + ")");
          return constructor.getParameters();
        }
      }
    }

    return null;
  }

  private boolean checkIfMethodMatches(String methodName, List<Parameter> methodParameters, String targetMethodName,
      Type[] targetMethodParams) {
    if (methodName.equals(targetMethodName)) {
      if (methodParameters == null && targetMethodParams.length != 0) {
        return false;
      }

      if (checkIfParamsMatches(methodParameters, targetMethodParams)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkIfParamsMatches(List<Parameter> methodParams, Type[] pTypes) {
    List<String> convertedParams = new ArrayList<>(methodParams.size());
    for (Parameter methodParam : methodParams) {
      convertedParams.add(methodParam.getType().toString());
    }
    Collections.sort(convertedParams);

    List<String> convertedTypes = new ArrayList<>(pTypes.length);
    for (Type pType : pTypes) {
      convertedTypes.add(Utils.getShortTypeName(pType));
    }
    Collections.sort(convertedTypes);

    return convertedTypes.equals(convertedParams);
  }
}
