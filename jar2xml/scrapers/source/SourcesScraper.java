package jar2xml.scrapers.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.List;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import jar2xml.IDocScraper;
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
    String fileName = sourcesDir.getPath() + "/";

    if (asm.name.contains("$")) {
      return EMPTY_RESPONSE;
//      fileName += asm.name.substring(0, asm.name.indexOf("$"));
    } else {
      fileName += asm.name;
    }
    fileName += ".java";

    try {
      List<Parameter> params = getParamsForMethodName(fileName, name, ptypes);
      if (params == null) {
        return EMPTY_RESPONSE;
      }
      return getMethodParamNames(params);
    } catch (FileNotFoundException e) {
      System.err.println(String.format("File {%s} for class {%s} not found.", fileName, asm.name));
    } catch (ParseException e) {
      System.err.println(String.format("Failed to parse file {%s} for class {%s}.", fileName, asm.name));
    } catch (StringIndexOutOfBoundsException e) {
      System.err.println("Something went wrong. Need to investigate. File: " + fileName);
    }

    return EMPTY_RESPONSE;
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

  private List<Parameter> getParamsForMethodName(String file, String name, Type[] methodParams) throws FileNotFoundException, ParseException {
    CompilationUnit javaFile = JavaParser.parse(new FileInputStream(file));

    TypeDeclaration clazz = javaFile.getTypes().get(0);

    for (BodyDeclaration bodyDeclaration : clazz.getMembers()) {
      if (bodyDeclaration instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) bodyDeclaration;

        if (checkIfMethodMatches(method.getName(), method.getParameters(), name, methodParams)) {
          System.out.println("Found " + clazz.getName() + "." + method.getName() + "(" + getMethodParamsString(method.getParameters()) + ")");
          return method.getParameters();
        }
      } else if (bodyDeclaration instanceof ConstructorDeclaration) {
        ConstructorDeclaration constructor = (ConstructorDeclaration) bodyDeclaration;

        if (checkIfMethodMatches(constructor.getName(), constructor.getParameters(), name, methodParams)) {
          System.out.println("Found " + clazz.getName() + "." + constructor.getName() + "(" + getMethodParamsString(constructor.getParameters()) + ")");
          return constructor.getParameters();
        }
      }
    }

    System.err.println(String.format("Method {%s(%s)} not found in {%s}", name, getMethodParamsString(methodParams), file));

    return null;
  }

  private String getMethodParamsString(Type[] params) {
    StringBuilder builder = new StringBuilder("");

    if (params.length == 0) {
      return builder.toString();
    }

    for (int i = 0; i < params.length; i++) {
      builder.append(params[i].toString());

      if (i != params.length - 1) {
        builder.append(",");
      }
    }

    return builder.toString();
  }

  private String getMethodParamsString(List<Parameter> params) {
    StringBuilder builder = new StringBuilder("");

    if (params == null) {
      return builder.toString();
    }

    for (Parameter param : params) {
      builder.append(param.getId().getName());

      if (params.indexOf(param) != params.size() - 1) {
        builder.append(",");
      }
    }

    return builder.toString();
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
    for (Parameter parameter : methodParams) {
      boolean doesMatches = false;

      for (Type pType : pTypes) {
        //TODO: optimize this ugly algo!!!
        //TODO: Maybe there is need to check whether full type names match each other rather than only short type name
        //TODO:  like Request and com.squareup.picasso.Request
        String pTypeWithDots = pType.toString().replace("$", ".");
        doesMatches = doesMatches || pTypeWithDots.contains(parameter.getType().toString());
      }

      if (!doesMatches) {
        return false;
      }
    }
    return true;
  }
}
