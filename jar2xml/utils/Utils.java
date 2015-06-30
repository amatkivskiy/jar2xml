package jar2xml.utils;

import java.lang.reflect.Type;
import java.util.List;

import japa.parser.ast.body.Parameter;

public class Utils {
  public static String getShortTypeName(Type type) {
//    Some methods don't use inner class names with outer class names
//    For example: Some classes use Bitmap.Config but other use Listener (real type us Picasso.Listener)
//
//  Type : interface com.squareup.picasso.Picasso$Listener
//  Parameter : Listener

    String shortName = type.toString();

    if (shortName.contains(".")) {
      String[] parts = shortName.split("\\.");
      shortName = parts[parts.length - 1];
    }

    if (shortName.contains("class ")) {
      shortName = shortName.replace("class ", "");
    }

    if (shortName.contains("interface ")) {
      shortName= shortName.replace("interface ", "");
    }

    shortName = shortName.replace("$", ".");

    return shortName;
  }

  public static String getMethodParamsString(Type[] params) {
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

  public static String getMethodParamsString(List<Parameter> params) {
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
}
