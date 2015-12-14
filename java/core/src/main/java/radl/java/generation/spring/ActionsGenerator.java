/*
 */
package radl.java.generation.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import radl.core.code.Code;
import radl.core.code.common.Constants;
import radl.core.code.radl.RadlCode;
import radl.java.code.JavaCode;

public class ActionsGenerator extends FromRadlCodeGenerator {

  private final Constants transitionConstants = new Constants("", "");

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    Code result = generateActions(radl, (Boolean)context.get(HAS_HYPERMEDIA));
    context.put(TRANSITION_CONSTANTS, transitionConstants);
    return Arrays.asList(result);
  }

  private Code generateActions(RadlCode radl, boolean hasHyperMediaTypes) {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", ACTIONS_TYPE);
    if (hasHyperMediaTypes) {
      addTransitionConstants(radl, result);
      result.add("");
    }
    result.add("}");
    return result;
  }

  private void addTransitionConstants(RadlCode radl, JavaCode code) {
    for (String transition : getTransitions(radl)) {
      transitionConstants.add(transition, transition, null);
    }
    addConstants(transitionConstants, code);
  }

  private Iterable<String> getTransitions(RadlCode radl) {
    Collection<String> result = new TreeSet<>();
    for (String state : radl.stateNames()) {
      if (radl.isStartState(state)) {
        continue;
      }
      for (String transition : radl.stateTransitionNames(state)) {
        result.add(transition);
      }
    }
    return result;
  }

}
