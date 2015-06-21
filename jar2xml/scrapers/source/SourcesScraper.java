package jar2xml.scrapers.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
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
    boolean isInner = false;
    String fileName = sourcesDir.getPath() + "/";
    if (asm.name.contains("$")) {
      isInner = true;
      fileName += asm.name.substring(0, asm.name.indexOf("$"));
    } else {
      fileName += asm.name;
    }
    fileName += ".java";

    if (isInner) return EMPTY_RESPONSE;

    try {
      MethodDeclaration method = getMethodForName(fileName, name, ptypes);
      if (method == null) {
        System.err.println("Unable to find method \"" + name + "\" with params : " + Arrays.toString(ptypes));
        return EMPTY_RESPONSE;
      }

      return getMethodParamNames(method);
    } catch (FileNotFoundException e) {
      System.err.println(String.format("File {%s} for class {%s} not found.", fileName, asm.name));
    } catch (ParseException e) {
      System.err.println(String.format("Failed to parse file {%s} for class {%s}.", fileName, asm.name));
    }

    return EMPTY_RESPONSE;
  }

  private String[] getMethodParamNames(MethodDeclaration method) {
    if (method.getParameters() == null) {
      return new String[0];
    } else{
      String[] results = new String[method.getParameters().size()];
      for (int i = 0; i < results.length; i++) {
        results[i] = method.getParameters().get(i).getId().getName();
      }

      return results;
    }
  }

  private MethodDeclaration getMethodForName(String file, String name, Type[] methodParams) throws FileNotFoundException, ParseException {
    CompilationUnit javaFile = JavaParser.parse(new FileInputStream(file));

    TypeDeclaration clazz = javaFile.getTypes().get(0);

    for (BodyDeclaration bodyDeclaration : clazz.getMembers()) {
      if (bodyDeclaration instanceof MethodDeclaration) {
        MethodDeclaration method = (MethodDeclaration) bodyDeclaration;
        if (method.getName().equals(name)) {
          if (method.getParameters() == null && methodParams.length != 0) {
            continue;
          }

          if (checkIfParamsMatches(method.getParameters(), methodParams)) {
            return method;
          }
        }
      }
    }

    return null;
  }

  private boolean checkIfParamsMatches(List<Parameter> methodParams, Type[] ptypes) {
    for (Parameter parameter : methodParams) {
      boolean doesMatches = false;

      for (Type ptype : ptypes) {
        //TODO: optimize this ugly algo!!!
        //TODO: Maybe there is need to check whether full type names match each other rather than only short type name
        //TODO:  like Request and com.squareup.picasso.Request
        String ptypeWithDots = ptype.toString().replace("$", ".");
        doesMatches = doesMatches || ptypeWithDots.contains(parameter.getType().toString());
      }

      if (!doesMatches) {
        return false;
      }
    }
    return true;
  }
}
